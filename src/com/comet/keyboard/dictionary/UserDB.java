package com.comet.keyboard.dictionary;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardApp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public final class UserDB {

	private static UserDB mUserDB;
//	protected SQLiteDatabase mDB = null;
	protected SQLiteOpenHelper mOpenHelper = null;
	private String mLanguage = "en";

	// User look-ahead table
	private static final String LOOKAHEAD_TABLE_NAME = "trigrams";
	private static final String LOOKAHEAD_FIELD_LANG = "lang";
	private static final String LOOKAHEAD_FIELD_WORD1 = "word1";
	private static final String LOOKAHEAD_FIELD_WORD2 = "word2";
	private static final String LOOKAHEAD_FIELD_WORD3 = "word3";
	private static final String LOOKAHEAD_FIELD_COUNT = "count";

	// User lexicon table
	private static final String LEXICON_TABLE_NAME = "lexicon";
	private static final String LEXICON_FIELD_LANG = "lang";
	private static final String LEXICON_FIELD_WORD = "word";
	private static final String LEXICON_FIELD_COUNT = "count";

	public static synchronized UserDB getUserDB(Context context, String language) {
		if(mUserDB == null || !mUserDB.mLanguage.equals(language))
			mUserDB = new UserDB(context, language);

		return mUserDB;
	}



	private UserDB(Context context, String language) {

		mOpenHelper = new UserDbOpenHelper(context);
		
		mLanguage = language;
	}



	public boolean addWordToLexicon(String language, String word, int count) {	
		Assert.assertTrue(language != null);
		Assert.assertTrue(word != null);
		Assert.assertTrue(count > 0);
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		try {
			// Append new shortcut item to database 
			ContentValues values = new ContentValues();
			values.put(LEXICON_FIELD_LANG, language);
			values.put(LEXICON_FIELD_WORD, word);
			values.put(LEXICON_FIELD_COUNT, count);

			long result = db.insertOrThrow(LEXICON_TABLE_NAME, null, values);
			if(result == -1)
				return false;
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LEXICON_TABLE_NAME, e);
			return false;
		}

		return true;
	}

	
	public void deleteWordFromLexicon(String language, String word) {
		try {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			db.delete(LEXICON_TABLE_NAME, LEXICON_FIELD_LANG + "=?" + " AND " + LEXICON_FIELD_WORD + "=?",
					new String[] {language,word});
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
	}
	
	
	
	public boolean addTriGramToLookAhead(String language, String word1, String word2, String word3, int count) {	
		Assert.assertTrue(language != null);
		Assert.assertTrue(word1 != null);
		Assert.assertTrue(word2 != null);
		Assert.assertTrue(word3 != null);
		Assert.assertTrue(count > 0);

		try {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			// Append new shortcut item to database 
			ContentValues values = new ContentValues();
			values.put(LOOKAHEAD_FIELD_LANG, language);
			values.put(LOOKAHEAD_FIELD_WORD1, word1);
			values.put(LOOKAHEAD_FIELD_WORD2, word2);
			values.put(LOOKAHEAD_FIELD_WORD3, word3);
			values.put(LOOKAHEAD_FIELD_COUNT, count);

			long result = db.insertOrThrow(LOOKAHEAD_TABLE_NAME, null, values);
			if(result == -1)
				return false;
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LOOKAHEAD_TABLE_NAME, e);
			return false;
		}

		return true;
	}


	public void deleteWordFromLookAhead(String language, String word) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		try {
			db.delete(LOOKAHEAD_TABLE_NAME, LOOKAHEAD_FIELD_LANG + "=?" + " AND " + LOOKAHEAD_FIELD_WORD1 + "=? OR " + LOOKAHEAD_FIELD_WORD2 + "=? OR " + LOOKAHEAD_FIELD_WORD3 + "=?",
					new String[] {language,word,word,word});
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
	}



	private static final int TRUNCATE_LOOKAHEAD_TO = 5000;
	public boolean truncateLookAheadTables() {
		// The tri-grams table is always growing and will eventually slow down the word prediction.
		// It needs to be truncated occasionally so it doesn't get too big.
		// Let's limit the size to 5000.
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		String strCountQuery = "SELECT COUNT(*) FROM lookahead";
		String strSelectLastLimit = "SELECT count"
				+ " FROM lookahead ORDER BY count DESC LIMIT 1 OFFSET 5000";
		String strTruncateQuery = "DELETE FROM bigrams WHERE count <= ";
		try {
			SQLiteStatement statement = db.compileStatement(strCountQuery);
			long count = statement.simpleQueryForLong();
			
			if(count > TRUNCATE_LOOKAHEAD_TO) {
				statement = db.compileStatement(strSelectLastLimit);
				String lastUsed = statement.simpleQueryForString();
				
				db.execSQL(strTruncateQuery + "'" + lastUsed + "'");
			}
			
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return false;
		}

		return true;
	}



	public static void close() {
		if(mUserDB == null)
			return;

		if(mUserDB.mOpenHelper != null)
			mUserDB.mOpenHelper.close();

		mUserDB.mOpenHelper = null;
		mUserDB = null;
	}
	
	
	
	/*
	 * Build trees from tables
	 */
	
	/**
	 * Loads a lexicon from a table into a trie
	 * @param lexicon		The trie to fill
	 * @param nRecords		The maximum number of records to load
	 * @return				The sum of all counts
	 */
	public final int loadLanguage(TrieDictionary lexicon, int nRecords) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int countSum = 0;
		try {
			String limit = null;
			if(nRecords > 0)
				limit = String.valueOf(nRecords);
			Cursor cursor = db.query("lexicon", new String[] {"word", "count"}, LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null, null, null, "count DESC", limit);

			if (cursor == null)
				return 0;

			while(cursor.moveToNext() && !lexicon.isCancelled()) {
				String word = cursor.getString(0);
				int count = cursor.getInt(1);
				countSum += count;
//				Log.d(KeyboardApp.LOG_TAG, "word=" + word + ",count=" + count);
				lexicon.insert(word, count);
			}

			cursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
		
		return countSum;
	}

	public final int loadLanguage(TrieDictionary lexicon) {
		return loadLanguage(lexicon, -1);
	}

	/**
	 * Load an n-gram set from a table into a trie
	 * @param lookAhead		The trie to fill
	 * @param nRecords		The maximum number of records to load
	 * @return				The sum of all counts
	 */
	public final int loadLookAhead(TrieDictionary lookAhead, int nRecords) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int countSum = 0;
		try {
			String limit = null;
			if(nRecords > 0)
				limit = String.valueOf(nRecords);
			
			// Compute 1-gram sums
			Cursor cursor = db.query("trigrams", new String[] {"word1", "SUM(count) AS count"}, LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null, "word1", null, "count DESC", limit);
			if (cursor == null)
				return 0;

			while(cursor.moveToNext() && !lookAhead.isCancelled()) {
				String word1 = cursor.getString(0);
				int count = cursor.getInt(1);
				
				lookAhead.insert(word1, count);
			}
			cursor.close();

			
			// Compute 2-gram sums
			cursor = db.query("trigrams", new String[] {"word1", "word2", "SUM(count) AS count"}, LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null, "word1,word2", null, "count DESC", limit);
			if (cursor == null)
				return 0;

			while(cursor.moveToNext() && !lookAhead.isCancelled()) {
				String word1 = cursor.getString(0);
				String word2 = cursor.getString(1);
				int count = cursor.getInt(2);
				
				lookAhead.insert(word1 + " " + word2, count);
			}
			cursor.close();

			
			// Load 3-grams
			cursor = db.query("trigrams", new String[] {"word1", "word2", "word3", "count"}, LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null, null, null, "count DESC", limit);
			if (cursor == null)
				return 0;

			while(cursor.moveToNext() && !lookAhead.isCancelled()) {
				String word1 = cursor.getString(0);
				String word2 = cursor.getString(1);
				String word3 = cursor.getString(2);
				int count = cursor.getInt(3);
				countSum += count;
				
				lookAhead.insert(word1 + " " + word2 + " " + word3, count);
			}
			cursor.close();
			

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
		
		return countSum;
	}




	private class UserDbOpenHelper extends SQLiteOpenHelper {

		private static final String DB_FILE = "user_dictionary.db";
		private static final int DB_VERSION = 2;

		protected UserDbOpenHelper(Context context) {
			super(context, DB_FILE, null, DB_VERSION);
		}


		@Override
		public void onCreate(SQLiteDatabase db) {
			createLookAheadTable(db);
			createLexiconTable(db);
		}


		private void createLookAheadTable(SQLiteDatabase db) throws SQLiteException {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + LOOKAHEAD_TABLE_NAME
					+ "(" + LOOKAHEAD_FIELD_LANG + " TEXT, "
					+ LOOKAHEAD_FIELD_WORD1 + " TEXT, "
					+ LOOKAHEAD_FIELD_WORD2 + " TEXT, "
					+ LOOKAHEAD_FIELD_WORD3 + " TEXT, "
					+ LOOKAHEAD_FIELD_COUNT + " INTEGER, "
					+ "CONSTRAINT unique_fields UNIQUE (" + LOOKAHEAD_FIELD_LANG
					+ "," + LOOKAHEAD_FIELD_WORD1 + "," + LOOKAHEAD_FIELD_WORD2 + "," + LOOKAHEAD_FIELD_WORD3
					+ ") ON CONFLICT REPLACE)");
			// Index to speed up queries
			db.execSQL("CREATE INDEX IF NOT EXISTS find_index ON " + LOOKAHEAD_TABLE_NAME + " ("
					+ LOOKAHEAD_FIELD_LANG + ", " + LOOKAHEAD_FIELD_WORD1 + ", " + LOOKAHEAD_FIELD_WORD2 + ", " + LOOKAHEAD_FIELD_WORD3 + ", "
					+ LOOKAHEAD_FIELD_COUNT + " DESC)");
		}


		private void createLexiconTable(SQLiteDatabase db) throws SQLiteException {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + LEXICON_TABLE_NAME
					+ "(" + LEXICON_FIELD_LANG + " TEXT, "
					+ LEXICON_FIELD_WORD + " TEXT, "
					+ LEXICON_FIELD_COUNT + " INTEGER, "
					+ "CONSTRAINT unique_fields UNIQUE (" + LEXICON_FIELD_LANG
					+ "," + LEXICON_FIELD_WORD + ") ON CONFLICT REPLACE)");
			// Index to speed up queries
			db.execSQL("CREATE INDEX IF NOT EXISTS find_index ON " + LEXICON_TABLE_NAME + " ("
					+ LEXICON_FIELD_LANG + ", " + LEXICON_FIELD_WORD + ", "
					+ LEXICON_FIELD_COUNT + " DESC)");
		}

		
		
		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// Do nothing
		}
		
	}

}