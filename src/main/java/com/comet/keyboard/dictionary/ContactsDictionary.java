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

	/*package*/ ContactsDictionary(Context context) {
		mContext = context;
	}

	
	
	@Override
	public Suggestions getSuggestions(Suggestions suggestions) {
		String prefix = suggestions.getComposing();
		if(prefix != null && prefix.length() < 5)
			// Don't look up short words
			return suggestions;

		prefix = prefix.toLowerCase();

		// We only need the names
		String[] columns = new String[] {PhoneLookup.DISPLAY_NAME};

		// Query the custom dictionary
		ContentResolver contentResolver = mContext.getContentResolver();
		Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, columns, PhoneLookup.DISPLAY_NAME + " LIKE \"" + prefix + "%\" OR " + PhoneLookup.DISPLAY_NAME + " LIKE \"% " + prefix + "%\"", null, null);
		if(cursor == null)
			return suggestions;

		// Iterate through the results
		while(cursor.moveToNext()) {
	        String name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
	        String names[] = name.split(" ");
	        for(int iName = 0; iName < names.length; iName++)
	        	if(names[iName].toLowerCase().startsWith(prefix) && suggestions.findIndex(names[iName]) != -1)
	        		suggestions.add(new ContactSuggestion(names[iName]));
		}
		cursor.close();

		return suggestions;
	}



	public static class ContactSuggestion extends Suggestion {

		public ContactSuggestion(String name) {
			super(name, 2);
		}

		@Override
		protected int compareTo(Suggestion another, String prefix) {
			return 0;
		}
	}



	@Override
	public boolean contains(String word) {
		return false;
	}
}
