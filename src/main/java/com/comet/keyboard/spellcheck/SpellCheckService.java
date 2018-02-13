/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comet.keyboard.spellcheck;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.textservice.SpellCheckerService;
import android.util.Log;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

import java.util.ArrayList;
import java.util.Iterator;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.dictionary.Suggestion;
import com.comet.keyboard.dictionary.Suggestions;
import com.comet.keyboard.dictionary.Suggestor.SuggestionsExpiredException;

public class SpellCheckService extends SpellCheckerService {
    private static final boolean DBG = true;

    @Override
    public Session createSession() {
        return new AndroidSpellCheckerSession();
    }

    private static class AndroidSpellCheckerSession extends Session {

        private boolean isSentenceSpellCheckApiSupported() {
            // Note that the sentence level spell check APIs work on Jelly Bean or later.
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
        }

        private String mLocale;
        @Override
        public void onCreate() {
            mLocale = getLocale();
        }

        /**
         * This method should have a concrete implementation in all spell checker services.
         * Please note that the default implementation of
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * calls up this method. You may want to override
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * by your own implementation if you'd like to provide an optimized implementation for
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}.
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
		@Override
        public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
        	Log.d(KeyboardApp.LOG_TAG, "onGetSuggestions: " + textInfo.getText());

            int flags;
            final String input = textInfo.getText();

            // Check language dictionary
            boolean isInDictionary = KeyboardService.getIME().mSuggestor.containsIgnoreCase(input);
        	ArrayList<String> results = new ArrayList<String>();
            if(!isInDictionary) {
            	flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;

            	try {
            		// TODO Checking suggestions again is incredibly inefficient!
            		Suggestions suggestions = KeyboardService.getIME().mSuggestor.findSuggestions(input);
            		suggestions.matchCase();

            		if(suggestions.size() > 0)
            			flags |= SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS;

            		Iterator<Suggestion> iterator = suggestions.iterator();
            		while(iterator.hasNext())
            			results.add(iterator.next().getWord());
            	} catch (SuggestionsExpiredException e) {
            		// Do nothing
            	}
            } else
            	// Not a typo
            	flags = 0;

            return new SuggestionsInfo(flags, results.toArray(new String[0]));
        }

        @Override
		public SuggestionsInfo[] onGetSuggestionsMultiple(TextInfo[] textInfos,	int suggestionsLimit, boolean sequentialWords) {
			return super.onGetSuggestionsMultiple(textInfos, suggestionsLimit, sequentialWords);
		}

		/**
         * Please consider providing your own implementation of sentence level spell checking.
         * Please note that this sample implementation is just a mock to demonstrate how a sentence
         * level spell checker returns the result.
         * If you don't override this method, the framework converts queries of
         * {@link SpellCheckerService.Session#onGetSentenceSuggestionsMultiple(TextInfo[], int)}
         * to queries of
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * by the default implementation.
         */
        @Override
        public SentenceSuggestionsInfo[] onGetSentenceSuggestionsMultiple(TextInfo[] textInfos, int suggestionsLimit) {
        	return super.onGetSentenceSuggestionsMultiple(textInfos, suggestionsLimit);
        }
    }
}
