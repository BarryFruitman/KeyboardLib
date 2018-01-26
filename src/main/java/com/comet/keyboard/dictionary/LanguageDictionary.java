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
import com.comet.keyboard.Suggestor.Suggestion;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.layouts.KeyboardLayout;

import junit.framework.Assert;


public final class LanguageDictionary extends TrieDictionary {

	private static TrieDictionary mLoading = null;
	private LanguageDictionaryDB mLanguageDB;
	private int mCountSum = 0;
	private final int MIN_COUNT = 2; // Count threshold for suggestions


	public LanguageDictionary(Context context, KeyCollator collator) {
		super(context, collator);
	}


	@Override
	public void loadDictionary() {
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
		mCountSum = mLanguageDB.loadDictionaryFromDB(this, 5000);

		// Clear the cache of any suggestions that were cached during pre-loading
		if(ime != null && ime.isInputViewCreated() && !ime.isNeedUpdateDicts()) {
			KeyboardService.getIME().clearMessage();
		}

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		// Now load all records.
		mCountSum = mLanguageDB.loadDictionaryFromDB(this, -1);
	}


	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		if(mCountSum <= 0) {
			return suggestions;
		}

		/*
		 * 1. Find conjoined bigrams
		 * 2. Add static suggestions
		 * 3. Merge user suggestions
		 */
		return super.getSuggestions(findConjoinedBiGrams(suggestions));
	}


	private Suggestions findConjoinedBiGrams(Suggestions languageSuggestions) {
		// Check for conjoined bi-grams (e.g. "areyou")
		if(languageSuggestions.getComposing().length() >= 5) {
			// Check if this is a conjoined bi-gram
			for(int iPrefix = 1; iPrefix < languageSuggestions.getComposing().length() - 1; iPrefix++) {
				// Split into two words
				final String word1 = languageSuggestions.getComposing().substring(0, iPrefix + 1);
				final String word2 = languageSuggestions.getComposing().substring(iPrefix + 1, languageSuggestions.getComposing().length());
				final int count = ((TrieDictionary) KeyboardService.getIME().getSuggestor().getLookAheadDictionary()).getCount(word1 + " " + word2);
				if(count > 0) {
					final int count1 = getCount(word1);
					final int count2 = getCount(word2);
					// This bi-gram is in the LookAhead dictionary, therefore it is common enough to suggest.
					addSuggestion(languageSuggestions, word1 + " " + word2, Math.max(count1, count2), EditDistance.getJoined());
				}
			}

			// Check if this is a bi-gram conjoined by a space-adjacent key
			for(int iPrefix = 1; iPrefix < languageSuggestions.getComposing().length() - 1; iPrefix++) {
				if(KeyboardLayout.getCurrentLayout().isAdjacentToSpaceBar(languageSuggestions.getComposing().charAt(iPrefix))) {
					// Split into two words, omitting space-adjacent key
					final String word1 = languageSuggestions.getComposing().substring(0, iPrefix);
					final String word2 = languageSuggestions.getComposing().substring(iPrefix + 1, languageSuggestions.getComposing().length());
					final int count = ((TrieDictionary) KeyboardService.getIME().getSuggestor().getLookAheadDictionary()).getCount(word1 + " " + word2);
					if(count > 0) {
						final int count1 = getCount(word1);
						final int count2 = getCount(word2);
						// This bi-gram is in the LookAhead dictionary, therefore it is common enough to suggest.
						addSuggestion(languageSuggestions, word1 + " " + word2, Math.max(count1, count2), EditDistance.getJoined());
					}
				}
			}
		}
		
		return languageSuggestions;
	}


	@Override
	protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
		suggestions.add(new LanguageSuggestion(word, count, mCountSum, editDistance));
	}


	/**
	 * Class LanguageSuggestion
	 * @author Barry
	 *
	 */
	public static class LanguageSuggestion extends Suggestion {
		private double mFrequency;
		private int mEditDistance;
		private int mCount;
		private double mScore = 0;

		public LanguageSuggestion(String word, int count, int countSum, int editDistance) {
			super(word, 6);
			mCount = count;
			mFrequency = (double) count / (double) countSum;
			mEditDistance = editDistance;
		}
		
		
		private double computeScore() {
			// Normalize
			double score = Math.abs(Math.log10(mFrequency));

			// Subtract edit distance
			score += mEditDistance;
			
			return score;
		}
		
		
		@Override
		public double getScore() {
			if(mScore == 0) {
				mScore = computeScore();
			}
			
			return mScore;
		}
		
		
		@Override
		protected int compareTo(Suggestion suggestion, String prefix) {
			if(!(suggestion instanceof LanguageDictionary.LanguageSuggestion)) {
				return super.compareTo(suggestion, prefix);
			}

			LanguageSuggestion another = (LanguageSuggestion) suggestion;

			double score = getScore();
			double otherScore = another.getScore();

			if(score == otherScore) {
				return suggestion.getWord().compareTo(another.getWord());
			}

			// Return the comparison
			return score < otherScore ? -1 : 1;
		}


		@Override
		public String toString() {
			return "Language(" + getWord() + "," + mEditDistance  + "," + mCount + "," + String.format("%.6f", mFrequency) + "," + String.format("%.6f", getScore())  + ")";
		}
	}


	@Override
	public boolean contains(String word) {
		if(super.contains(word)) {
			return true;
		}
		
		return false;
	}


	@Override
	public boolean matches(String word) {
		if(super.matches(word)) {
			return true;
		}

		return false;
	}


	@Override
	public int learn(String word, int count) {
		count = super.learn(word, count);

		mCountSum += count;

		// Write to db
		mLanguageDB.addWordToLexicon(word, count);

		return count;
	}


	/**
	 * Adds a word to the dictionary with count = 1, or increments its count by 1
	 * @param word		The word to learn
	 */
	public boolean learn(String word) {
		learn(word, 1);

		return true;
	}


	/**
	 * Remove this word from the dictionary.
	 * @param word		The word to forget.
	 */
	@Override
	public boolean forget(String word) {
		super.forget(word);

		// Write to db
		mLanguageDB.deleteWordFromLexicon(mCollator.getLanguageCode(), word);

		return true;
	}


	@Override
	public boolean remember(String word) {
		if(isRemembered(word)) {
			return false;
		}

		learn(word, MIN_COUNT);

		return true;
	}


		/**
		 * Returns true if the word can appear in suggestions.
		 * @param word	The word to check.
		 * @return
		 */
		private boolean isRemembered(String word) {
			if(getCount(word) >= MIN_COUNT) {
				return true;
			}

			return false;
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
		public final int loadDictionaryFromDB(TrieDictionary lexicon, int nRecords) {
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


		public boolean addWordToLexicon(String word, int count) {
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


		public void deleteWordFromLexicon(String language, String word) {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			try {
				db.delete(LEXICON_TABLE_NAME, LEXICON_FIELD_WORD + "=?",
						new String[] {language,word});
			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
				db.close();
			}
		}
	}
}
