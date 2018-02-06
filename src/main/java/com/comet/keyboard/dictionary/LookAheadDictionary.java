/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

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
import com.comet.keyboard.Suggestor.SuggestionRequest;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.util.ProfileTracer;

import junit.framework.Assert;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;


public class LookAheadDictionary extends TrieDictionary {

	private static LookAheadDictionary mLoadingLexicon = null;
	private LookAheadDictionaryDB mLookAheadDB;

	public LookAheadDictionary(Context context, KeyCollator collator) {
		super(context, collator);
	}


	@Override
	protected void loadDictionary() {
		Thread.currentThread().setName("LookAheadLoader-" + mCollator.getLanguageCode());
		
		if(mLoadingLexicon != null) {
			mLoadingLexicon.cancel();
		}
		mLoadingLexicon = this;

		// Check if dictionary file exists
		if(!KeyboardApp.getApp().getUpdater().isDictionaryExist(mContext, mCollator.getLanguageCode())) {
			return;
		}
		
		// Skip "other" language
		if(mCollator.getLanguageCode().equals(mContext.getString(R.string.lang_code_other))) {
			return;
		}

		// Load lexicon from DB
		mLookAheadDB = new LookAheadDictionaryDB(mContext, mCollator.getLanguageCode());
		final ProfileTracer tracer = new ProfileTracer();
		
		tracer.log("LookAheadDictionary.loadLexicon()...");

		mLookAheadDB.loadDictionaryFromDB(this, -1);

		tracer.log("LookAheadDictionary.loadLexicon(): ...done populating");
	}


	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		StringBuilder word1 = new StringBuilder();
		StringBuilder word2 = new StringBuilder();
		// TODO: extracting words from KeyboardService is hacky. Pass them in somehow?
		KeyboardService.getIME().getTwoWordsBeforePrefix(word1, word2);

		word1 = new StringBuilder(word1.toString().toLowerCase());
		word2 = new StringBuilder(word2.toString().toLowerCase());

		/*
		 * 1. Get static suggestions
		 * 2. Merge user suggestions
		 */
		return getSuggestions(word1, word2, suggestions);
	}


	private Suggestions getSuggestions(StringBuilder word1, StringBuilder word2, Suggestions suggestions) {
		// Depth = 2
		if(word1.length() > 0 && word2.length() > 0) {
			String prefix = word1 + " " + word2;
			final int countSum = getCount(prefix);
			prefix += " ";
			final LookAheadSuggestions lookAheadSuggestions2 = new LookAheadSuggestions(suggestions, prefix, countSum, 2);
			super.getSuggestionsWithPrefix(lookAheadSuggestions2, prefix);
			suggestions.addAll(lookAheadSuggestions2);
		}

		if(suggestions.getRequest().getComposing().equals("")) {
			// Depth = 1
			if (word2.length() > 0) {
				String prefix = word2.toString();
				final int countSum = getCount(prefix);
				prefix += " ";
				final LookAheadSuggestions lookAheadSuggestions1 = new LookAheadSuggestions(suggestions, prefix, countSum, 1);
				super.getSuggestionsWithPrefix(lookAheadSuggestions1, prefix);
				suggestions.addAll(lookAheadSuggestions1);
			}
		}

		return suggestions;
	}


	@Override
	protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
		// TODO: THIS CAST IS HACKY
		suggestions.add(new LookAheadSuggestion(word, count, getCountSum(), editDistance, ((LookAheadSuggestions) suggestions).mDepth));
	}


	/**
	 * Adds a word to the dictionary with count = 1, or increments its count by 1
	 * @param trigram		The trigram to learn
	 */
	@Override
	final public boolean learn(String trigram) {
		final String words[] = trigram.split(" ");
		if(words.length != 3) {
			return false;
		}

		super.learn(trigram, COUNT_INCREMENT);

		final String bigram = new StringBuilder(words[0]).append(' ').append(words[1]).toString();
		super.learn(bigram, COUNT_INCREMENT * 2);

		super.learn(words[0], COUNT_INCREMENT * 3);

		return true;
	}


	/**
	 * Remove this word from the dictionary.
	 * @param word		The word to forget.
	 */
	@Override
	final public boolean forget(String word) {
		return false;
	}


	@Override
	final public boolean remember(String word) {
		return false;
	}


	@Override
	final void addToDB(final String trigram, final int count) {
		// Write to db
		mLookAheadDB.addTriGramToLookAhead(trigram, count);
	}


	@Override
	final void deleteFromDB(final String _word) {
		// Do nothing.
	}


	private class LookAheadSuggestions extends Suggestions {
		private final int mDepth;
		private final int mCountSum;

		public LookAheadSuggestions(Suggestions suggestions, String prefix, int countSum, int depth) {
			KeyboardService.getIME().getSuggestor().super(suggestions);
			
			mCountSum = countSum;
			mDepth = depth;
		}
	}


	public static class LookAheadSuggestion extends Suggestion {
		private final int mCount;
		private final double mFrequency;
		private final int mEditDistance;
		private double mScore = 0;
		private final int mDepth;

		public LookAheadSuggestion(String word, int count, int countSum, int editDistance, int depth) {
			super(word, 4);

			mCount = count;
			mEditDistance = editDistance;
			mFrequency = (double) count / (double) countSum;
			mDepth = depth;
		}


		private double computeScore() {
			// Normalize
			double score = Math.abs(Math.log(mFrequency));

			// Subtract edit distance
			score += mEditDistance;
			
			return score;
		}
		

		@Override
		public double getScore() {
			if(mScore == 0)
				mScore = computeScore();
			
			return mScore;
		}

		
		@Override
		protected int compareTo(Suggestion suggestion, String prefix) {
			if(!(suggestion instanceof LookAheadSuggestion)) {
				return super.compareTo(suggestion, prefix);
			}

			final LookAheadSuggestion another = (LookAheadSuggestion) suggestion;

			if(mDepth != another.mDepth) {
				return another.mDepth - mDepth;
			}

			// Is either one an exact match?
			if(mEditDistance == 0 && mWord.length() == prefix.length()) {
				return -1;
			} else {
				if(another.mEditDistance == 0 && another.mWord.length() == prefix.length()) {
					return 1;
				}
			}

			// Compare scores
			final double score = getScore();
			final double otherScore = another.getScore();

			if(score == otherScore) {
				return getWord().compareTo(another.getWord());
			}
			
			// Return the comparison
			return score < otherScore ? -1 : 1;
		}


		@Override
		public String toString() {
			return "LookAhead(" + getWord() + "," + mEditDistance + "," + mDepth + "," + String.format("%.4f", mFrequency) + "," + String.format("%.4f", getScore()) + ")";
		}
	}


	/**
	 * 
	 * @author Barry
	 *
	 */
	private class LookAheadDictionaryDB extends DictionaryDB {

		// User look-ahead table
		private static final String LOOKAHEAD_TABLE_NAME = "trigrams";
		private static final String LOOKAHEAD_FIELD_WORD1 = "word1";
		private static final String LOOKAHEAD_FIELD_WORD2 = "word2";
		private static final String LOOKAHEAD_FIELD_WORD3 = "word3";
		private static final String LOOKAHEAD_FIELD_COUNT = "count";

		public boolean addTriGramToLookAhead(String trigram, int count) {
			final String word1, word2, word3;
			final String words[] = trigram.split(" ");
			if(words.length != 3) {
				return false;
			} else {
				word1 = words[0];
				word2 = words[1];
				word3 = words[2];
			}

			Assert.assertTrue(word1 != null);
			Assert.assertTrue(word2 != null);
			Assert.assertTrue(word3 != null);
			Assert.assertTrue(count > 0);

			try {
				final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
				// Append new shortcut item to database
				final ContentValues values = new ContentValues();
				values.put(LOOKAHEAD_FIELD_WORD1, word1);
				values.put(LOOKAHEAD_FIELD_WORD2, word2);
				values.put(LOOKAHEAD_FIELD_WORD3, word3);
				values.put(LOOKAHEAD_FIELD_COUNT, count);

				final String whereClause =
						String.format("%s=? AND %s=? AND %s=?",
								LOOKAHEAD_FIELD_WORD1,
								LOOKAHEAD_FIELD_WORD2,
								LOOKAHEAD_FIELD_WORD3);
				long result = db.update(LOOKAHEAD_TABLE_NAME, values, whereClause, new String[] { word1, word2, word3 } );
				if(result == 0) {
					result = db.insertWithOnConflict(LOOKAHEAD_TABLE_NAME, null, values, CONFLICT_REPLACE);
					if (result == -1) {
						return false;
					}
				}
			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, "Failed to add word to " + LOOKAHEAD_TABLE_NAME, e);
				return false;
			} finally {
				mOpenHelper.close();
			}

			return true;
		}


		protected LookAheadDictionaryDB(Context context, String language) {
			super(context, language);
		}


		/**
		 * Load an n-gram set from a table into a trie
		 * @param lookAhead		The trie to fill
		 * @param nRecords		The maximum number of records to load
		 * @return				The sum of all counts
		 */
		public final int loadDictionaryFromDB(TrieDictionary lookAhead, int nRecords) {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			if(db == null) {
				return 0;
			}

			int countSum = 0;
			try {
				// Compute 1-gram sums
				final String limit = nRecords > 0 ? String.valueOf(nRecords) : null;
				Cursor cursor = db.query("trigrams", new String[] {"word1", "SUM(count) AS count"}, null,
						null, "word1", null, "count DESC", limit);
				if (cursor == null) {
					return 0;
				}

				while(cursor.moveToNext() && !lookAhead.isCancelled()) {
					String word1 = cursor.getString(0);
					final int count = cursor.getInt(1);

					lookAhead.insert(word1, count);
				}
				cursor.close();

				// Compute 2-gram sums
				cursor = db.query("trigrams", new String[] {"word1", "word2", "SUM(count) AS count"}, null,
						null, "word1,word2", null, "count DESC", limit);
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
				cursor = db.query("trigrams", new String[] {"word1", "word2", "word3", "count"}, null,
						null, null, null, "count DESC", limit);
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
			} finally {
				mOpenHelper.close();
			}
			
			return countSum;
		}
	}
}
