/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
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
	private Context mContext;

	private static final double MIN_SCORE_FOR_DEFAULT = 13f;


	private Suggestor(final Context context) {
		mDicContacts = new ContactsDictionary(context);
		mDicShortcuts = new ShortcutsDictionary(context);
		mDicNumber = new NumberDictionary();
		mThreadPool = new ThreadPool();
		mContext = context;
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
			if(!suggestions.isExpired()
					&& suggestions.getRequest() != null
					&& suggestions.getRequest().getListener() != null) {
				suggestions.getRequest().getListener().onSuggestionsReady(suggestions);
			}
		}
	};


	private synchronized void newPendingRequest(final SuggestionRequest request) {
		if(mPendingRequest != null) {
			mPendingRequest.setExpired();
		}

		mPendingRequest = request;
	}


	public void findSuggestionsAsync(final String composing, final SuggestionsListener listener) {
		final SuggestionRequest request = new SuggestionRequest(composing, listener);

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


	public interface SuggestionsListener {
		void onSuggestionsReady(Suggestions suggestions);
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
		Suggestions lookAheadSuggestions =
				new Suggestions(request);
		if (mPredictNextWord) {
			lookAheadSuggestions = mDicLookAhead.getSuggestions(lookAheadSuggestions);
		}

		if(request.getComposing().length() == 0) {
			// There is no composing to match. Just return the look-ahead matches.
			suggestions.addAll(lookAheadSuggestions);
			suggestions.matchCase();
			suggestions.noDefault();
			removeDuplicates(suggestions);
			return suggestions;
		}

		// Get numeric suggestions
		final Suggestions numberSuggestions =
				mDicNumber.getSuggestions(new Suggestions(request));

		// Add shortcut suggestions
		final Suggestions shortcutsSuggestions =
				mDicShortcuts.getSuggestions(new Suggestions(request));

		// Get contact suggestions
		Suggestions contactsSuggestions =
				new Suggestions(request);
		if(mIncludeContacts) {
			contactsSuggestions = mDicContacts.getSuggestions(contactsSuggestions);
		}

		// Get suggestions from language dictionary
		final Suggestions languageSuggestions =
				mDicLanguage.getSuggestions(new Suggestions(request));

		suggestions.addAll(languageSuggestions);
		suggestions.addAll(lookAheadSuggestions);
		suggestions.addAll(numberSuggestions);
		suggestions.addAll(contactsSuggestions);
		suggestions.addAll(shortcutsSuggestions);

		// Match case of suggestions to composing.
		suggestions.matchCase();

		// Make sure composing is one of the suggestions.
		addComposingSuggestion(suggestions);

		// Remove duplicates
		removeDuplicates(suggestions);

		return suggestions;
	}


	private void addComposingSuggestion(Suggestions suggestions) {
		boolean hasShortcut = false;
		boolean hasPerfect = false;
		final String composing = suggestions.getComposing();
		final Iterator<Suggestion> iterator = suggestions.iterator();
		final Suggestions prefixes = new Suggestions(suggestions.getRequest());

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
			suggestions.addAll(prefixes);
			if(hasShortcut) {
				suggestions.mDefault = prefixes.size();
			}
			if(!hasPerfect) {
				// Add the prefix as the perfect (non-default) match
				PrefixSuggestion prefixSuggestion = new PrefixSuggestion(composing);
				suggestions.add(prefixSuggestion);
				suggestions.mDefault++;
			}
		} else {
			// Add the prefix as the first match
			final PrefixSuggestion prefixSuggestion = new PrefixSuggestion(composing);
			suggestions.add(prefixSuggestion);

			if(!mDicLanguage.contains(suggestions.getComposing())
					&& !mDicLanguage.contains(suggestions.getComposing().toLowerCase())) {
				// Don't make it the default
				suggestions.mDefault++;
			}
		}

		if(suggestions.getDefaultSuggestion().getScore() > MIN_SCORE_FOR_DEFAULT) {
			suggestions.mDefault = -1;
		}
	}


	private void removeDuplicates(Suggestions suggestions) {
		final Iterator<Suggestion> iterator = suggestions.iterator();
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
				if(iSuggestion < suggestions.mDefault) {
					suggestions.mDefault--;
				} else if(iSuggestion == suggestions.mDefault) {
					// Deleting the default???
					// Don't do it
					continue;
				}
			}
		}
	}


	public void learnSuggestions(final String input) {

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

					if(KeyboardService.getIME().getIsAutoCaps()) {
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


	public void loadPreferences() {
		final SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mIncludeContacts = sharedPrefs.getBoolean("include_contacts", false);
		mPredictNextWord = sharedPrefs.getBoolean("nextword", true);

		if(mDicLanguage == null || !mLanguage.equals(sharedPrefs.getString("language", "en"))) {
			mLanguage = Language.createLanguage(sharedPrefs.getString("language", "en"));
			mCollator = new KeyCollator(mLanguage, KeyboardLayout.getCurrentLayout());
			mDicLanguage = new LanguageDictionary(mContext, mCollator);
			mDicLookAhead = new LookAheadDictionary(mContext, mCollator);
		}
	}


	public void reloadLanguageDictionary() {
		final SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mLanguage = Language.createLanguage(sharedPrefs.getString("language", "en"));
		mCollator = new KeyCollator(mLanguage, KeyboardLayout.getCurrentLayout());
		mDicLanguage = new CacheDictionary(new LanguageDictionary(mContext, mCollator));
		mDicLookAhead = new LookAheadDictionary(mContext, mCollator);
	}


	public LearningDictionary getLanguageDictionary() {
		return mDicLanguage;
	}


	public LearningDictionary getLookAheadDictionary() {
		return mDicLookAhead;
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
	}


	public static class SuggestionsExpiredException extends RuntimeException {
		private static final long serialVersionUID = -241038711087099668L;
	}
}