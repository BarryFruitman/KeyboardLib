package com.comet.keyboard.dictionary;

import com.comet.keyboard.Suggestor.Suggestions;

public interface Dictionary {
	Suggestions getSuggestions(Suggestions suggestions);
	boolean contains(String word);
}
