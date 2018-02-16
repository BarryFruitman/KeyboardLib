package com.comet.keyboard.dictionary;

/**
 * Created by barry on 2/13/18.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Suggestions is a set of suggestions returned by the various dictionaries
 * @author Barry Fruitman
 *
 */
public class SortedSuggestions implements Suggestions, Cloneable, Iterable<Suggestion> {
    protected final SuggestionsRequest mRequest;
    private BoundedPriorityQueue<Suggestion> mSuggestions;


    /*package*/ SortedSuggestions(final SuggestionsRequest request) {
        mRequest = request;
        mSuggestions = new BoundedPriorityQueue<>(
                new SuggestionComparator(getComposing()),
                MAX_SUGGESTIONS);
    }


    @Override
    public String getComposing() {
        return mRequest.getComposing();
    }


    public SuggestionsRequest getRequest() {
        return mRequest;
    }


    @Override
    public ArrayList<Suggestion> getSuggestions() {
        return new ArrayList<Suggestion>(mSuggestions);
    }


    public ArrayList<String> getWords() {
        final Iterator<Suggestion> iterator = mSuggestions.iterator();
        final ArrayList<String> words = new ArrayList<String>();
        while(iterator.hasNext()) {
            Suggestion suggestion = iterator.next();
            words.add(suggestion.getWord());
        }

        return words;
    }


    @Override
    public void add(final Suggestion suggestion) {
        if(suggestion == null) {
            return;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.offer(suggestion);
    }


    @Override
    public void addAll(final Suggestions suggestions) {
        if(suggestions == null) {
            return;
        }

        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.offerAll(suggestions.getSuggestions());
    }


    @Override
    public int size() {
        return mSuggestions.size();
    }


    public Suggestion getSuggestionAt(final int i) {
        int iSuggestion = 0;
        for(Suggestion suggestion : mSuggestions) {
            if(iSuggestion++ == i) {
                return suggestion;
            }
        }

        return null;
    }


    public synchronized boolean isExpired() {
        return mRequest.isExpired();
    }


    public void matchCase() {
        matchCase(getComposing());
    }


    public void matchCase(final String match) {
        final Iterator<Suggestion> iterator = mSuggestions.iterator();
        while(iterator.hasNext()) {
            iterator.next().matchCase(match);
        }
    }


    public int findIndex(final String word) {
        int iWord = 0;
        Iterator<Suggestion> iterator = mSuggestions.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().getWord().equalsIgnoreCase(word)) {
                return iWord;
            }
            iWord++;
        }

        return -1;
    }


    @Override
    public String toString() {
        String string = "[" + mSuggestions.size() + ": ";
        Iterator<Suggestion> iterator = mSuggestions.iterator();
        while(iterator.hasNext()) {
            string += iterator.next();
            if(iterator.hasNext()) {
                string += " , ";
            }
        }

        return string + " ]";
    }


    @Override
    public Iterator<Suggestion> iterator() {
        return mSuggestions.iterator();
    }


    /**
     * Used by the TreeMap to compare any two instances of Suggestion. If they are the same sub-class,
     * they are ordered by ORDER. If not, they are compared by the sub-class.
     *
     * @author Barry Fruitman
     *
     */
    private final class SuggestionComparator implements Comparator<Suggestion> {
        protected String mComposing;


        public SuggestionComparator(final String composing) {
            mComposing = composing;
        }


        @Override
        public int compare(final Suggestion lhs, final Suggestion rhs) {
            if(!lhs.getClass().equals(rhs.getClass())) {
                return lhs.getOrder() - rhs.getOrder();
            }

            return lhs.compareTo(rhs, mComposing);
        }
    }

}
