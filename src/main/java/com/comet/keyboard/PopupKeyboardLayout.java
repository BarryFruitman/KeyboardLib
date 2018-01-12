/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import com.comet.keyboard.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 * @author Barry Fruitman
 *
 */
public class PopupKeyboardLayout extends FrameLayout {
	
	protected PopupKeyboardView mPopupKeyboardView = null;

	public PopupKeyboardLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PopupKeyboardLayout(Context context) {
		super(context);
	}

	
	protected PopupKeyboardView getKeyboardView() {
		if(mPopupKeyboardView != null)
			return mPopupKeyboardView;

		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPopupKeyboardView = (PopupKeyboardView) layoutInflater.inflate(R.layout.popup_keyboard_view, null);
		addView(mPopupKeyboardView);
		return mPopupKeyboardView;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		measureChildren(widthMeasureSpec, heightMeasureSpec);

		int width = 0;
		int height = 0;
		if(mPopupKeyboardView == null) {
			width = getPaddingLeft() + getPaddingRight();
			height = getPaddingTop() + getPaddingBottom();
		} else {
			width = mPopupKeyboardView.getMeasuredWidth()
				+ mPopupKeyboardView.getPaddingLeft() + mPopupKeyboardView.getPaddingRight()
				+ getPaddingLeft() + getPaddingRight();
			height = mPopupKeyboardView.getMeasuredHeight()
				+ mPopupKeyboardView.getPaddingTop() + mPopupKeyboardView.getPaddingBottom()
				+ getPaddingTop() + getPaddingBottom();
		}
		
		int resolvedWidth = resolveSize(width, widthMeasureSpec);
		int resolvedHeight = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
	}	
}
