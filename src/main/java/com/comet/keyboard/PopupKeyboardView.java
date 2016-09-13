/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import java.util.List;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.comet.keyboard.theme.KeyboardThemeManager;

public class PopupKeyboardView extends KeyboardView {

//	private Key nearestKey = null;
	private boolean mOneTouch = false;

	public PopupKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PopupKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	/**
	 * A one-touch keyboard will only stay open while the user is pressing. If not,
	 * it will stay open until the user presses a key.
	 * @return	true if this is a one-touch popup keyboard.
	 */
	public boolean isOneTouch() {
		return mOneTouch;
	}
	/**
	 * Set the one-touch flag.
	 * @param oneTouch	Set true to make this a one-touch keyboard.
	 */
	public void setOneTouch(boolean oneTouch) {
		this.mOneTouch = oneTouch;
		if(!oneTouch)
			((FrameLayout) getParent()).setBackgroundResource(R.drawable.popup_pinned_bg);
	}
	
	
	/**
	 * Sets a key as the nearest. Only used to initialize the keyboard. After that, it is updated
	 * as the user moves their finger.
	 * @param code
	 */
	public int setNearestKey(int code) {
		int def = -1;
		if(!mOneTouch)
			return def;

		List<Key> keys = getKeyboard().getKeys();
		if(keys.size() == 0)
			return def;

		for(int iKey = 0; iKey < keys.size(); iKey++) {
			Key key = keys.get(iKey);
			if(key.codes.length > 0 && key.codes[0] == code) {
				updatePreview(iKey);
				return iKey;
			}
		}
		
		return def;
	}




	/*
	 * This override of onTouchEvent waits for ACTION_MOVE events
	 * and finds the key closest to the event. 
	 */
	@Override
	public boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
		if(mOneTouch) {
			// Find the key nearest to the user's finger
			int x = (int) me.getX();
			int y = (int) me.getY();
			
			int iNearestKey = getNearestKeyIndex(x, y);

			int action = me.getAction() & MotionEvent.ACTION_MASK;
			if(action == MotionEvent.ACTION_CANCEL) {
				updatePreview(NOT_A_KEY);
				return true;

			} else if(action == MotionEvent.ACTION_MOVE) {
				// Update popup preview
				updatePreview(iNearestKey);
				return true;
				
			} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				// Close popup keyboard
				dismissAllPopupWindows();
				Key nearestKey;
				if((nearestKey = getKeyboard().getKeys().get(iNearestKey)) != null)
					// Send a key event
					if(nearestKey.text != null) {
						getOnKeyboardActionListener().onText(nearestKey.text);
					} else {
						getOnKeyboardActionListener().onKey(nearestKey.codes[0], nearestKey.codes);
					}

				updatePreview(NOT_A_KEY);

				return true;
			}
			
			return false;
		}

		return super.onModifiedTouchEvent(me, possiblePoly);
	}
	
	

	/**
	 * Find the key on this keyboard, closest to x,y.
	 * 
	 * @param x		An x-location, relative to the keyboard view.
	 * @param y		A y-location, relative to the keyboard view.
	 * @return		The index to the key nearest x,y.
	 */
	private int getNearestKeyIndex(int x, int y) {
		List<Key> keys = getKeyboard().getKeys();
		int minDist = -1;
		int iNearestKey = - 1;
		int iInsideKey = -1;
		if(keys.size() > 0) {
			// Go through every key on the keyboard and find the one closest to x,y
			for(int iKey = 0; iKey < keys.size(); iKey++) {
				Key key = keys.get(iKey);
				int dist = key.squaredDistanceFrom(x, y);
				if(key.isInside(x, y)) {
					minDist = dist;
					iInsideKey = iKey;
				} else if(dist < minDist || minDist == -1) {
					minDist = dist;
					iNearestKey = iKey;
				}
			}
		}
		
		if(iInsideKey != -1)
			return iInsideKey;
		
		return iNearestKey;
	}

	
		
	@Override
	protected Drawable getKeyBackground(Key key) {
		Drawable background = super.getKeyBackground(key);
		
		// Use zero transparency for popup keyboards
		background.setAlpha(255);

		return background;
	}

	
	
	@Override
	public void onDraw(Canvas canvas) {
		drawKeyboard(canvas);
	}
	
	

	@Override
    protected void drawBackground(Canvas canvas) {
		// Popup keyboards use a solid color background
   		canvas.drawColor(KeyboardThemeManager.getCurrentTheme().getBGColor());
    }


	@Override
	protected void loadWallpaper(Context context) {
		// Do nothing
	}


	@Override
    protected void setPhotoWallpaperBounds() {
    	// Do nothing
    }


	/**
	 * Vertically center popup labels
	 */
	@Override
	protected float getLabelY(Key key, Rect kbPadding, Rect keyPadding) {
		return getLabelCenterY(key, kbPadding, keyPadding);
	}
}
