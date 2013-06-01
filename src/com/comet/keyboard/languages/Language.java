package com.comet.keyboard.languages;

import com.comet.keyboard.dictionary.radixtrie.RadixTrie.CharComparator;


public class Language implements CharComparator {

	protected final String mCode;

	protected Language(String code) {
		mCode = code;
	}



	public static Language createLanguage(String code) {
		Language language;
		if(code.equals("hu"))
			language = new Hungarian();
		else if(code.equals("es"))
			language = new Spanish();
		else
			// Default language
			language = new Language(code);
		
		return language;
	}



	public String getCode() {
		return mCode;
	}

	
	public int compareChars(char c1, char c2) {
		return Character.toLowerCase(c1) - Character.toLowerCase(c2);
	}


	public boolean compareWords(String word1, String word2) {
		int i1 = 0, i2 = 0;
		while(i1 < word1.length() && i2 < word2.length()) {
			char c2 = word2.charAt(i2);
			if(isWordPunc(c2)) {
				i2++;
				continue;
			}

			if(compareChars(word1.charAt(i1), c2) != 0)
				return false;
			
			i1++; i2++;
		}

		if(i1 == word1.length() && i2 == word2.length())
			return true;

		return false;
	}


	private boolean isWordPunc(char c) {
		if(c == '\'' || c == '-')
			return true;
		
		return false;
	}


	public String toString() {
		return mCode;
	}


	@Override
	public boolean equals(Object o) {
		if(o instanceof Language)
			return ((Language) o).getCode().equals(mCode);
		
		if(o instanceof String)
			return ((String) o).equals(mCode);
		
		return false;
	}
	
	
	
}
