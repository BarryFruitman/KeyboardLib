package com.comet.keyboard.dictionary;

import androidx.annotation.NonNull;

class SuggestionsRequest {
    private boolean mExpired = false;
    private final String mComposing;
    private final Suggestor.SuggestionsListener mListener;


    /*package*/ SuggestionsRequest(final String composing) {
        mComposing = composing;
        mListener = null;
    }


    /*package*/ SuggestionsRequest(
            final String composing,
            @NonNull final Suggestor.SuggestionsListener listener) {
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
