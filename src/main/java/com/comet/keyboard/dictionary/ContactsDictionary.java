/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;


public class ContactsDictionary implements Dictionary {
	
	private Context mContext;

	public ContactsDictionary(Context context) {
		mContext = context;
	}

	
	
	@Override
	public Suggestions getSuggestions(SuggestionsRequest request) {
		final ArraySuggestions suggestions = new ArraySuggestions(request);
		String composing = suggestions.getComposing();
		if(composing != null && composing.length() < 5)
			// Don't look up short words
			return suggestions;

		composing = composing.toLowerCase();

		// We only need the names
		String[] columns = new String[] {PhoneLookup.DISPLAY_NAME};

		// Query the custom dictionary
		ContentResolver contentResolver = mContext.getContentResolver();
		Cursor cursor = contentResolver.query(
				ContactsContract.Contacts.CONTENT_URI,
				columns,
				PhoneLookup.DISPLAY_NAME + " LIKE \"" + composing + "%\" OR "
						+ PhoneLookup.DISPLAY_NAME + " LIKE \"% " + composing + "%\"",
				null,
				PhoneLookup.DISPLAY_NAME);
		if(cursor == null)
			return suggestions;

		// Iterate through the results
		while(cursor.moveToNext()) {
	        String name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
	        String names[] = name.split(" ");
	        for(int iName = 0; iName < names.length; iName++)
	        	if(names[iName].toLowerCase().startsWith(composing) && suggestions.getSuggestionsList().contains(names[iName]))
	        		suggestions.add(new ContactSuggestion(names[iName]));
		}
		cursor.close();

		return suggestions;
	}


	private static class ContactSuggestion extends Suggestion {
		public ContactSuggestion(String name) {
			super(name);
		}
	}



	@Override
	public boolean contains(String word) {
		return false;
	}
}
