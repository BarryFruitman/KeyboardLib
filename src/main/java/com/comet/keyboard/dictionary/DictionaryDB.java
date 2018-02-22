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
//				upgradeLookAheadToV2(db);
			}
		}


		private void upgradeLanguageToV2(final SQLiteDatabase db) {
			// Copy UserDB to main db.
			final UserDB userDB = new UserDB(mContext, mLanguage);
			userDB.iterateLanguage(new UserDB.UserDbLanguageIteratorCallback() {
				@Override
				public void nextEntry(String word, final int count) {
					// If word is capitalized but there's no capitalized entry
					// in the language db, but there *is* a lower-case entry,
					// update the lower-case entry instead.
					if(DictionaryUtils.isCapitalized(word)) {
						final Cursor cursorCap = queryWord(word);
						if(cursorCap != null) {
							if (cursorCap.getCount() == 0) {
								final Cursor cursorLower = queryWord(word.toLowerCase());
								if (cursorLower != null) {
									if (cursorLower.getCount() > 0) {
										word = word.toLowerCase();
									}
									cursorLower.close();
								}
							}
							cursorCap.close();
						}
					}

					final Cursor cursor = queryWord(word);

					// Insert or update
					final ContentValues values = new ContentValues();
					values.put("word", word);
					if(cursor != null && cursor.moveToNext()) {
						values.put("count", cursor.getInt(0) + count);
						db.update(
								"lexicon",
								values,
								"word=?",
								new String[] { word });
					} else {
						values.put("count", count);
						db.insert(
								"lexicon",
								null,
								values);
					}
				}


				private Cursor queryWord(final String word) {
					return db.query(
							"lexicon",
							new String[] { "count" },
							"count > 0 AND word=?",
							new String[] { word },
							null,
							null,
							null);
				}
			});
		}


//		private void upgradeLookAheadToV2(final SQLiteDatabase db) {
//			// Copy UserDB to main db.
//			final UserDB userDB = new UserDB(mContext, mLanguage);
//			userDB.iterateLookAhead(new UserDB.UserDbLookAheadIteratorCallback() {
//				@Override
//				public void nextEntry(
//						final String word1,
//						final String word2,
//						final String word3,
//						final int count) {
//					final Cursor cursor = db.query(
//							"lookahead",
//							new String[] { "count" },
//							"count > 0 AND word1=? AND word2=? AND word3=?",
//							new String[] { word1, word2, word3 },
//							null,
//							null,
//							null);
//
//					// Insert or update
//					if(cursor != null && cursor.getCount() > 0) {
//						final int newCount = cursor.getInt(0) + count;
//						final ContentValues values = new ContentValues();
//						values.put("word1", word1);
//						values.put("word2", word2);
//						values.put("word3", word3);
//						values.put("count", newCount);
//						db.update(
//								"lookahead",
//								values,
//								"word1=? AND word2=? AND word3=?",
//								new String[] { word1, word2, word3 });
//					}
//				}
//			});
//		}
	}
}