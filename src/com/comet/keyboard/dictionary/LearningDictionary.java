package com.comet.keyboard.dictionary;

public interface LearningDictionary extends Dictionary {
	public abstract boolean learn(String word);
	public abstract boolean remember(String word);
	public abstract boolean forget(String word);
}
