package com.comet.keyboard.dictionary;

import com.comet.keyboard.suggestions.Suggestions;

public interface Dictionary {
	Suggestions getSuggestions(Suggestions suggestions);
	boolean contains(String word);
}
