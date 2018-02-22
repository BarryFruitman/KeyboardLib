package com.comet.keyboard.dictionary;

import com.comet.keyboard.KeyboardApp;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * This database is deprecated and this class exists only for db upgrade purposes.
 */
public final class UserDB {

	private static UserDB mUserDB;
	protected SQLiteOpenHelper mOpenHelper = null;
	private String mLanguage = "en";

	// User look-ahead table
	private static final String LOOKAHEAD_TABLE_NAME = "trigrams";
	private static final String LOOKAHEAD_FIELD_WORD1 = "word1";
	private static final String LOOKAHEAD_FIELD_WORD2 = "word2";
	private static final String LOOKAHEAD_FIELD_WORD3 = "word3";
	private static final String LOOKAHEAD_FIELD_COUNT = "count";

	// User lexicon table
	private static final String LEXICON_TABLE_NAME = "lexicon";
	private static final String LEXICON_FIELD_WORD = "word";
	private static final String LEXICON_FIELD_COUNT = "count";

	UserDB(Context context, String language) {
		mOpenHelper = new UserDbOpenHelper(context);
		mLanguage = language;
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


	public interface UserDbLanguageIteratorCallback {
		void nextEntry(String word, int count);
	}


	/**
	 * Iterates through every language entry, calling a callback for each
	 */
	public final void iterateLanguage(UserDbLanguageIteratorCallback callback) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		try {
			Cursor cursor = db.query(
					LEXICON_TABLE_NAME,
					new String[] {
							LEXICON_FIELD_WORD,
							LEXICON_FIELD_COUNT
					},
					"count > 0 AND lang='" + mLanguage + "'",
					null,
					null,
					null,
					"count DESC");

			if (cursor == null) {
				return;
			}

			while(cursor.moveToNext()) {
				String word = cursor.getString(0);
				int count = cursor.getInt(1);
				callback.nextEntry(word, count);
			}

			cursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
	}


	public interface UserDbLookAheadIteratorCallback {
		void nextEntry(String word1, String word2, String word3, int count);
	}


	/**
	 * Iterates through every look-ahead entry, calling a callback for each
	 */
	public final void iterateLookAhead(UserDbLookAheadIteratorCallback callback) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		try {
			// Load 3-grams
			final Cursor cursor = db.query(
					LOOKAHEAD_TABLE_NAME,
					new String[] {
							LOOKAHEAD_FIELD_WORD1,
							LOOKAHEAD_FIELD_WORD2,
							LOOKAHEAD_FIELD_WORD3,
							LOOKAHEAD_FIELD_COUNT },
					"count >= 0 AND lang='" + mLanguage + "'",
					null,
					null,
					null,
					"count DESC");
			if (cursor == null) {
				return;
			}

			while(cursor.moveToNext()) {
				String word1 = cursor.getString(0);
				String word2 = cursor.getString(1);
				String word3 = cursor.getString(2);
				int count = cursor.getInt(3);

				callback.nextEntry(word1, word2, word3, count);
			}

			cursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
	}


	private class UserDbOpenHelper extends SQLiteOpenHelper {
		private static final String DB_FILE = "user_dictionary.db";
		private static final int DB_VERSION = 2;

		protected UserDbOpenHelper(Context context) {
			super(context, DB_FILE, null, DB_VERSION);
		}


		@Override
		public void onCreate(SQLiteDatabase db) {
			// Do nothing
		}


		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// Do nothing
		}
	}
}