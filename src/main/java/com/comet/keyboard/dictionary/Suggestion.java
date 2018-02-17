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


    public void matchCase(final String composing) {
        setWord(DictionaryUtils.matchCase(composing, getWord(),
                KeyboardService.getIME().getKeyboardView().isShifted(),
                KeyboardService.getIME().getKeyboardView().getCapsLock()));
    }


    @Override
    public boolean equals(Object object) {
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
