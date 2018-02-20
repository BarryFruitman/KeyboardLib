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
import com.comet.keyboard.R;
import com.comet.keyboard.util.ProfileTracer;

import junit.framework.Assert;

import java.util.Comparator;
import java.util.Locale;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;


public class LookAheadDictionary
		extends TrieDictionary<LookAheadDictionary.LookAheadSuggestion, LookAheadDictionary.LookAheadSuggestionsRequest> {


	private static LookAheadDictionary mLoadingLexicon = null;
	private LookAheadDictionaryDB mLookAheadDB;

	LookAheadDictionary(Context context, KeyCollator collator) {
		super(context, collator);
	}


	@Override
	protected void loadDictionary() {
		Thread.currentThread().setName("LookAheadLoader-" + mCollator.getLanguageCode());

		if (mLoadingLexicon != null) {
			mLoadingLexicon.cancel();
		}
		mLoadingLexicon = this;

		// Check if dictionary file exists
		if (!KeyboardApp.getApp().getUpdater().isDictionaryExist(mContext, mCollator.getLanguageCode())) {
			return;
		}

		// Skip "other" language
		if (mCollator.getLanguageCode().equals(mContext.getString(R.string.lang_code_other))) {
			return;
		}

		// Load lexicon from DB
		mLookAheadDB = new LookAheadDictionaryDB(mContext, mCollator.getLanguageCode());
		final ProfileTracer tracer = new ProfileTracer();

		tracer.log("LookAheadDictionary.loadLexicon()...");

		setCountSum(mLookAheadDB.loadDictionaryFromDB(this, -1));

		tracer.log("LookAheadDictionary.loadLexicon(): ...done populating");
	}


	@Override
	public Suggestions<LookAheadSuggestion> getSuggestions(LookAheadSuggestionsRequest request) {
		final ArraySuggestions<LookAheadSuggestion> suggestions =
				new ArraySuggestions<>(request);

		final Suggestions<LookAheadSuggestion> matches2 = getMatches(request.getPrefix2());
		for (final LookAheadSuggestion match : matches2) {
			suggestions.addAll(getSuggestionsAfterPrefix(
					new SortedSuggestions<>(request, getComparator()),
					match.getWord() + " ", match.getCount()));
		}

		final Suggestions<LookAheadSuggestion> matches1 = getMatches(request.getPrefix1());
		for (final LookAheadSuggestion match : matches1) {
			suggestions.addAll(getSuggestionsAfterPrefix(
					new SortedSuggestions<>(request, getComparator()),
					match.getWord() + " ", match.getCount()));
		}

		return suggestions;
	}


	@Override
	protected void addSuggestion(
			final Suggestions<LookAheadSuggestion> suggestions,
			final String word,
			final int count,
			final int countSum,
			final double editDistance) {
		// count sum needs to be the count of prefix
		suggestions.add(new LookAheadSuggestion(word, count, countSum, editDistance));
	}


	/**
	 * Adds a word to the dictionary with count = 1, or increments its count by 1
	 *
	 * @param trigram The trigram to learn
	 */
	@Override
	final public boolean learn(String trigram) {
		final String words[] = trigram.split(" ");
		if (words.length != 3) {
			return false;
		}

		super.learn(trigram, COUNT_INCREMENT);

		final String bigram = words[0] + " " + words[1];
		super.learn(bigram, COUNT_INCREMENT * 2);

		super.learn(words[0], COUNT_INCREMENT * 3);

		return true;
	}


	/**
	 * Remove this word from the dictionary.
	 *
	 * @param word The word to forget.
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


	static class LookAheadSuggestionsRequest extends SuggestionsRequest {
		final String mWord1;
		final String mWord2;

		LookAheadSuggestionsRequest(
				final SuggestionsRequest request,
				final String word1,
				final String word2) {
			super(request.getComposing());

			mWord1 = word1;
			mWord2 = word2;
		}

		String getPrefix1() {
			return mWord2;
		}

		String getPrefix2() {
			return mWord1 + " " + mWord2;
		}
	}


	public static class LookAheadSuggestion extends Suggestion {
		private final double mFrequency;
		private final double mEditDistance;
		private final double mScore;
		private final int mCount;
		private final int mCountSum;

		LookAheadSuggestion(String word, int count, int countSum, double editDistance) {
			super(word);

			mCount = count;
			mCountSum = countSum;
			mEditDistance = editDistance;
			mFrequency = (double) count / (double) countSum;
			mScore = computeScore();
		}


		private double computeScore() {
			// Normalize
			double score = Math.abs(Math.log(mFrequency));

			// Subtract edit distance
			score += mEditDistance;

			return score;
		}


		int getCount() {
			return mCount;
		}


		int getCountSum() {
			return mCountSum;
		}


		double getScore() {
			return mScore;
		}


		@Override
		public String toString() {
			return "LookAhead("
					+ getWord() + ","
					+ mEditDistance + ","
					+ mCount + ","
					+ String.format(Locale.getDefault(), "%.4f", mFrequency) + ","
					+ String.format(Locale.getDefault(), "%.4f", getScore()) + ")\n";
		}
	}


	@Override
	protected Comparator<LookAheadSuggestion> getComparator() {
		return mComparator;
	}


	private final Comparator<LookAheadSuggestion> mComparator = new Comparator<LookAheadSuggestion>() {
		@Override
		public int compare(LookAheadSuggestion suggestion1, LookAheadSuggestion suggestion2) {
			// Compare scores
			final double score = suggestion1.getScore();
			final double otherScore = suggestion2.getScore();

			if (score == otherScore) {
				return suggestion1.getWord().compareTo(suggestion2.getWord());
			}

			// Return the comparison
			return score < otherScore ? -1 : 1;
		}
	};


	/**
	 * @author Barry
	 */
	private class LookAheadDictionaryDB extends DictionaryDB {

		// User look-ahead table
		private static final String LOOKAHEAD_TABLE_NAME = "trigrams";
		private static final String LOOKAHEAD_FIELD_WORD1 = "word1";
		private static final String LOOKAHEAD_FIELD_WORD2 = "word2";
		private static final String LOOKAHEAD_FIELD_WORD3 = "word3";
		private static final String LOOKAHEAD_FIELD_COUNT = "count";

		boolean addTriGramToLookAhead(String trigram, int count) {
			final String word1, word2, word3;
			final String words[] = trigram.split(" ");
			if (words.length != 3) {
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
				long result = db.update(LOOKAHEAD_TABLE_NAME, values, whereClause, new String[]{word1, word2, word3});
				if (result == 0) {
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


		LookAheadDictionaryDB(Context context, String language) {
			super(context, language);
		}


		/**
		 * Load an n-gram set from a table into a trie
		 *
		 * @param lookAhead The trie to fill
		 * @param nRecords  The maximum number of records to load
		 * @return The sum of all counts
		 */
		public final int loadDictionaryFromDB(TrieDictionary lookAhead, int nRecords) {
			final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			if (db == null) {
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

				while (cursor.moveToNext() && !lookAhead.isCancelled()) {
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

				while (cursor.moveToNext() && !lookAhead.isCancelled()) {
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

				while (cursor.moveToNext() && !lookAhead.isCancelled()) {
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
