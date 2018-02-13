package com.comet.keyboard.suggestions;

import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.dictionary.DictionaryUtils;

import junit.framework.Assert;

/**
 * Abstract parent class for all suggestion types.
 * @author Barry Fruitman
 *
 */
public abstract class Suggestion implements Cloneable {
    protected int mOrder;
    protected String mWord;


    protected Suggestion(final String word, final int order) {
        mWord = word;
        mOrder = order;
    }


    public String getWord() {
        return mWord;
    }


    protected void setWord(final String word) {
        mWord = word;
    }


    public int getOrder() {
        return mOrder;
    }


    public double getScore() {
        return 0;
    }


    protected int compareTo(final Suggestion another, final String prefix) {
        return another.getOrder() - getOrder();
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

        if(object instanceof String) {
            return ((String) object).equals(mWord);
        }

        return false;
    }


    @Override
    public Object clone()  {
        try {
            final Suggestion s = (Suggestion) super.clone();
            s.mWord = new String(mWord);
            return s;
        } catch (CloneNotSupportedException e) {
            // Should never reach here since this class implements Clonable and its parent is Object.
            Assert.assertTrue("The class '" + this.getClass().getName() + "' is not clonable.", true); // Just in case.
        }

        return null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + getWord() + ")";
    }
}
