package com.comet.keyboard.dictionary;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class DictionaryDB {

	static LexiconDbOpenHelper mOpenHelper = null;

	DictionaryDB(Context context, String language) {
		mOpenHelper = getOpenHelper(context, language);
	}
	
	private static synchronized LexiconDbOpenHelper getOpenHelper(Context context, String language) {
		if(mOpenHelper == null || !mOpenHelper.getLanguage().equals(language))
			mOpenHelper = new LexiconDbOpenHelper(context, language);
		
		return mOpenHelper;
	}


	public abstract int loadDictionaryFromDB(TrieDictionary lexicon, int nRecords);



	public interface OnLoadDictionaryListener {
		public void onLexiconLoadStarted();
		public void onLexiconLoaded();
	}



	static class LexiconDbOpenHelper extends SQLiteOpenHelper {

		private static final String DB_EXT = ".dic";
		private static final int DB_VERSION = 1;
		private static String mLanguage;

		private LexiconDbOpenHelper(Context context, String language) {
			super(context, language + DB_EXT, null, DB_VERSION);
			
			mLanguage = language;
		}
		
		
		private String getLanguage() {
			return mLanguage;
		}
		
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// Do nothing
		}

		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Do nothing
		}
	}
}