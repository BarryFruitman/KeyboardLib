/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

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


public class LookAheadDictionary extends TrieDictionary {

	private final UserDictionary mDicUser;
	private static LookAheadDictionary mLoadingLexicon = null;

	public LookAheadDictionary(Context context, KeyCollator collator) {
		super(context, collator);
		
		mDicUser = new UserDictionary(context, collator);
	}


	@Override
	protected void loadDictionary() {
		Thread.currentThread().setName("LookAheadLoader-" + mCollator.getLanguageCode());
		
		if(mLoadingLexicon != null)
			mLoadingLexicon.cancel();
		mLoadingLexicon = this;

		// Check if dictionary file exists
		if(!KeyboardApp.getApp().getUpdater().isDictionaryExist(mContext, mCollator.getLanguageCode()))
			return;
		
		// Skip "other" language
		if(mCollator.getLanguageCode().equals(mContext.getString(R.string.lang_code_other)))
			return;

		// Load lexicon from DB
		final DictionaryDB lookAheadDB = new LookAheadDictionaryDB(mContext, mCollator.getLanguageCode());
		ProfileTracer tracer = new ProfileTracer();
		
		tracer.log("LookAheadDictionary.loadLexicon()...");

		lookAheadDB.loadDictionaryFromDB(this, -1);

		tracer.log("LookAheadDictionary.loadLexicon(): ...done populating");

//		langDbHelper.close();
	}


	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		StringBuilder word1 = new StringBuilder();
		StringBuilder word2 = new StringBuilder();
		KeyboardService.getIME().getTwoWordsBeforePrefix(word1, word2);

		word1 = new StringBuilder(word1.toString().toLowerCase());
		word2 = new StringBuilder(word2.toString().toLowerCase());

		/*
		 * 1. Get static suggestions
		 * 2. Merge user suggestions
		 */
		return mDicUser.getSuggestions(getSuggestions(word1, word2, suggestions));
	}



	private Suggestions getSuggestions(StringBuilder word1, StringBuilder word2, Suggestions suggestions) {
		// Depth = 2
		if(word1.length() > 0 && word2.length() > 0) {
			String prefix = word1 + " " + word2;
			int countSum = getCount(prefix);
			prefix += " ";
			LookAheadSuggestions lookAheadSuggestions2 = new LookAheadSuggestions(suggestions, prefix, countSum, 2);
			super.getSuggestionsWithPrefix(lookAheadSuggestions2, prefix);
			suggestions.addAll(lookAheadSuggestions2);
		}

		// Depth = 1
		if(word2.length() > 0) {
			String prefix = word2.toString();
			int countSum = getCount(prefix);
			prefix += " ";
			LookAheadSuggestions lookAheadSuggestions1 = new LookAheadSuggestions(suggestions, prefix, countSum, 1);
			super.getSuggestionsWithPrefix(lookAheadSuggestions1, prefix);
			suggestions.addAll(lookAheadSuggestions1);
		}
		
		
		return suggestions;
	}



	@Override
	protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
		double frequency = (double) count / (double) ((LookAheadSuggestions) suggestions).mCountSum;
		suggestions.add(new LookAheadSuggestion(word, frequency, editDistance, ((LookAheadSuggestions) suggestions).mDepth));
	}
	
	

	@Override
	public boolean learn(String word) {
		return mDicUser.learn(word);
	}
	
	
	
	@Override
	public boolean forget(String word) {
		if(super.contains(word))
			// Cannot unlearn a word in the static dictionary
			return false;
		
		return mDicUser.forget(word);
	}



	@Override
	public boolean remember(String word) {
		if(super.contains(word))
			// Cannot remember a word in the static dictionary
			return false;
		
		return mDicUser.remember(word);
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



	private class LookAheadSuggestion extends Suggestion {
		private final double mFrequency;
		private final int mEditDistance;
		private double mScore = 0;
		private final int mDepth;

		public LookAheadSuggestion(String word, double frequency, int editDistance, int depth) {

			super(word, 4);
			
			mFrequency = frequency;
			mEditDistance = editDistance;
			mDepth = depth;
		}


		private double computeScore() {

			// Compute frequency
			double frequency = mFrequency;

			// Normalize
			double score = Math.abs(Math.log10(frequency));

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

		
		private void setScore(double score) {
			mScore = score;
		}
		
		
		@Override
		protected int compareTo(Suggestion suggestion, String prefix) {
			if(!(suggestion instanceof LookAheadSuggestion))
				return super.compareTo(suggestion, prefix);

			LookAheadSuggestion another = (LookAheadSuggestion) suggestion;

			if(mDepth != another.mDepth)
				return another.mDepth - mDepth;

			// Is either one an exact match?
			if(mEditDistance == 0 && mWord.length() == prefix.length())
				return -1;
			else if(another.mEditDistance == 0 && another.mWord.length() == prefix.length())
				return 1;

			// Compare scores
			double score = getScore();
			double otherScore = another.getScore();

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
	private final class UserDictionary extends TrieDictionary implements Dictionary {

		private final int MIN_COUNT = 2; // Count threshold for suggestions

		public UserDictionary(Context context, KeyCollator collator) {
			super(context, collator);
		}



		@Override
		public void loadDictionary() {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			UserDB userDB = UserDB.getUserDB(mContext, mCollator.getLanguageCode());

			// Now load all records.
			userDB.loadLookAhead(this, -1);
		}

		
		
		@Override
		public Suggestions getSuggestions(Suggestions suggestions) {
			StringBuilder word1 = new StringBuilder();
			StringBuilder word2 = new StringBuilder();
			KeyboardService.getIME().getTwoWordsBeforePrefix(word1, word2);

			word1 = new StringBuilder(word1.toString().toLowerCase());
			word2 = new StringBuilder(word2.toString().toLowerCase());

			return getSuggestions(word1, word2, suggestions);
		}


	
		private Suggestions getSuggestions(StringBuilder word1, StringBuilder word2, Suggestions suggestions) {
			// Depth = 2
			if(word1.length() > 0 && word2.length() > 0) {
				String prefix = word1 + " " + word2;
				int countSum = getCount(prefix);
				prefix += " ";
				LookAheadSuggestions lookAheadSuggestions2 = new LookAheadSuggestions(suggestions, prefix, countSum, 2);
				super.getSuggestionsWithPrefix(lookAheadSuggestions2, prefix);
				suggestions.addAll(lookAheadSuggestions2);
			}

			// Depth = 1
			if(word2.length() > 0) {
				String prefix = word2.toString();
				int countSum = getCount(prefix);
				prefix += " ";
				LookAheadSuggestions lookAheadSuggestions1 = new LookAheadSuggestions(suggestions, prefix, countSum, 1);
				super.getSuggestionsWithPrefix(lookAheadSuggestions1, prefix);
				suggestions.addAll(lookAheadSuggestions1);
			}

			return suggestions;
		}



		@Override
		protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
			if(count < MIN_COUNT) // Ignore results with low count. This weeds out the accidents and one-timer.
				return;

			LookAheadSuggestion suggestion = null;
			double frequency = (double) count / ((double) ((LookAheadSuggestions) suggestions).mCountSum + 5000);

			// First, check if this word is already in suggestions
			for(Suggestion s : suggestions) {
				if(!(s instanceof LookAheadSuggestion))
					// Skip other suggestion types
					continue;
				if(s.equals(word)) {
					// Merge this suggestion into existing one
					suggestion = (LookAheadSuggestion) s;
					s = new LookAheadSuggestion(word, frequency, editDistance, ((LookAheadSuggestions) suggestions).mDepth);
					suggestion.setScore((suggestion.getScore() * 0.5) + (s.getScore() * 0.5));
				}
			}

			if(suggestion == null) {
				// Add new suggestion at 1%
				suggestion = new LookAheadSuggestion(word, frequency, editDistance, ((LookAheadSuggestions) suggestions).mDepth);
//				suggestion.setFrequency(suggestion.getFrequency() * 0.01);
				suggestions.add(suggestion);
			}

	
			suggestions.add(suggestion);
		}
		
		
		
		/**
		 * Adds a word to the dictionary with count = 1, or increments its count by 1
		 * @param word		The word to learn
		 */
		public boolean learn(String trigram) {
			int count = getCount(trigram);
			String words[] = trigram.split(" ");
			if(words.length != 3)
				return false;

			if(count > 0) {
				// Update trie entry
				setCount(trigram, ++count);
			} else {
				// Insert into trie
				count = 1;
				insert(words[0], 1);
				insert(words[0] + " " + words[1], 1);
				insert(trigram, 1);
			}

			// Write to db
			UserDB.getUserDB(mContext, mCollator.getLanguageCode()).addTriGramToLookAhead(mCollator.getLanguageCode(), words[0], words[1], words[2], count);
			
			return true;
		}



		@Override
		public boolean forget(String word) {
			super.forget(word);

			// Write to db
			UserDB.getUserDB(mContext, mCollator.getLanguageCode()).deleteWordFromLookAhead(mCollator.getLanguageCode(), word);

			return true;
		}

		
		
		public boolean isRemembered(String word) {
			if(getCount(word) >= MIN_COUNT)
				return true;

			return false;
		}



		/**
		 * Adds a word to the dictionary with count = MIN_COUNT, or increments its count by 1
		 * @param word		The word to remember
		 */
		@Override
		public boolean remember(String word) {
			if(isRemembered(word))
				return false;
			
			learn(word, MIN_COUNT);
			
			return true;
		}
	}
	
	
	
	/**
	 * 
	 * @author Barry
	 *
	 */
	private class LookAheadDictionaryDB extends DictionaryDB {

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
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			if(db == null)
				return 0;

			int countSum = 0;
			try {
				String limit = null;
				if(nRecords > 0)
					limit = String.valueOf(nRecords);

				// Compute 1-gram sums
				Cursor cursor = db.query("trigrams", new String[] {"word1", "SUM(count) AS count"}, null,
						null, "word1", null, "count DESC", limit);
				if (cursor == null)
					return 0;

				while(cursor.moveToNext() && !lookAhead.isCancelled()) {
					String word1 = cursor.getString(0);
					int count = cursor.getInt(1);
					
					lookAhead.insert(word1, count);
				}
				cursor.close();

				
				// Compute 2-gram sums
				cursor = db.query("trigrams", new String[] {"word1", "word2", "SUM(count) AS count"}, null,
						null, "word1,word2", null, "count DESC", limit);
				if (cursor == null)
					return 0;

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
				if (cursor == null)
					return 0;

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
			}
			
			return countSum;
		}
	}
}
