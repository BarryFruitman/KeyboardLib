package com.comet.keyboard.dictionary;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Suggestions is a list of suggestions returned by the various dictionaries.
 * The suggestions are sorted using the provided Comparator and truncated to a max size.
 * @author Barry Fruitman
 *
 */
public class SortedSuggestions<S extends Suggestion> implements Suggestions<S> {
    private final SuggestionsRequest mRequest;
    private final BoundedPriorityQueue<S> mSuggestions;


    SortedSuggestions(final SuggestionsRequest request, final Comparator<S> comparator) {
        mRequest = request;
        mSuggestions = new BoundedPriorityQueue<>(comparator, MAX_SUGGESTIONS);
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
            Suggestion suggestion = iterator.next();
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

        return mSuggestions.offer(suggestion);
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

        mSuggestions.offerAll(suggestions.getSuggestionsList());
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
