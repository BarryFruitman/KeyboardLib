package com.comet.keyboard.dictionary;

import java.util.HashMap;
import java.util.Map;

public final class CacheDictionary /*implements LearningDictionary*/ {

//	final LearningDictionary mDicCached;
//	final Map<String, Suggestions> mCache = new HashMap<String, Suggestions>();
//
//	public CacheDictionary(LearningDictionary cachedDic) {
//		mDicCached = cachedDic;
//	}
//
//
//	@Override
//	public Suggestions getSuggestionsList(SuggestionsRequest request) {
//		final Suggestions suggestions = new Suggestions(request);
//		if(suggestions.getComposing().length() > 2)
//			return mDicCached.getSuggestionsList(request);
//
//		Suggestions cachedSuggestions = mCache.get(suggestions.getComposing());
//		if(cachedSuggestions != null)
//			return cachedSuggestions;
//
//		// Cache these suggestions on-the-fly
//		cachedSuggestions = mDicCached.getSuggestionsList(request);
//		mCache.put(suggestions.getComposing(), (Suggestions) cachedSuggestions.clone()); // Flag as cached
//
//		return cachedSuggestions;
//	}
//
//
//
//	@Override
//	public boolean contains(String word) {
//		return mDicCached.contains(word);
//	}
//
//
//	@Override
//	public boolean learn(String word) {
//		return mDicCached.learn(word);
//	}
//
//
//	@Override
//	public boolean remember(String word) {
//		return mDicCached.remember(word);
//	}
//
//
//	@Override
//	public boolean forget(String word) {
//		return mDicCached.forget(word);
//	}
}
