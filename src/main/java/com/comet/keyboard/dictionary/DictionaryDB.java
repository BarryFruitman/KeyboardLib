package com.comet.keyboard.dictionary;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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


	class LexiconDbOpenHelper extends SQLiteOpenHelper {
		private static final String DB_EXT = ".dic";
		private static final int DB_VERSION = 2;
		private String mLanguage;
		private int mReferenceCount = 0;
		private final Context mContext;


		private LexiconDbOpenHelper(final Context context, final String language) {
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
		public void close() {
			mReferenceCount--;
			if (mReferenceCount == 0) {
				super.close();
			}
		}


		@Override
		public void onCreate(final SQLiteDatabase db) {
			// Do nothing
		}

		
		@Override
		public void onUpgrade(final SQLiteDatabase db,
							  final int oldVersion,
							  final int newVersion) {
			if(oldVersion == 1 && newVersion == 2) {
				upgradeLanguageToV2(db);
				upgradeLookAheadToV2(db);
			}
		}


		private void upgradeLanguageToV2(final SQLiteDatabase db) {
			// Copy UserDB to main db.
			final UserDB userDB = new UserDB(mContext, mLanguage);
			userDB.iterateLanguage(new UserDB.UserDbLanguageIteratorCallback() {
				@Override
				public void nextEntry(final String word, final int count) {
					final Cursor cursor = db.query(
							"lexicon",
							new String[] { "count" },
							"count > 0 AND word=?",
							new String[] { word },
							null,
							null,
							null);

					// Insert or update
					if(cursor != null && cursor.getCount() > 0) {
						final int newCount = cursor.getInt(0) + count;
						final ContentValues values = new ContentValues();
						values.put("word", word);
						values.put("count", newCount);
						db.update(
								"lexicon",
								values,
								"word=?",
								new String[] { word });
					}
				}
			});
		}


		private void upgradeLookAheadToV2(final SQLiteDatabase db) {
			// Copy UserDB to main db.
			final UserDB userDB = new UserDB(mContext, mLanguage);
			userDB.iterateLookAhead(new UserDB.UserDbLookAheadIteratorCallback() {
				@Override
				public void nextEntry(
						final String word1,
						final String word2,
						final String word3,
						final int count) {
					final Cursor cursor = db.query(
							"lookahead",
							new String[] { "count" },
							"count > 0 AND word1=? AND word2=? AND word3=?",
							new String[] { word1, word2, word3 },
							null,
							null,
							null);

					// Insert or update
					if(cursor != null && cursor.getCount() > 0) {
						final int newCount = cursor.getInt(0) + count;
						final ContentValues values = new ContentValues();
						values.put("word1", word1);
						values.put("word2", word2);
						values.put("word3", word3);
						values.put("count", newCount);
						db.update(
								"lookahead",
								values,
								"word1=? AND word2=? AND word3=?",
								new String[] { word1, word2, word3 });
					}
				}
			});
		}
	}
}