package com.comet.keyboard.dictionary;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class UserDB {
	private static UserDB mUserDB;
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


	public static void close() {
		if(mUserDB == null)
			return;

		if(mUserDB.mOpenHelper != null)
			mUserDB.mOpenHelper.close();

		mUserDB.mOpenHelper = null;
		mUserDB = null;
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