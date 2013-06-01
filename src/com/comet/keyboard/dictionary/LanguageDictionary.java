package com.comet.keyboard.dictionary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
//import android.os.Looper;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.Suggestor.Suggestion;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.layouts.KeyboardLayout;


public final class LanguageDictionary extends TrieDictionary {

	private UserDictionary mDicUser;
	private static TrieDictionary mLoading = null;
	private int mCountSum = 0;


	public LanguageDictionary(Context context, KeyCollator collator) {
		super(context, collator);
		mDicUser  = new UserDictionary(mContext, collator);
	}


	@Override
	public void loadDictionary() {
		Thread.currentThread().setName("LanguageLoader-" + mCollator.getLanguageCode());
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		if(mLoading != null)
			mLoading.cancel();
		mLoading = this;

		// Check if dictionary file exists
		if(!KeyboardApp.getApp().getUpdater().isDictionaryExist(mContext, mCollator.getLanguageCode()))
			return;
		
		// Skip "other" language
		if(mCollator.getLanguageCode().equals(mContext.getString(R.string.lang_code_other)))
			return;

		// TODO Replace this with OnLoadLexiconListener interface
		KeyboardService ime = KeyboardService.getIME(); 
		if(ime != null && ime.isInputViewCreated() && !ime.isNeedUpdateDicts())
			KeyboardService.getIME().showMessage(mContext.getString(R.string.dictionary_loading_message), null);

		// Load dictionary from DB
		DictionaryDB languageDB = new LanguageDictionaryDB(mContext, mCollator.getLanguageCode());
		
		// Pre-load first 5,000 records for quick response.
		mCountSum = languageDB.loadDictionaryFromDB(this, 5000);

		// Clear the cache of any suggestions that were cached during pre-loading
		if(ime != null && ime.isInputViewCreated() && !ime.isNeedUpdateDicts())
			KeyboardService.getIME().clearMessage();

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		// Now load all records.
		mCountSum = languageDB.loadDictionaryFromDB(this, -1);
	}
	
	

	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		if(mCountSum <= 0)
			return suggestions;

		/*
		 * 1. Find conjoined bigrams
		 * 2. Add static suggestions
		 * 3. Merge user suggestions
		 */
		return mDicUser.getSuggestions(
						super.getSuggestions(
						findConjoinedBiGrams(suggestions))); 
	}



	private Suggestions findConjoinedBiGrams(Suggestions languageSuggestions) {
		// Check for conjoined bi-grams (e.g. "areyou")
		if(languageSuggestions.getComposing().length() >= 5) {
			// Check if this is a conjoined bi-gram
			for(int iPrefix = 1; iPrefix < languageSuggestions.getComposing().length() - 1; iPrefix++) {
				// Split into two words
				String word1 = languageSuggestions.getComposing().substring(0, iPrefix + 1);
				String word2 = languageSuggestions.getComposing().substring(iPrefix + 1, languageSuggestions.getComposing().length());
				int count = ((TrieDictionary) KeyboardService.getIME().getSuggestor().getLookAheadDictionary()).getCount(word1 + " " + word2);
				if(count > 0) {
					int count1 = getCount(word1);
					int count2 = getCount(word2);
					// This bi-gram is in the LookAhead dictionary, therefore it is common enough to suggest.
					addSuggestion(languageSuggestions, word1 + " " + word2, Math.max(count1, count2), EditDistance.getJoined());
				}
			}

			// Check if this is a bi-gram conjoined by a space-adjacent key
			for(int iPrefix = 1; iPrefix < languageSuggestions.getComposing().length() - 1; iPrefix++) {
				if(KeyboardLayout.getCurrentLayout().isAdjacentToSpaceBar(languageSuggestions.getComposing().charAt(iPrefix))) {
					// Split into two words, omitting space-adjacent key
					String word1 = languageSuggestions.getComposing().substring(0, iPrefix);
					String word2 = languageSuggestions.getComposing().substring(iPrefix + 1, languageSuggestions.getComposing().length());
					int count = ((TrieDictionary) KeyboardService.getIME().getSuggestor().getLookAheadDictionary()).getCount(word1 + " " + word2);
					if(count > 0) {
						int count1 = getCount(word1);
						int count2 = getCount(word2);
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
		
		
//		private int getCount() {
//			return mCount;
//		}
		
		
		private double getFrequency() {
			return mFrequency;
		}
		
		
		private void setFrequency(double frequency) {
			mFrequency = frequency;
			mScore = computeScore();
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
			if(mScore == 0)
				mScore = computeScore();
			
			return mScore;
		}
		
		
		protected void setScore(double score) {
			mScore = score;
		}
		
		
		@Override
		protected int compareTo(Suggestion suggestion, String prefix) {
			if(!(suggestion instanceof LanguageDictionary.LanguageSuggestion))
				return super.compareTo(suggestion, prefix);

			LanguageSuggestion another = (LanguageSuggestion) suggestion;

			double score = getScore();
			double otherScore = another.getScore();

			if(score == otherScore)
				return suggestion.getWord().compareTo(another.getWord());

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
		if(super.contains(word))
			return true;
		
		return mDicUser.contains(word);
	}



	@Override
	public boolean matches(String word) {
		if(super.matches(word))
			return true;

		return mDicUser.matches(word);
	}



	@Override
	public boolean learn(String word) {
		return mDicUser.learn(word);
	}



	@Override
	public boolean forget(String word) {
		return mDicUser.forget(word) && !super.contains(word);
	}



	@Override
	public boolean remember(String word) {
		if(super.contains(word))
			// Cannot remember a word in the static dictionary
			return false;
		
		return mDicUser.remember(word);
	}




	private class LanguageDictionaryDB extends DictionaryDB {

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
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();

			if(db == null)
				return 0;

			int countSum = 0;
			try {
				String limit = null;
				if(nRecords > 0)
					limit = String.valueOf(nRecords);

				Cursor cursor = db.query("lexicon", new String[] {"word", "count"}, "count>=0",
						null, null, null, "count DESC", limit);

				if (cursor == null)
					return 0;

				while(cursor.moveToNext() && !lexicon.isCancelled()) {
					String word = cursor.getString(0);
					int count = cursor.getInt(1);
					countSum += count;
					lexicon.insert(word, count);
				}

				cursor.close();

			} catch (SQLiteException e) {
				Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			}

			return countSum;
		}
	}
	
	


	/**
	 * Class UserDictionary
	 * @author Barry
	 *
	 */
//	private static Looper mContentObserverLooper = null;
	private final class UserDictionary extends TrieDictionary {

		private final int MIN_COUNT = 2; // Count threshold for suggestions
		private int mCountSum = 0;

		private UserDictionary(Context context, KeyCollator collator) {
			super(context, collator);
		}



		@Override
		public void loadDictionary() {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			UserDB userDB = UserDB.getUserDB(mContext, mCollator.getLanguageCode());

			// Load all records.
			mCountSum = userDB.loadLanguage(this) + 50000;
			
			// Wait for changes to the Android user dictionary
			waitForChanges();
		}



		private void waitForChanges() {
//			// Quit the previous Looper, if any
//			if(mContentObserverLooper != null)
//				mContentObserverLooper.quit();
//
//			// Prepare a looper for this thread
//			Looper.prepare();
//			mContentObserverLooper = Looper.myLooper();
//
//			// Register a ContentObserver
//			mContext.getContentResolver().registerContentObserver(android.provider.UserDictionary.Words.CONTENT_URI, false,
//					new ContentObserver(new Handler(this)) {
//				@Override
//				public void onChange(boolean selfChange) {
//					super.onChange(selfChange);
//
//					// Look for changes in the user dictionary
//				}
//			});
//
//			// Enter a message queue loop and wait for changes
//			Looper.loop();
		}


		
		@Override
		public Suggestions getSuggestions(Suggestions suggestions) {
			if(mCountSum <= 0)
				return suggestions;

			/*
			 * 1. Find user suggestions
			 * 2. Merge into suggestions
			 */
			return super.getSuggestions(suggestions);
		}

		
		
		@Override
		protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
			if(count < MIN_COUNT) // Ignore results with low count. This weeds out the accidents and one-timers.
				return;
			
			// First, check if this word is already in suggestions
			LanguageSuggestion suggestion = null;
			for(Suggestion s : suggestions) {
				if(!(s instanceof LanguageSuggestion))
					// Skip other suggestion types
					continue;
				if(s.equals(word)) {
					// Merge this suggestion into existing one
					suggestion = (LanguageSuggestion) s;
					suggestion.setFrequency(
							(suggestion.getFrequency() * 0.99) + ((count / mCountSum) * 0.01));
				}
			}

			if(suggestion == null) {
				// Add new suggestion at 1%
				suggestion = new LanguageSuggestion(word, count, mCountSum, editDistance);
//				suggestion.setFrequency(suggestion.getFrequency() * 0.01);
				suggestions.add(suggestion);
			}

	
			suggestions.add(suggestion);
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
		 * Remember a word. Remembered words have the minimum count necessary to appear
		 * in suggestions returned by getSuggestions().
		 * 
		 * @param word		The word to remember.
		 * 
		 * @return			false if the word was already remembered. 
		 */
		@Override
		public boolean remember(String word) {
			if(isRemembered(word))
				return false;

			learn(word, MIN_COUNT);

			return true;
		}



		/**
		 * Returns true if the word can appear in suggestions.
		 * @param word	The word to check.
		 * @return
		 */
		public boolean isRemembered(String word) {
			if(getCount(word) >= MIN_COUNT)
				return true;

			return false;
		}



		/**
		 * Adds a word to the dictionary with desired count, or increments its count by 1
		 * @param word		The word to learn
		 * @param count		The default count for new words
		 */
		@Override
		protected int learn(String word, int count) {
			count = super.learn(word, count);

			mCountSum += count;

			// Write to db
			UserDB.getUserDB(mContext, mCollator.getLanguageCode()).addWordToLexicon(mCollator.getLanguageCode(), word, count);
			
			return count;
		}


		/**
		 * Remove this word from the dictionary.
		 * @param word		The word to forget.
		 */
		@Override
		public boolean forget(String word) {
			super.forget(word);

			// Write to db
			UserDB.getUserDB(mContext, mCollator.getLanguageCode()).deleteWordFromLexicon(mCollator.getLanguageCode(), word);
			
			return true;
		}
	}
}
