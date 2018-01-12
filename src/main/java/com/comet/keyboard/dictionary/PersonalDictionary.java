/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

//import android.content.ContentValues;
//import android.content.Context;
//import android.provider.UserDictionary;

/**
 * THIS CLASS IS DISABLED
 * @author Barry
 *
 */

//import java.util.ArrayList;
//
//import com.comet.keyboard.Suggestor.Suggestion;
//import com.comet.keyboard.Suggestor.Suggestions;
//import com.comet.keyboard.dictionary.LanguageDictionary.LanguageSuggestion;
//
//import android.content.ContentResolver;
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.provider.UserDictionary;

public class PersonalDictionary /*extends Lexicon implements Dictionary*/ {
	

//	private static ArrayList<Entry> mEntries = new ArrayList<Entry>();
//	private static Context mContext;
//	
//	
//	public PersonalDictionary(Context context, String language) {
//		super(context, language);
//	}
//
//
//	@Override
//	protected void loadLexicon() {
//		// Query the custom dictionary
//		ContentResolver contentResolver = mContext.getContentResolver();
//		Cursor cursor = contentResolver.query(UserDictionary.Words.CONTENT_URI, new String[] {UserDictionary.Words.WORD,UserDictionary.Words.FREQUENCY}, null, null, UserDictionary.Words.DEFAULT_SORT_ORDER);
//		int countSum = 0;
//
//		// Iterate through results
//		while(cursor.moveToNext()) {
//			String word = cursor.getString(0);
//			int count = Integer.parseInt(cursor.getString(1));
//			
//			
//			countSum += count;
////			Log.d(KeyboardApp.LOG_TAG, "word=" + word + ",count=" + count);
//			insert(word, count);
//		}
//		
//		cursor.close();
//	}
//
//
//
//	@Override
//	public Suggestions getSuggestions(Suggestions suggestions) {
//		String prefix = suggestions.getComposing();
//		if(prefix != null && prefix.length() < 2)
//			// Don't look up one-letter words
//			return suggestions;
//
//		// Iterate through results
//		Entry entry;
////		fragment = Word.removePunc(fragment);
//		for(int iEntry = 0; iEntry < mEntries.size(); iEntry++) {
////			matches.match(mEntries.get(iEntry));
//			entry = mEntries.get(iEntry);
//			if(entry.getWord().startsWith(prefix))
//				suggestions.add(new PersonalSuggestion(entry.getWord().toString(), entry.getRank()));
//		}
//
//		return suggestions;
//	}
//    
//
//
//    public boolean contains(String word) {
//    	word = word.toLowerCase();
//		for(int iEntry = 0; iEntry < mEntries.size(); iEntry++)
//			if(mEntries.get(iEntry).getWord().toString().equalsIgnoreCase(word))
//				return true;
//    		
//   		return false;
//    }
//
//
//
//
//	@Override
//	protected void addSuggestion(Suggestions suggestions, String word, int count, int editDistance) {
//		suggestions.add(new PersonalSuggestion(word, count, editDistance));
//	}
//
//
//
//	public static class PersonalSuggestion extends Suggestion {
//		private final int mCount;
//		
//		public PersonalSuggestion(String word, int count, int editDistance) {
//			super(word, 5);
//			mCount = count;
//		}
//
//		@Override
//		protected int compareTo(Suggestion another, String prefix) {
//			long normal = (mCount * 100) / PersonalDictionary.mMaxFrequency;  // Normalize to a number between 0 and 100
//			return (int) normal;
//		}
//	}
	
	
	
	
	
	
	
	
	

//	/**
//	 * Remove user word from custom dictionary
//	 * @param word
//	 */
//	public static void deleteWord(Context context, String word) {
//		word = word.toLowerCase();
//		context.getContentResolver().delete(UserDictionary.Words.CONTENT_URI,
//				"lower(" + UserDictionary.Words.WORD + ")=?", new String[] { word });
//		// Iterate through results
//		for(int iEntry = 0; iEntry < mEntries.size(); iEntry++)
//			if(mEntries.get(iEntry).getWord().toString().equalsIgnoreCase(word))
//				mEntries.remove(iEntry);
//	}

//	public static void addWord(Context context, String word) {
//		addWord(context, word, 1);
//	}
//	/**
//	 * Add user word to custom dictionary
//	 * @param word
//	 * @param frequency
//	 */
//	public static void addWord(Context context, String word, int frequency) {
//		word = word.toLowerCase();
//		ContentValues values = new ContentValues(1);
//		values.put(UserDictionary.Words.WORD, word);
//		context.getContentResolver().insert(UserDictionary.Words.CONTENT_URI, values);
//		UserDictionary.Words.addWord(context, word, frequency, UserDictionary.Words.LOCALE_TYPE_CURRENT);
////		mEntries.add(new Entry(word, frequency));
////		if(frequency > mMaxFrequency)
////			mMaxFrequency = frequency;
//	}


	
	
}
