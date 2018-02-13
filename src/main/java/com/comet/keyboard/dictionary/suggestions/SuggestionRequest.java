package com.comet.keyboard.dictionary.suggestions;

import android.support.annotation.NonNull;

public final class SuggestionRequest {
    private boolean mExpired = false;
    private final String mComposing;
    private final Suggestor.SuggestionsListener mListener;


    /*package*/ SuggestionRequest(String composing) {
        mComposing = composing;
        mListener = null;
    }


    /*package*/ SuggestionRequest(String composing, @NonNull Suggestor.SuggestionsListener listener) {
        mComposing = composing;
        mListener = listener;
    }


    final /*package*/ boolean isExpired() {
        return mExpired;
    }


    final /*package*/ synchronized void setExpired() {
        mExpired = true;
    }


    final public String getComposing() {
        return mComposing;
    }


    final public Suggestor.SuggestionsListener getListener() {
        return mListener;
    }
}
