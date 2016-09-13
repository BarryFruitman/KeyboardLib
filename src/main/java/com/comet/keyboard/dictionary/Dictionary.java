package com.comet.keyboard.dictionary;

import com.comet.keyboard.Suggestor.Suggestions;

public interface Dictionary {
	public abstract Suggestions getSuggestions(Suggestions suggestions);
	public abstract boolean matches(String word);
	public abstract boolean contains(String word);
}
