package com.comet.keyboard.dictionary;

import com.comet.keyboard.KeyboardService;

/**
 * Abstract parent class for all suggestion types.
 * @author Barry Fruitman
 *
 */
public abstract class Suggestion {
    private String mWord;


    /*package*/ Suggestion(final String word) {
        mWord = word;
    }


    public String getWord() {
        return mWord;
    }


    protected void setWord(final String word) {
        mWord = word;
    }


    public Suggestion matchCase(final String composing) {
        setWord(DictionaryUtils.matchCase(composing, getWord(),
                KeyboardService.getIME().getKeyboardView().isShifted(),
                KeyboardService.getIME().getKeyboardView().getCapsLock()));

        return this;
    }



    public boolean matches(final String word) {
        return normalize(mWord).equalsIgnoreCase(normalize(word));
    }


    static String normalize(final String word) {
        return word.replaceAll("[^a-zA-Z0-9 ]", "");
    }


    @Override
    public boolean equals(final Object object) {
        if(object instanceof Suggestion) {
            return ((Suggestion) object).getWord().equals(mWord);
        }

        return object instanceof String && object.equals(mWord);
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + getWord() + ")";
    }
}
