package com.comet.keyboard.dictionary;

import com.comet.keyboard.languages.Language;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.dictionary.radixtrie.RadixTrie.CharComparator;

public class KeyCollator implements CharComparator {
	private Language mLanguage;
	private KeyboardLayout mLayout;


	public KeyCollator(Language language, KeyboardLayout layout) {
		mLanguage = language;
		mLayout   = layout;
	}


	public KeyCollator(String languageCode, KeyboardLayout layout) {
		mLanguage = Language.createLanguage(languageCode);
		mLayout   = layout;
	}
	
	
	public Language getLanguage() {
		return mLanguage;
	}
	
	
	public String getLanguageCode() {
		return mLanguage.getCode();
	}


	public KeyboardLayout getKeyboardLayout() {
		return mLayout;
	}


	public boolean compareWords(String word1, String word2) {
		return mLanguage.compareWords(word1, word2);
	}


	/**
	 * 
	 * @param c		The character to compare.
	 * @param key	The key to compare against.
	 * @return		0 if close or exact match, 1 if typo, -1 if no match.
	 */
	public int compareCharToKey(char c, char key) {
		// Compare against each other and close (i.e. accented) letters
		if(mLanguage.compareChars(c, key) == 0)
			return 0;

		// Compare against adjacent keys
		if(mLayout.isAdjacentTo(key, c))
			// Typo
			return 1;

		return -1;
	}


	@Override
	public int compareChars(char c1, char c2) {
		return mLanguage.compareChars(c1, c2);
	}
}