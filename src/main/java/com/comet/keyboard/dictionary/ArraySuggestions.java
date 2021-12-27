package com.comet.keyboard.dictionary;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Suggestions is a list of suggestions returned by the various dictionaries.
 * The suggestions are ordered by order of insertion.
 * @author Barry Fruitman
 *
 */
public class ArraySuggestions<S extends Suggestion> implements Suggestions<S> {
    private final SuggestionsRequest mRequest;
    private ArrayList<S> mSuggestions;


    ArraySuggestions(final SuggestionsRequest request) {
        mRequest = request;
        mSuggestions = new ArrayList<>();
    }


    ArraySuggestions(final SuggestionsRequest request, final Suggestions<S> suggestions) {
        this(request);
        addAll(suggestions);
    }


    @Override
    public String getComposing() {
        return mRequest.getComposing();
    }


    public SuggestionsRequest getRequest() {
        return mRequest;
    }


    @Override
    public List<S> getSuggestionsList() {
        return new ArrayList<>(mSuggestions);
    }


    public ArrayList<String> getWords() {
        final Iterator<S> iterator = mSuggestions.iterator();
        final ArrayList<String> words = new ArrayList<>();
        while(iterator.hasNext()) {
            S suggestion = iterator.next();
            words.add(suggestion.getWord());
        }

        return words;
    }


    @Override
    public boolean add(final S suggestion) {
        if(suggestion == null) {
            return false;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        return mSuggestions.add(suggestion);
    }


    public void add(final int index, final S suggestion) {
        if(suggestion == null) {
            return;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.add(index, suggestion);
    }


    public S get(final int index) {
        if(index < 0) {
            return null;
        }

        return mSuggestions.get(index);
    }


    public S get(final String word) {
        final int index = indexOf(word);
        return index >= 0 ? mSuggestions.get(index) : null;
    }


    public int indexOf(final String word) {
        int iSuggestion;
        for (iSuggestion = 0; iSuggestion < mSuggestions.size(); iSuggestion++) {
            final Suggestion suggestion = mSuggestions.get(iSuggestion);
            if(suggestion.getWord().equals(word)) {
                return iSuggestion;
            }
        }

        return -1;
    }


    public boolean remove(final S suggestion) {
        return mSuggestions.remove(suggestion);
    }


    @Override
    public void addAll(final Suggestions<S> suggestions) {
        if(suggestions == null) {
            return;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.addAll(suggestions.getSuggestionsList());
    }


    public void addAll(final int index, final Suggestions<S> suggestions) {
        if(suggestions == null) {
            return;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.addAll(index, suggestions.getSuggestionsList());
    }


    @Override
    public int size() {
        return mSuggestions.size();
    }


    public synchronized boolean isExpired() {
        return mRequest.isExpired();
    }


    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("[" + "\"" + getComposing() + "\":" + mSuggestions.size() + ": ");
        Iterator<S> iterator = mSuggestions.iterator();
        while(iterator.hasNext()) {
            string.append(iterator.next());
            if(iterator.hasNext()) {
                string.append(" , ");
            }
        }

        return string + " ]";
    }


    @Override
    @NonNull
    public Iterator<S> iterator() {
        return mSuggestions.iterator();
    }
}
