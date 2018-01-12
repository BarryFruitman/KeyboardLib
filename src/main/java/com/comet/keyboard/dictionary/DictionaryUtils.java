/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary;

import java.util.regex.Pattern;

public class DictionaryUtils {

	public static final String WORD_PUNC = "-'.& ";
	public static final String WORD_PUNC_REGEX = "[" + WORD_PUNC + "]";
	public static final Pattern patternWordPunc = Pattern.compile(WORD_PUNC_REGEX); 
	
	/*
	 * Static utility methods
	 */
	// Capitalize the first letter in word
    public static String capFirstChar(String word) {
    	if(word == null || word.length() == 0)
    		return word;

    	word = word.toLowerCase().replace('_', ' ');
		return Character.valueOf(Character.toUpperCase(word.charAt(0))).toString() + word.substring(1);
    }
    
    
    public static String removePunc(String word) {
    	if(word.matches(".*" + WORD_PUNC_REGEX + ".*"))
    		return word.replaceAll(WORD_PUNC_REGEX, "");

    	return word;
    }
    
    
    public static boolean equalsIgnorePunc(String word1, String word2) {
    	return removePunc(word1).equalsIgnoreCase(removePunc(word2));
    }


    /**
     * Checks if a word contains only upper-case letters.
     * @param word The word to check
     * @return True if it is all upper-case
     */
    public static boolean isAllCaps(String word) {
    	for(int iChar = 0; iChar < word.length(); iChar++)
    		if(!Character.isUpperCase(word.charAt(iChar)))
    			return false;
    	
    	return true;
    }


    /**
     * Checks if a word contains only upper-case letters.
     * @param word The word to check
     * @return True if it is all upper-case
     */
    public static boolean isAllLower(String word) {
    	for(int iChar = 0; iChar < word.length(); iChar++)
    		if(!Character.isLowerCase(word.charAt(iChar)))
    			return false;
    	
    	return true;
    }


    /**
     * Checks if the first and only first letter is capitalized.
     * @param word The word to check
     * @return True if the first and only first letter is capitalized
     */
    public static boolean isCapitalized(String word) {
    	return (word.length() > 0 && Character.isUpperCase(word.charAt(0)));
    }


    /**
     * Checks if the word contains a mixed of upper and lower case letters. The exception is if only the first letter
     * is upper case and the rest of the word is lower case. This is considered "capitalized" not "mixed case."
     * @param word The word to check
     * @return True if it contains both upper and lower case letters
     */
    public static boolean isMixedCase(String word) {
    	// Mixed case is a word where at least one character after the first is upper case.
    	if(word.length() < 2)
    		return false;

    	String word2 = word.substring(1);
    	return (!word.equals(word.toLowerCase()) && !word.equals(word.toUpperCase()) && !word2.equals(word2.toLowerCase()) && !word2.equals(word2.toUpperCase()));
    }


    /**
     * Converts a word to match the case of another word. If the match word is blank,
     * it matches the keyboard shift and caps lock states.
     * @param match The word to match
     * @param word The word to convert
     * @param caps The state of the shift key
     * @param capsLock The state of caps lock
     * @return The original word, with case changed if necessary
     */
    // Change the case of word to match toMatch (either all-caps or first-cap)
    public static String matchCase(String match, String word, boolean caps, boolean capsLock) {
    	if(match == null || match.equals("")) {
    		// No string to match. Use caps flags instead.
    		if(capsLock)
    			return word.toUpperCase();
    		else if(caps)
    			return capFirstChar(word);
    		else
    			return word;
    	}

    	if(isAllCaps(match))
    	{
    		if(match.length() == 1)
    			return capFirstChar(word);
    		else
    			// User is typing in all-caps
    			return word.toUpperCase();
    	}

    	if(isAllCaps(word))
    		// Suggestion is in all-caps. Do nothing.
    		return word;

    	if(isMixedCase(word))
    		// Suggestion is in mixed case. Do nothing.
    		return word;

    	if(isCapitalized(match))
    		// First letter is caps.
    		return capFirstChar(word);

    	// If word is in lower case or MiXeD CaPs, do nothing
    	return word;
    }
}
