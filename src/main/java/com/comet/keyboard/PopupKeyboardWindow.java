/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.PopupWindow;

public class PopupKeyboardWindow extends PopupWindow implements PopupWindow.OnDismissListener {
	PopupKeyboardID mKeyboardID;
	
    public PopupKeyboardWindow(Context context) {
        super(context, null);
    }

    public PopupKeyboardWindow(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

    public PopupKeyboardWindow(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }
    
	public PopupKeyboardID getKeyboardID() {
		return mKeyboardID;
	}
	
	public void setKeyboardID(PopupKeyboardID keyboardID) {
		mKeyboardID = keyboardID;
	}

	public void onDismiss() {
		mKeyboardID = PopupKeyboardID.POPUP_KEYBOARD_NONE;
	}
}
