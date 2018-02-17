package com.comet.keyboard.dictionary;

import java.util.List;

public interface Suggestions<S extends Suggestion> extends Iterable<S> {
    int MAX_SUGGESTIONS = 12;


    String getComposing();


    List<S> getSuggestionsList();


    boolean isExpired();


    boolean add(S suggestion);


    void addAll(Suggestions<S> suggestions);


    int size();
}
