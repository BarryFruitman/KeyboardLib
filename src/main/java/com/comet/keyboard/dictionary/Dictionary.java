package com.comet.keyboard.dictionary;

import com.comet.keyboard.dictionary.suggestions.Suggestions;

public interface Dictionary {
	Suggestions getSuggestions(Suggestions suggestions);
	boolean contains(String word);
}
