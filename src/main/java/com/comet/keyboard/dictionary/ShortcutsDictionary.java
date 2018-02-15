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


public class ShortcutsDictionary implements Dictionary {

	private static ArrayList<ShortcutData> mShortcuts;
	private static Context mContext;

	/*package*/ ShortcutsDictionary(Context context) {
		mContext = context;
		getShortcutsList(mContext);
	}


	public static void loadFromDatabase(Context context) {
		DatabaseHelper.safeGetDatabaseHelper(context).getShortcutItems(getShortcutsList(context));
	}


	
	@Override
	public Suggestions getSuggestions(SuggestionsRequest request) {
		final Suggestions suggestions = new Suggestions(request);
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
			super(phrase, 1);
		}

		@Override
		public void matchCase(final String composing) {
			// Do nothing
		}
		
		@Override
		protected int compareTo(Suggestion another, String composing) {
			return 0;
		}
	}
}
