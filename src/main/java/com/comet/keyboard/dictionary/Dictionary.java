package com.comet.keyboard.dictionary;

public interface Dictionary {
	Suggestions getSuggestions(SuggestionsRequest request);
	boolean contains(String word);
}
