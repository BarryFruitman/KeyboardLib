package com.comet.keyboard.dictionary;

import java.util.Iterator;
import java.util.List;

public interface Suggestions extends Iterable<Suggestion> {
    int MAX_SUGGESTIONS = 12;


    String getComposing();


    List<Suggestion> getSuggestions();


    Iterator<Suggestion> iterator();


    boolean isExpired();


    void add(Suggestion suggestion);


    void addAll(Suggestions suggestions);


    int size();
}
