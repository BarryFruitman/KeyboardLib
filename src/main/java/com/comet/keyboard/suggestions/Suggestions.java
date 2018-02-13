package com.comet.keyboard.suggestions;

/**
 * Created by barry on 2/13/18.
 */

import com.comet.keyboard.dictionary.BoundedPriorityQueue;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Suggestions is a set of suggestions returned by the various dictionaries
 * @author Barry Fruitman
 *
 */
public class Suggestions implements Cloneable, Iterable<Suggestion> {
    private final SuggestionRequest mRequest;
    /*package*/ int mDefault = 0;
    private BoundedPriorityQueue<Suggestion> mSuggestions;
    public static final int MAX_SUGGESTIONS = 12;


    public Suggestions(final SuggestionRequest request) {
        mRequest = request;
        mSuggestions = new BoundedPriorityQueue<>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
    }


    public Suggestions(final Suggestions suggestions) {
        mRequest = suggestions.getRequest();
        mSuggestions = new BoundedPriorityQueue<>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
    }


    public String getComposing() {
        return mRequest.getComposing();
    }


    public SuggestionRequest getRequest() {
        return mRequest;
    }


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


    public void add(final Suggestion suggestion) {
        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.offer(suggestion);
    }


    public void addAll(final Suggestions suggestions) {
        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.offerAll(suggestions.mSuggestions);
    }


    public void addAll(final Collection<Suggestion> suggestions) {
        if(mRequest.isExpired()) {
            // Too late
            throw new Suggestor.SuggestionsExpiredException();
        }

        mSuggestions.offerAll(suggestions);
    }


    public int size() {
        return mSuggestions.size();
    }


    public int getDefault() {
        return mDefault;
    }


    public Suggestion getSuggestion(final int i) {
        int iSuggestion = 0;
        for(Suggestion suggestion : mSuggestions) {
            if(iSuggestion++ == i) {
                return suggestion;
            }
        }

        return null;
    }


    public Suggestion getDefaultSuggestion() {
        if(mDefault < 0 || mSuggestions.size() < mDefault) {
            return null;
        }

        Iterator<Suggestion> iterator = mSuggestions.iterator();
        Suggestion result = null;
        int i = 0;
        while(iterator.hasNext()) {
            result = iterator.next();
            if(i++ == mDefault) {
                break;
            }
        }

        return result;
    }


    public void noDefault() {
        mDefault = -1;
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


    /**
     * Returns a deep-copy clone.
     */
    @Override
    public Object clone()  {
        Suggestions clone = null;
        try {
            clone = (Suggestions) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never reach here since this class implements Clonable and its parent is Object.
            Assert.assertTrue("The class '" + this.getClass().getName() + "' is not clonable.", true); // Just in case.
        }

        clone.mSuggestions = new BoundedPriorityQueue<Suggestion>(new SuggestionComparator(getComposing()), MAX_SUGGESTIONS);
        final Iterator<Suggestion> iterator = mSuggestions.iterator();
        while(iterator.hasNext()) {
            clone.add((Suggestion) iterator.next().clone());
        }

        return clone;
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
