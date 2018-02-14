package com.comet.keyboard.dictionary;

public interface Dictionary {
	Suggestions getSuggestions(SuggestionRequest request);
	boolean contains(String word);
}
