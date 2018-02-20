package com.comet.keyboard.dictionary;

public interface Dictionary<S extends Suggestion, R extends SuggestionsRequest> {
	Suggestions<S> getSuggestions(R request);
	boolean contains(String word);
}
