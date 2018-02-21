/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

import java.util.ArrayList;

import android.content.Context;

import com.comet.keyboard.settings.ShortcutData;
import com.comet.keyboard.util.DatabaseHelper;


public class ShortcutDictionary implements Dictionary {

	private static ArrayList<ShortcutData> mShortcuts;

	ShortcutDictionary(Context context) {
		getShortcutsList(context);
	}


	public static void loadFromDatabase(Context context) {
		DatabaseHelper.safeGetDatabaseHelper(context).getShortcutItems(getShortcutsList(context));
	}


	
	@Override
	public Suggestions getSuggestions(SuggestionsRequest request) {
		final ArraySuggestions suggestions = new ArraySuggestions(request);
		for(int iShortcut = 0; iShortcut < mShortcuts.size(); iShortcut++) {
			ShortcutData shortcut = mShortcuts.get(iShortcut);
			if(shortcut.mKeystroke.equalsIgnoreCase(suggestions.getComposing())) {
				suggestions.add(new ShortcutSuggestion(shortcut.mExpand));
				break;
			}
		}

		return suggestions;
	}
	
	
	private static ArrayList<ShortcutData> getShortcutsList(Context context) {
		if(mShortcuts  == null) {
			mShortcuts = new ArrayList<ShortcutData>();
			loadFromDatabase(context);
		}
		
		return mShortcuts;
	}



	@Override
	public boolean contains(String word) {
		// TODO: Unimplemented
		return false;
	}


	public static class ShortcutSuggestion extends Suggestion {

		public ShortcutSuggestion(String phrase) {
			super(phrase);
		}

		@Override
		public ShortcutSuggestion matchCase(final String composing) {
			// Don't alter shortcut case.
			return this;
		}
	}
}
