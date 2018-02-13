package com.comet.keyboard.dictionary;

public interface Dictionary {
	Suggestions getSuggestions(Suggestions suggestions);
	boolean contains(String word);
}
