package com.comet.keyboard.dictionary;

public interface LearningDictionary<S extends Suggestion, R extends SuggestionsRequest>
	   extends Dictionary<S, R> {
	public abstract boolean learn(String word);
	public abstract boolean remember(String word);
	public abstract boolean forget(String word);
}
