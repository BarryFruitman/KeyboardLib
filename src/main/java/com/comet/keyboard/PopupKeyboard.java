/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;

public class PopupKeyboard extends BaseKeyboard {
    
    public PopupKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public PopupKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }
    
    
    protected float getLabelY(Key key, Rect kbPadding, Rect keyPadding, Paint paint) {
    	return key.y + kbPadding.top + (key.height - keyPadding.top - keyPadding.bottom) / 2 + (paint.getTextSize() - paint.descent()) / 2 + keyPadding.top;
    }
}
