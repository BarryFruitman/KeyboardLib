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
	private UserDbOpenHelper mOpenHelper;
	private String mLanguage;

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

	public static synchronized UserDB getUserDB(final Context context, final String language) {
		if(mUserDB == null || !mUserDB.mLanguage.equals(language))
			mUserDB = new UserDB(context, language);

		return mUserDB;
	}


	private UserDB(final Context context, final String language) {
		mOpenHelper = new UserDbOpenHelper(context);
		mLanguage = language;
	}


	public void insertOrIncrement(final String language, final String word, final int increment) {
		Assert.assertTrue(language != null);
		Assert.assertTrue(word != null);
		Assert.assertTrue(increment > 0);

		try {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			// Append new shortcut item to database
			final ContentValues values = new ContentValues();
			values.put(LEXICON_FIELD_WORD, word);
			int count = 0;

			final Cursor cursor = db.query(
					"lexicon",
					new String[] {"count"},
					LEXICON_FIELD_WORD + "=? AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null,
					null,
					null,
					"count DESC",
					"1");
			if (cursor != null) {
				if (cursor.moveToNext()) {
					count = cursor.getInt(0);
				}
				cursor.close();
			}

			final int newCount = count + increment;
			values.put(LEXICON_FIELD_COUNT, newCount);

			long result = db.update(LEXICON_TABLE_NAME, values, "word=?", new String[]{word});
			if (result == 0) {
				db.insert(LEXICON_TABLE_NAME, null, values);
			}
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LEXICON_TABLE_NAME, e);
		} finally {
			mOpenHelper.close();
		}
	}


	public void deleteWordFromLexicon(final String language, final String word) {
		try {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			db.delete(LEXICON_TABLE_NAME,
					LEXICON_FIELD_LANG + "=?" + " AND " + LEXICON_FIELD_WORD + "=?",
					new String[] {language,word});
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		} finally {
			mOpenHelper.close();
		}
	}


	public boolean addTriGramToLookAhead(
			final String language,
			final String word1,
			final String word2,
			final String word3,
			final int count) {
		Assert.assertTrue(language != null);
		Assert.assertTrue(word1 != null);
		Assert.assertTrue(word2 != null);
		Assert.assertTrue(word3 != null);
		Assert.assertTrue(count > 0);

		try {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			// Append new shortcut item to database
			ContentValues values = new ContentValues();
			values.put(LOOKAHEAD_FIELD_LANG, language);
			values.put(LOOKAHEAD_FIELD_WORD1, word1);
			values.put(LOOKAHEAD_FIELD_WORD2, word2);
			values.put(LOOKAHEAD_FIELD_WORD3, word3);
			values.put(LOOKAHEAD_FIELD_COUNT, count);

			final long result = db.insertOrThrow(LOOKAHEAD_TABLE_NAME, null, values);
			if(result == -1) {
				return false;
			}
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LOOKAHEAD_TABLE_NAME, e);
			return false;
		} finally {
			mOpenHelper.close();
		}

		return true;
	}


	public void deleteWordFromLookAhead(final String language, final String word) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		try {
			db.delete(
					LOOKAHEAD_TABLE_NAME,
					LOOKAHEAD_FIELD_LANG + "=?" + " AND " + LOOKAHEAD_FIELD_WORD1 + "=? OR " + LOOKAHEAD_FIELD_WORD2 + "=? OR " + LOOKAHEAD_FIELD_WORD3 + "=?",
					new String[] {language,word,word,word});
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		} finally {
			mOpenHelper.close();
		}
	}


	private static final int TRUNCATE_LOOKAHEAD_TO = 5000;
	public boolean truncateLookAheadTables() {
		// The tri-grams table is always growing and will eventually slow down the word prediction.
		// It needs to be truncated occasionally so it doesn't get too big.
		// Let's limit the size to 5000.

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final String strCountQuery = "SELECT COUNT(*) FROM lookahead";
		final String strSelectLastLimit = "SELECT count FROM lookahead ORDER BY count DESC LIMIT 1 OFFSET 5000";
		final String strTruncateQuery = "DELETE FROM bigrams WHERE count <= ";
		try {
			SQLiteStatement statement = db.compileStatement(strCountQuery);
			final long count = statement.simpleQueryForLong();

			if(count > TRUNCATE_LOOKAHEAD_TO) {
				statement = db.compileStatement(strSelectLastLimit);
				String lastUsed = statement.simpleQueryForString();

				db.execSQL(strTruncateQuery + "'" + lastUsed + "'");
			}
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return false;
		} finally {
			mOpenHelper.close();
		}

		return true;
	}


	public static void close() {
		if(mUserDB == null) {
			return;
		}

		if(mUserDB.mOpenHelper != null) {
			mUserDB.mOpenHelper.close();
		}

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
	public final int loadLanguage(final TrieDictionary lexicon, final int nRecords) {
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		int countSum = 0;
		try {
			String limit = null;
			if(nRecords > 0) {
				limit = String.valueOf(nRecords);
			}
			final Cursor cursor = db.query(
					"lexicon",
					new String[] {"word", "count"},
					LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null,
					null,
					null,
					"count DESC",
					limit);

			if (cursor == null) {
				return 0;
			}

			while(cursor.moveToNext() && !lexicon.isCancelled()) {
				String word = cursor.getString(0);
				final int countUser = cursor.getInt(1);
				countSum += countUser;

				final int countLang = lexicon.getCount(word);
				lexicon.insert(word, countLang + countUser);
			}

			cursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		} finally {
			mOpenHelper.close();
		}

		return countSum;
	}


	public final int loadLanguage(final TrieDictionary lexicon) {
		return loadLanguage(lexicon, -1);
	}


	/**
	 * Load an n-gram set from a table into a trie
	 * @param lookAhead		The trie to fill
	 * @param nRecords		The maximum number of records to load
	 * @return				The sum of all counts
	 */
	public final int loadLookAhead(final TrieDictionary lookAhead, final int nRecords) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		int countSum = 0;
		try {
			String limit = null;
			if(nRecords > 0) {
				limit = String.valueOf(nRecords);
			}

			// Compute 1-gram sums
			Cursor cursor = db.query("trigrams",
					new String[] {"word1", "SUM(count) AS count"},
					LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null,
					"word1",
					null,
					"count DESC",
					limit);
			if (cursor == null) {
				return 0;
			}

			while(cursor.moveToNext() && !lookAhead.isCancelled()) {
				String word1 = cursor.getString(0);
				int count = cursor.getInt(1);

				lookAhead.insert(word1, count);
			}
			cursor.close();


			// Compute 2-gram sums
			cursor = db.query(
					"trigrams",
					new String[] {"word1", "word2", "SUM(count) AS count"},
					LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null,
					"word1,word2",
					null,
					"count DESC",
					limit);

			if (cursor == null) {
				return 0;
			}

			while(cursor.moveToNext() && !lookAhead.isCancelled()) {
				String word1 = cursor.getString(0);
				String word2 = cursor.getString(1);
				int count = cursor.getInt(2);

				lookAhead.insert(word1 + " " + word2, count);
			}
			cursor.close();


			// Load 3-grams
			cursor = db.query(
					"trigrams",
					new String[] {"word1", "word2", "word3", "count"},
					LEXICON_FIELD_COUNT + ">=0 AND " + LEXICON_FIELD_LANG + "='" + mLanguage + "'",
					null,
					null,
					null,
					"count DESC",
					limit);
			if (cursor == null) {
				return 0;
			}

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

		private UserDbOpenHelper(final Context context) {
			super(context, DB_FILE, null, DB_VERSION);
		}


		@Override
		public void onCreate(final SQLiteDatabase db) {
			createLookAheadTable(db);
			createLexiconTable(db);
		}


		private void createLookAheadTable(final SQLiteDatabase db) throws SQLiteException {
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


		private void createLexiconTable(final SQLiteDatabase db) throws SQLiteException {
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
		public void onUpgrade(final SQLiteDatabase db, final int versionOld, final int versionNew) {
			// Do nothing
		}
	}
}