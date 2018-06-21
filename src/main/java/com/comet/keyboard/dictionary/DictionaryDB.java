package com.comet.keyboard.dictionary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class DictionaryDB {

	LexiconDbOpenHelper mOpenHelper = null;


	DictionaryDB(final Context context, final String language) {
		mOpenHelper = getOpenHelper(context, language);
	}
	

	private synchronized LexiconDbOpenHelper getOpenHelper(
			final Context context,
			final String language) {
		if(mOpenHelper == null || !mOpenHelper.getLanguage().equals(language)) {
			mOpenHelper = new LexiconDbOpenHelper(context, language);
		}
		
		return mOpenHelper;
	}


	public abstract int loadDictionaryFromDB(TrieDictionary lexicon, int nRecords);


	public static class LexiconDbOpenHelper extends SQLiteOpenHelper {
		private static final String DB_EXT = ".dic";
		private static final int DB_VERSION = 1;
		private String mLanguage;
		private int mReferenceCount = 0;
		private final Context mContext;


		public LexiconDbOpenHelper(final Context context, final String language) {
			super(context, language + DB_EXT, null, DB_VERSION);

			mContext = context;
			mLanguage = language;
		}
		
		
		private String getLanguage() {
			return mLanguage;
		}


		@Override
		public SQLiteDatabase getWritableDatabase() {
			mReferenceCount++;
			return super.getWritableDatabase();
		}


		@Override
		public SQLiteDatabase getReadableDatabase() {
			mReferenceCount++;
			return super.getReadableDatabase();
		}


		@Override
		public void onCreate(SQLiteDatabase db) {
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}


		@Override
		public void close() {
			mReferenceCount--;
			if (mReferenceCount == 0) {
				super.close();
			}
		}
	}
}