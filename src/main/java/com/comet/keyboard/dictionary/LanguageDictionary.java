package com.comet.keyboard.dictionary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.layouts.KeyboardLayout;

import junit.framework.Assert;

import java.util.Comparator;
import java.util.Locale;


public final class LanguageDictionary extends TrieDictionary<LanguageDictionary.LanguageSuggestion, SuggestionsRequest> {

	private static TrieDictionary mLoading = null;
	private LanguageDictionaryDB mLanguageDB;


	LanguageDictionary(final Context context, final KeyCollator collator) {
		super(context, collator);
	}


	@Override
	protected void loadDictionary() {
		Thread.currentThread().setName("LanguageLoader-" + mCollator.getLanguageCode());
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		if(mLoading != null) {
			mLoading.cancel();
		}
		mLoading = this;

		// Check if dictionary file exists
		if(!KeyboardApp.getApp().getUpdater().isDictionaryExist(mContext, mCollator.getLanguageCode())) {
			return;
		}

		// Skip "other" language
		if(mCollator.getLanguageCode().equals(mContext.getString(R.string.lang_code_other))) {
			return;
		}

		// TODO Replace this with OnLoadLexiconListener interface
		final KeyboardService ime = KeyboardService.getIME();
		if(ime != null && ime.isInputViewCreated() && !ime.isNeedUpdateDicts()) {
			KeyboardService.getIME().showMessage(mContext.getString(R.string.dictionary_loading_message), null);
		}

		// Load dictionary from DB
		mLanguageDB = new LanguageDictionaryDB(mContext, mCollator.getLanguageCode());

		// Pre-load first 5,000 records for quick response.
		setCountSum(mLanguageDB.loadDictionaryFromDB(this, 5000));

		// Clear the cache of any suggestions that were cached during pre-loading
		if (ime != null && ime.isInputViewCreated() && !ime.isNeedUpdateDicts()) {
			KeyboardService.getIME().clearMessage();
		}

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		// Now load all records.
		setCountSum(mLanguageDB.loadDictionaryFromDB(this, -1));
	}


	@Override
	protected void addSuggestion(
			final Suggestions suggestions,
			final String word,
			final int count,
			final int countSum,
			final double editDistance) {
		suggestions.add(new LanguageSuggestion(word, count, countSum, editDistance));
	}


	@Override
	protected Comparator<LanguageSuggestion> getComparator() {
		return mComparator;
	}


	private final Comparator<LanguageSuggestion> mComparator = new Comparator<LanguageSuggestion>() {
		@Override
		public int compare(LanguageSuggestion suggestion1, LanguageSuggestion suggestion2) {
			// Compare scores
			final double score = suggestion1.getScore();
			final double otherScore = suggestion2.getScore();

			if(score == otherScore) {
				return suggestion1.getWord().compareTo(suggestion2.getWord());
			}

			// Return the comparison
			return score < otherScore ? -1 : 1;
		}
	};


	/**
	 * Class LanguageSuggestion
	 * @author Barry
	 */
	public static class LanguageSuggestion extends Suggestion {
		private final double mFrequency;
		private final double mEditDistance;
		private final int mCount;
		private final int mCountSum;
		private final double mScore;

		public LanguageSuggestion(String word, int count, int countSum, double editDistance) {
			super(word);
			mCount = count;
			mCountSum = countSum;
			mFrequency = (double) count / (double) countSum;
			mEditDistance = editDistance;
			mScore = computeScore();
		}

		private double computeScore() {
			// Normalize
			double score = Math.abs(Math.log(mFrequency));

			// Subtract edit distance
			score += mEditDistance;

			return score;
		}


		public int getCount() {
			return mCount;
		}


		public int getCountSum() {
			return mCountSum;
		}

		
		public double getScore() {
			return mScore;
		}


		@Override
		public String toString() {
			return "Language("
					+ getWord() + ","
					+ mEditDistance  + ","
					+ mCount + ","
					+ String.format(Locale.getDefault(), "%.6f", mFrequency) + ","
					+ String.format(Locale.getDefault(), "%.6f", getScore()) + ")\n";
		}
	}


	@Override
	final void addToDB(String word, int count) {
		// Write to db
		mLanguageDB.addWordToLexicon(word, count);
	}


	@Override
	final void deleteFromDB(String word) {
		// Write to db
		mLanguageDB.deleteWordFromLexicon(word);
	}


	private class LanguageDictionaryDB extends DictionaryDB {
		// User lexicon table
		private static final String LEXICON_TABLE_NAME = "lexicon";
		private static final String LEXICON_FIELD_WORD = "word";
		private static final String LEXICON_FIELD_COUNT = "count";


		protected LanguageDictionaryDB(Context context, String language) {
			super(context, language);
		}


		/**
		 * Loads a lexicon from a table into a trie
		 * @param lexicon		The trie to fill
		 * @param nRecords		The maximum number of records to load
		 * @return				The sum of all counts
		 */
		@Override
		public final int loadDictionaryFromDB(final TrieDictionary lexicon, final int nRecords) {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			if(db == null) {
				return 0;
			}

			int countSum = 0;
			try {
				final String limit = nRecords > 0 ? String.valueOf(nRecords) : null;
				final Cursor cursor = db.query(LEXICON_TABLE_NAME, new String[] {LEXICON_FIELD_WORD, LEXICON_FIELD_COUNT}, "count>=0",
						null, null, null, "count DESC", limit);

				if (cursor == null) {
					return 0;
				}

				while(cursor.moveToNext() && !lexicon.isCancelled()) {
					final String word = cursor.getString(0);
					final int count = cursor.getInt(1);
					countSum += count;
					lexicon.insert(word, count);
				}

				cursor.close();

			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			} finally {
				mOpenHelper.close();
			}

			return countSum;
		}


		public boolean addWordToLexicon(final String word, final int count) {
			Assert.assertTrue(word != null);
			Assert.assertTrue(count > 0);

			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			try {
				// Append new shortcut item to database
				final ContentValues values = new ContentValues();
				values.put(LEXICON_FIELD_WORD, word);
				values.put(LEXICON_FIELD_COUNT, count);

				long result = db.update(LEXICON_TABLE_NAME, values, "word=?", new String[] { word });
				if(result == 0) {
					result = db.insert(LEXICON_TABLE_NAME, null, values);
					if (result == -1) {
						return false;
					}
				}
			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LEXICON_TABLE_NAME, e);
				return false;
			} finally {
				db.close();
			}

			return true;
		}


		public void deleteWordFromLexicon(final String word) {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			try {
				db.delete(LEXICON_TABLE_NAME, LEXICON_FIELD_WORD + "=?",
						new String[] { word } );
			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			} finally {
				mOpenHelper.close();
			}
		}
	}
}
