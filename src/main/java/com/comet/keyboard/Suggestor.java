/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;

import com.comet.keyboard.dictionary.BoundedPriorityQueue;
import com.comet.keyboard.dictionary.CacheDictionary;
import com.comet.keyboard.dictionary.Dictionary;
import com.comet.keyboard.dictionary.KeyCollator;
import com.comet.keyboard.dictionary.LearningDictionary;
import com.comet.keyboard.dictionary.LookAheadDictionary;
import com.comet.keyboard.dictionary.ContactsDictionary;
import com.comet.keyboard.dictionary.DictionaryUtils;
import com.comet.keyboard.dictionary.LanguageDictionary;
import com.comet.keyboard.dictionary.ShortcutsDictionary;
import com.comet.keyboard.dictionary.NumberDictionary;
import com.comet.keyboard.dictionary.ShortcutsDictionary.ShortcutSuggestion;
import com.comet.keyboard.languages.Language;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.settings.Settings;

/**
 * Suggestor is used by the IME to produce a list of suggestions based on what the user is typing.
 * The primary method is findSuggestions(). It pulls results from the various dictionaries, ranks,
 * sorts and removes duplicates, then returns a list of suggestions to the IME.
 *
 * @author Barry Fruitman
 *
 */
public final class Suggestor {
	private KeyCollator mCollator;
	private LearningDictionary mDicLanguage;
	private LearningDictionary mDicLookAhead;
	private Dictionary mDicContacts;
	private final Dictionary mDicShortcuts;
	private final Dictionary mDicNumber;
	private static Suggestor mInstance = null;
	private SuggestionRequest mPendingRequest = null;
	private final ThreadPool mThreadPool;
	private boolean mPredictNextWord;
	private boolean mIncludeContacts;
	private Language mLanguage;


	private Suggestor(final Context context) {
		mDicContacts = new ContactsDictionary(context);
		mDicShortcuts = new ShortcutsDictionary(context);
		mDicNumber = new NumberDictionary();
		mThreadPool = new ThreadPool();
	}


	public static Suggestor getSuggestor() {
		if(mInstance == null) {
			mInstance = new Suggestor(KeyboardApp.getApp());
		}

		return mInstance;
	}


	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message result) {
			Suggestions suggestions = (Suggestions) result.obj;
			if(!suggestions.isExpired() && KeyboardService.getIME() != null) {
				KeyboardService.getIME().returnCandidates(suggestions);
			}
		}
	};


	private synchronized void newPendingRequest(final SuggestionRequest request) {
		if(mPendingRequest != null) {
			mPendingRequest.setExpired();
		}

		mPendingRequest = request;
	}


	protected void findSuggestionsAsync(final String composing) {
		final SuggestionRequest request = new SuggestionRequest(composing);

		mThreadPool.run(new Runnable() {
			public void run() {
				try {
					Suggestions suggestions = findSuggestions(request);
					if(!suggestions.isExpired()) {
						mHandler.sendMessageAtFrontOfQueue(Message.obtain(mHandler, 0, suggestions));
					}
				} catch (SuggestionsExpiredException see) {
					Log.v(KeyboardApp.LOG_TAG, "Suggestions(" + composing + ") expired");
				} catch (Exception e) {
					Log.e(KeyboardApp.LOG_TAG, "Suggestor thread exception: ", e);
				}
			}
		});
	}


	public boolean forget(final Suggestion suggestion) {
		if (suggestion instanceof LanguageDictionary.LanguageSuggestion
				|| suggestion instanceof PrefixSuggestion) {
			return mDicLanguage.forget(suggestion.getWord());
		}

		return false;
	}


	private class ThreadPool {
		private final Set<Handler> mHandlers;
		private int mThreadCount = 0;


		public ThreadPool() {
			mHandlers = new HashSet<Handler>();
		}


		public void run(final Runnable runnable) {
			final Handler handler;

			synchronized (mHandlers) {
				final Iterator<Handler> iterator = mHandlers.iterator();
				if (iterator.hasNext()) {
					handler = iterator.next();
					iterator.remove();
				} else {
					final HandlerThread thread = new HandlerThread("SuggestorThread-" + mThreadCount++);
					thread.start();
					handler = new Handler(thread.getLooper());
				}
			}

			handler.post(new Runnable() {
				@Override
				public void run() {
					runnable.run();
					synchronized (mHandlers) {
						mHandlers.add(handler);
					}
				}
			});
		}
	}


	public Suggestions findSuggestions(final String composing) {
		return findSuggestions(new SuggestionRequest(composing));
	}


	public Suggestions findSuggestions(final SuggestionRequest request) {
		final Suggestions suggestions = new Suggestions(request);

		// Terminate previous thread
		newPendingRequest(request);

		// Get look-ahead suggestions
		Suggestions lookAheadSuggestions = new Suggestions(request);
		if (mPredictNextWord) {
			lookAheadSuggestions = mDicLookAhead.getSuggestions(lookAheadSuggestions);
		}

		if(request.getComposing().length() == 0) {
			// There is no composing to match. Just return the look-ahead matches.
			suggestions.addAll(lookAheadSuggestions);
			suggestions.matchCase();
			suggestions.noDefault();
			suggestions.removeDuplicates();
			return suggestions;
		}

		// Get numeric suggestions
		final Suggestions numberSuggestions = mDicNumber.getSuggestions(new Suggestions(request));

		// Add shortcut suggestions
		final Suggestions shortcutsSuggestions = mDicShortcuts.getSuggestions(new Suggestions(request));

		// Get contact suggestions
		Suggestions contactsSuggestions = new Suggestions(request);
		if(mIncludeContacts) {
			contactsSuggestions = mDicContacts.getSuggestions(contactsSuggestions);
		}

		// Get suggestions from language dictionary
		final Suggestions languageSuggestions = mDicLanguage.getSuggestions(new Suggestions(request));

		suggestions.addAll(languageSuggestions);
		suggestions.addAll(lookAheadSuggestions);
		suggestions.addAll(numberSuggestions);
		suggestions.addAll(contactsSuggestions);
		suggestions.addAll(shortcutsSuggestions);

		// Match case of suggestions to composing.
		suggestions.matchCase();

		// Make sure composing is one of the suggestions.
		suggestions.addComposingSuggestion();

		// Remove duplicates
		suggestions.removeDuplicates();

		return suggestions;
	}


	protected void learnSuggestions(final String input) {

		mThreadPool.run(new Runnable() {
			@Override
			public void run() {
				// Check length
				if(input.length() <= 0) {
					return;
				}

				final String sentences[] = input.split("[.?!\\n]");
				for(int iSentence = 0; iSentence < sentences.length; iSentence++) {
					String sentence = sentences[iSentence].trim();
					if(sentence.length() == 0) {
						continue;
					}

					if(KeyboardService.getIME().mAutoCaps) {
						// De-capitalize the first letter.
						final StringBuilder sbGroup = new StringBuilder(sentence);
						sbGroup.replace(0, 1, String.valueOf(Character.toLowerCase(sentence.charAt(0))));
						sentence = sbGroup.toString();
					}

					final String words[] = sentence.split("[^a-zA-Z0-9'-]");
					for(int iWord = 0; iWord < words.length; iWord++) {
						// Save to language dictionary
						mDicLanguage.learn(words[iWord]);

						// Save to lookahead dictionary
						if (iWord < words.length - 2) {
							String word1 = words[iWord];
							String word2 = words[iWord + 1];
							String word3 = words[iWord + 2];
							if (!word1.matches(".*[a-zA-Z'-].*") || !word2.matches(".*[a-zA-Z'-].*") || !word3.matches(".*[a-zA-Z'-].*")) {
								// word1 and word2 don't contain any letters
								continue;
							}
							mDicLookAhead.learn(new StringBuilder(word1).append(" ").append(word2).append(" ").append(word3).toString());
						}
					}
				}
			}
		});
	}


	protected void loadPreferences() {
		final SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mIncludeContacts = sharedPrefs.getBoolean("include_contacts", false);
		mPredictNextWord = sharedPrefs.getBoolean("nextword", true);

		if(mDicLanguage == null || !mLanguage.equals(sharedPrefs.getString("language", "en"))) {
			mLanguage = Language.createLanguage(sharedPrefs.getString("language", "en"));
			mCollator = new KeyCollator(mLanguage, KeyboardLayout.getCurrentLayout());
			mDicLanguage = new LanguageDictionary(KeyboardService.getIME(), mCollator);
			mDicLookAhead = new LookAheadDictionary(KeyboardService.getIME(), mCollator);
		}
	}


	public void reloadLanguageDictionary() {
		final SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mLanguage = Language.createLanguage(sharedPrefs.getString("language", "en"));
		mCollator = new KeyCollator(mLanguage, KeyboardLayout.getCurrentLayout());
		mDicLanguage = new CacheDictionary(new LanguageDictionary(KeyboardService.getIME(), mCollator));
		mDicLookAhead = new LookAheadDictionary(KeyboardService.getIME(), mCollator);
	}


	public LearningDictionary getLanguageDictionary() {
		return mDicLanguage;
	}


	public LearningDictionary getLookAheadDictionary() {
		return mDicLookAhead;
	}


	/**
	 * Abstract parent class for all suggestion types.
	 * @author Barry Fruitman
	 *
	 */
	public static abstract class Suggestion implements Cloneable {
		protected int mOrder;
		protected String mWord;


		protected Suggestion(final String word, final int order) {
			mWord = word;
			mOrder = order;
		}


		public String getWord() {
			return mWord;
		}


		protected void setWord(final String word) {
			mWord = word;
		}


		public int getOrder() {
			return mOrder;
		}


		public double getScore() {
			return 0;
		}


		protected int compareTo(final Suggestion another, final String prefix) {
			return another.getOrder() - getOrder();
		}


		public void matchCase(final String composing) {
			setWord(DictionaryUtils.matchCase(composing, getWord(),
					KeyboardService.getIME().getKeyboardView().isShifted(),
					KeyboardService.getIME().getKeyboardView().getCapsLock()));
		}


		@Override
		public boolean equals(Object object) {
			if(object instanceof Suggestion) {
				return ((Suggestion) object).getWord().equals(mWord);
			}

			if(object instanceof String) {
				return ((String) object).equals(mWord);
			}

			return false;
		}


		@Override
		public Object clone()  {
			Suggestion s = null;
			try {
				s = (Suggestion) super.clone();
			} catch (CloneNotSupportedException e) {
				// Should never reach here since this class implements Clonable and its parent is Object.
				Assert.assertTrue("The class '" + this.getClass().getName() + "' is not clonable.", true); // Just in case.
			}
			s.mWord = new String(mWord);
			return s;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + getWord() + ")";
		}
	}


	/**
	 * Suggestions is a set of suggestions returned by the various dictionaries
	 * @author Barry Fruitman
	 *
	 */
	public class Suggestions implements Cloneable, Iterable<Suggestion> {
		private static final double MIN_SCORE_FOR_DEFAULT = 13f;
		private final SuggestionRequest mRequest;
		private int mDefault = 0;
		private BoundedPriorityQueue<Suggestion> mSuggestions;
		public static final int MAX_SUGGESTIONS = 12;


		public Suggestions(final SuggestionRequest request) {
			mRequest = request;
			mSuggestions = new BoundedPriorityQueue<Suggestion>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
		}


		public Suggestions(final Suggestions suggestions) {
			mRequest = suggestions.getRequest();
			mSuggestions = new BoundedPriorityQueue<Suggestion>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
		}


		public Suggestions(final String composing, SuggestionComparator comparator, final SuggestionRequest request) {
			mRequest = request;
			mSuggestions = new BoundedPriorityQueue<Suggestion>(comparator, MAX_SUGGESTIONS);
		}


		public String getComposing() {
			return mRequest.getComposing();
		}


		public SuggestionRequest getRequest() {
			return mRequest;
		}


		public ArrayList<Suggestion> getSuggestions() {
			return new ArrayList<Suggestion>(mSuggestions);
		}


		public ArrayList<String> getWords() {
			final Iterator<Suggestion> iterator = mSuggestions.iterator();
			final ArrayList<String> words = new ArrayList<String>();
			while(iterator.hasNext()) {
				Suggestion suggestion = iterator.next();
				words.add(suggestion.getWord());
			}

			return words;
		}


		public void add(final Suggestion suggestion) {
			if(mRequest.isExpired()) {
				// Too late
				throw new SuggestionsExpiredException();
			}

			mSuggestions.offer(suggestion);
		}


		public void addAll(final Suggestions suggestions) {
			if(mRequest.isExpired()) {
				// Too late
				throw new SuggestionsExpiredException();
			}

			mSuggestions.offerAll(suggestions.mSuggestions);
		}


		public void addAll(final Collection<Suggestion> suggestions) {
			if(mRequest.isExpired()) {
				// Too late
				throw new SuggestionsExpiredException();
			}

			mSuggestions.offerAll(suggestions);
		}


		public int size() {
			return mSuggestions.size();
		}


		public int getDefault() {
			return mDefault;
		}


		public Suggestion getSuggestion(final int i) {
			int iSuggestion = 0;
			for(Suggestion suggestion : mSuggestions) {
				if(iSuggestion++ == i) {
					return suggestion;
				}
			}

			return null;
		}


		public Suggestion getDefaultSuggestion() {
			if(mDefault < 0 || mSuggestions.size() < mDefault) {
				return null;
			}

			Iterator<Suggestion> iterator = mSuggestions.iterator();
			Suggestion result = null;
			int i = 0;
			while(iterator.hasNext()) {
				result = iterator.next();
				if(i++ == mDefault) {
					break;
				}
			}

			return result;
		}


		public void noDefault() {
			mDefault = -1;
		}


		public synchronized boolean isExpired() {
			return mRequest.isExpired();
		}


		private void addComposingSuggestion() {
			boolean hasShortcut = false;
			boolean hasPerfect = false;
			final String composing = getComposing();
			final Iterator<Suggestion> iterator = mSuggestions.iterator();
			final ArrayList<Suggestion> prefixes = new ArrayList<Suggestion>();

			while(iterator.hasNext()) {
				Suggestion suggestion = iterator.next();
				if(mCollator.compareWords(composing, suggestion.getWord())) {
					// Move this exact match to the top of the list with the prefix suggestions.
					PrefixSuggestion prefixSuggestion = new PrefixSuggestion(suggestion);
					iterator.remove();
					prefixes.add(prefixSuggestion);
					if(suggestion.getWord().equals(composing))
						// This suggestion is a perfect match with the composing
						hasPerfect = true;
				} else if(suggestion instanceof ShortcutSuggestion) {
					// This suggestion is a shortcut
					hasShortcut = true;
				}
			}

			if(prefixes.size() > 0) {
				addAll(prefixes);
				if(hasShortcut) {
					mDefault = prefixes.size();
				}
				if(!hasPerfect) {
					// Add the prefix as the perfect (non-default) match
					PrefixSuggestion prefixSuggestion = new PrefixSuggestion(composing);
					add(prefixSuggestion);
					mDefault++;
				}
			} else {
				// Add the prefix as the first match
				final PrefixSuggestion prefixSuggestion = new PrefixSuggestion(composing);
				add(prefixSuggestion);

				if(!mDicLanguage.contains(getComposing())
						&& !mDicLanguage.contains(getComposing().toLowerCase())) {
					// Don't make it the default
					mDefault++;
				}
			}

			if(getDefaultSuggestion().getScore() > MIN_SCORE_FOR_DEFAULT) {
				mDefault = -1;
			}
		}


		private void removeDuplicates() {
			final Iterator<Suggestion> iterator = mSuggestions.iterator();
			final ArrayList<String> words = new ArrayList<String>();
			int iSuggestion = -1;
			while(iterator.hasNext()) {
				iSuggestion++;
				Suggestion suggestion = iterator.next();
				if(!words.contains(suggestion.getWord())) {
					words.add(suggestion.getWord());
					continue;
				} else {
					iterator.remove();
					if(iSuggestion < mDefault) {
						mDefault--;
					} else if(iSuggestion == mDefault) {
						// Deleting the default???
						// Don't do it
						continue;
					}
				}
			}
		}


		public void matchCase() {
			matchCase(getComposing());
		}


		public void matchCase(final String match) {
			final Iterator<Suggestion> iterator = mSuggestions.iterator();
			while(iterator.hasNext()) {
				iterator.next().matchCase(match);
			}
		}


		public int findIndex(final String word) {
			int iWord = 0;
			Iterator<Suggestion> iterator = mSuggestions.iterator();
			while(iterator.hasNext()) {
				if(iterator.next().getWord().equalsIgnoreCase(word)) {
					return iWord;
				}
				iWord++;
			}

			return -1;
		}


		/**
		 * Returns a deep-copy clone.
		 */
		@Override
		public Object clone()  {
			Suggestions clone = null;
			try {
				clone = (Suggestions) super.clone();
			} catch (CloneNotSupportedException e) {
				// Should never reach here since this class implements Clonable and its parent is Object.
				Assert.assertTrue("The class '" + this.getClass().getName() + "' is not clonable.", true); // Just in case.
			}

			clone.mSuggestions = new BoundedPriorityQueue<Suggestion>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
			final Iterator<Suggestion> iterator = mSuggestions.iterator();
			while(iterator.hasNext()) {
				clone.add((Suggestion) iterator.next().clone());
			}

			return clone;
		}


		@Override
		public String toString() {
			String string = "[" + mSuggestions.size() + ": ";
			Iterator<Suggestion> iterator = mSuggestions.iterator();
			while(iterator.hasNext()) {
				string += iterator.next();
				if(iterator.hasNext()) {
					string += " , ";
				}
			}

			return string + " ]";
		}


		@Override
		public Iterator<Suggestion> iterator() {
			return mSuggestions.iterator();
		}


		/**
		 * Used by the TreeMap to compare any two instances of Suggestion. If they are the same sub-class,
		 * they are ordered by ORDER. If not, they are compared by the sub-class.
		 *
		 * @author Barry Fruitman
		 *
		 */
		private final class SuggestionComparator implements Comparator<Suggestion> {
			protected String mComposing;


			public SuggestionComparator(final String composing) {
				mComposing = composing;
			}


			@Override
			public int compare(final Suggestion lhs, final Suggestion rhs) {
				if(!lhs.getClass().equals(rhs.getClass())) {
					return lhs.getOrder() - rhs.getOrder();
				}

				return lhs.compareTo(rhs, mComposing);
			}
		}

	}


	public final class SuggestionRequest {
		private boolean mExpired = false;
		private final String mComposing;


		private SuggestionRequest(String composing) {
			mComposing = composing;
		}


		private boolean isExpired() {
			return mExpired;
		}


		private synchronized void setExpired() {
			mExpired = true;
		}


		public String getComposing() {
			return mComposing;
		}
	}


	public boolean containsIgnoreCase(final String word) {
		if(mDicLanguage.contains(word)) {
			return true;
		}

		// Check user dictionary
		if(mDicNumber.contains(word)) {
			return true;
		}

		// Check contacts dictionary
		if(mIncludeContacts && mDicContacts.contains(word)) {
			return true;
		}

		return false;
	}


	public static class PrefixSuggestion extends Suggestion {
		private final double mScore;
		private final static int ORDER = 0;


		public PrefixSuggestion(final String word) {
			super(word, ORDER);
			mScore = 0;
		}


		public PrefixSuggestion(final Suggestion suggestion) {
			super(suggestion.getWord(), 0);
			mScore = suggestion.getScore();
		}


		@Override
		protected int compareTo(final Suggestion suggestion, final String prefix) {
			if(!(suggestion instanceof PrefixSuggestion)) {
				return super.compareTo(suggestion, prefix);
			}
			PrefixSuggestion another = (PrefixSuggestion) suggestion;

			return mScore == another.mScore ? 0 : (mScore < another.mScore ? -1 : 1);
		}


		// Prefix suggestions can be remembered to the user dictionary
		public boolean canRemember() { return true; }
	}


	public static class SuggestionsExpiredException extends RuntimeException {
		private static final long serialVersionUID = -241038711087099668L;
	}
}