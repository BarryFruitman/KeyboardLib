/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
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
	private LanguageDictionary mDicLanguage;
	private LookAheadDictionary mDicLookAhead;
	private Dictionary mDicContacts;
	private final Dictionary mDicShortcuts;
	private final Dictionary mDicNumber;
	private static Suggestor mInstance = null;
	private SuggestionsRequest mPendingRequest = null;
	private final ThreadPool mThreadPool;
	private boolean mPredictNextWord;
	private boolean mIncludeContacts;
	private Language mLanguage;
	private Context mContext;

	private static final double MIN_SCORE_FOR_DEFAULT = 13f;


	private Suggestor(final Context context) {
		mDicContacts = new ContactsDictionary(context);
		mDicShortcuts = new ShortcutDictionary(context);
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


	private Handler mHandler = new SuggestionsHandler();


	private static class SuggestionsHandler extends Handler {
		@Override
		public void handleMessage(final Message result) {
			FinalSuggestions suggestions = (FinalSuggestions) result.obj;
			if(!suggestions.isExpired()
					&& suggestions.getRequest() != null
					&& suggestions.getRequest().getListener() != null) {
				suggestions.getRequest().getListener().onSuggestionsReady(suggestions);
			}
		}
	}


	private synchronized void newPendingRequest(final SuggestionsRequest request) {
		if(mPendingRequest != null) {
			mPendingRequest.setExpired();
		}

		mPendingRequest = request;
	}


	public void findSuggestionsAsync(final String composing, final SuggestionsListener listener) {
		final SuggestionsRequest request = new SuggestionsRequest(composing, listener);

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
		void onSuggestionsReady(FinalSuggestions suggestions);
	}


	public boolean forget(final Suggestion suggestion) {
		if (suggestion instanceof LanguageDictionary.LanguageSuggestion
				|| suggestion instanceof ComposingSuggestion) {
			return mDicLanguage.forget(suggestion.getWord());
		}

		return false;
	}


	private class ThreadPool {
		private final Set<Handler> mHandlers;
		private int mThreadCount = 0;


		ThreadPool() {
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


	public FinalSuggestions findSuggestions(final String composing) {
		return findSuggestions(new SuggestionsRequest(composing));
	}


	private FinalSuggestions findSuggestions(final SuggestionsRequest request) {
		final FinalSuggestions finalSuggestions = new FinalSuggestions(request);

		// Terminate previous thread
		newPendingRequest(request);

		// Get look-ahead suggestions
		Suggestions lookAheadSuggestions = null;
		if (mPredictNextWord) {
			StringBuilder word1 = new StringBuilder();
			StringBuilder word2 = new StringBuilder();
			KeyboardService.getIME().getTwoWordsBeforeComposing(word1, word2);

			final LookAheadDictionary.LookAheadSuggestionsRequest lookAheadRequest =
			new LookAheadDictionary.LookAheadSuggestionsRequest(
					request,
					word1.toString(),
					word2.toString());
			lookAheadSuggestions = mDicLookAhead.getSuggestions(lookAheadRequest);
		}

		if(request.getComposing().length() == 0) {
			// There is no composing to match. Just return the look-ahead matches.
			finalSuggestions.addAll(lookAheadSuggestions);
			finalSuggestions.matchCase();
			finalSuggestions.removeDuplicates();
			finalSuggestions.setNoDefault();
			return finalSuggestions;
		}

		// Get numeric suggestions
		final Suggestions numberSuggestions = mDicNumber.getSuggestions(request);

		// Add shortcut suggestions
		final Suggestions shortcutsSuggestions = mDicShortcuts.getSuggestions(request);

		// Get contact suggestions
		Suggestions contactsSuggestions = null;
		if(mIncludeContacts) {
			contactsSuggestions = mDicContacts.getSuggestions(request);
		}

		// Get suggestions from language dictionary
		final Suggestions languageSuggestions = mDicLanguage.getSuggestions(request);

		// Merge all suggestions into FinalSuggestions.
		finalSuggestions.addAll(shortcutsSuggestions);
		finalSuggestions.addAll(numberSuggestions);
		finalSuggestions.addAll(contactsSuggestions);
		finalSuggestions.addAll(lookAheadSuggestions);
		finalSuggestions.addAll(languageSuggestions);

		// Match case of suggestions to composing.
		finalSuggestions.matchCase();

		// Make sure composing is one of the suggestions.
		finalSuggestions.addComposing();

		// Remove duplicates
		finalSuggestions.removeDuplicates();

		// Set default suggestion
		finalSuggestions.assignDefault();

		return finalSuggestions;
	}


	public class FinalSuggestions extends ArraySuggestions<Suggestion> {
		private int mDefaultIndex;


		private FinalSuggestions(SuggestionsRequest request) {
			super(request);
		}


		public int getDefaultIndex() {
			return mDefaultIndex;
		}


		public void assignDefault() {
			if(size() == 0) {
				setNoDefault();
				return;
			}

			// Set the default to the shortcut or highest-ranking language or look-ahead suggestion
			for (final Suggestion suggestion : this) {
				if (suggestion instanceof ShortcutDictionary.ShortcutSuggestion
						|| suggestion instanceof LanguageDictionary.LanguageSuggestion
						|| suggestion instanceof LookAheadDictionary.LookAheadSuggestion) {
					mDefaultIndex = indexOf(suggestion.getWord());
					break;
				}
			}

			if(mDefaultIndex == -1) {
				// Composing is the default if there is no other.
				mDefaultIndex = 0;
			} else if(mDefaultIndex > 0
					&& getComposing().length() == 1
					&& !get(mDefaultIndex).matches(getComposing())) {
				// Single letter composing suggestions should always be the default
				// unless it's a real word.
				mDefaultIndex = 0;
			}
		}


		public void setNoDefault() {
			mDefaultIndex = -1;
		}


		public void matchCase() {
			final Iterator<Suggestion> iterator = iterator();
			while(iterator.hasNext()) {
				iterator.next().matchCase(getComposing());
			}
		}


		public void addComposing() {
			// Move the top match to the front.
			final Suggestion topMatch = getTopMatch();
			if(topMatch != null) {
				remove(topMatch);
				add(0, topMatch);
			}

			// If the exact match isn't at the front, make it a
			// ComposingSuggestion and move it to the front.
			final int iExact = indexOf(getComposing());
			if(iExact > 0) {
				remove(get(iExact));
				add(0, new ComposingSuggestion(getComposing()));
			}

			// Is the composing still missing?
			if(get(getComposing()) == null) {
				add(0, new ComposingSuggestion(getComposing()));
			}
		}


		private void removeDuplicates() {
			final Iterator<Suggestion> iterator = iterator();
			final ArrayList<String> words = new ArrayList<>();
			int iSuggestion = -1;
			while(iterator.hasNext()) {
				iSuggestion++;
				Suggestion suggestion = iterator.next();
				if(!words.contains(suggestion.getWord())) {
					words.add(suggestion.getWord());
					continue;
				} else {
					iterator.remove();
					if(iSuggestion < mDefaultIndex) {
						mDefaultIndex = mDefaultIndex - 1;
					} else if(iSuggestion == mDefaultIndex) {
						// Deleting the default???
						// Don't do it
						continue;
					}
				}
			}
		}


		private Suggestion getTopMatch() {
			for(final Suggestion suggestion : this) {
				if(suggestion.matches(getComposing())) {
					return suggestion;
				}
			}

			return null;
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

					if(KeyboardService.getIME().getIsAutoCaps()
							&& !DictionaryUtils.isAllCaps(sentence)
							&& !DictionaryUtils.isMixedCase(sentence)) {
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
//						if (iWord < words.length - 2) {
//							String word1 = words[iWord];
//							String word2 = words[iWord + 1];
//							String word3 = words[iWord + 2];
//							if (!word1.matches(".*[a-zA-Z'-].*") || !word2.matches(".*[a-zA-Z'-].*") || !word3.matches(".*[a-zA-Z'-].*")) {
//								// word1 and word2 don't contain any letters
//								continue;
//							}
//							mDicLookAhead.learn(new StringBuilder(word1).append(" ").append(word2).append(" ").append(word3).toString());
//						}
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
		mDicLanguage = new LanguageDictionary(mContext, mCollator);
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


	private static class ComposingSuggestion extends Suggestion {
		private ComposingSuggestion(final String word) {
			super(word);
		}
	}


	public static class SuggestionsExpiredException extends RuntimeException {
		private static final long serialVersionUID = -241038711087099668L;
	}
}