/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Allows to add scroll listener
 * 
 * @author Kuban Dzhakipov<kuban.dzhakipov@sibers.com>
 * @version $Id: ObservableScrollView.java 2461 2012-06-13 09:16:54Z kuban $
 * 
 */
public class ObservableScrollView extends ScrollView {

	public interface ScrollViewListener {

		void onScrollChanged(ObservableScrollView scrollView, int x, int y,
				int oldx, int oldy);

		void onScrollStarted(ObservableScrollView scrollView);

		void onScrollFinished(ObservableScrollView scrollView);

	}

	/**
	 * listener
	 */
	private ScrollViewListener mScrollViewListener = null;
	
	/**
	 * scrolling started flag
	 */
	private boolean mScrollStarted;

	public ObservableScrollView(Context context) {
		super(context);
	}

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ObservableScrollView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setScrollViewListener(ScrollViewListener scrollViewListener) {
		this.mScrollViewListener = scrollViewListener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		/**
		 * Detects events for scroll view listener
		 */
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE:
			if (!mScrollStarted && mScrollViewListener != null) {
				mScrollViewListener.onScrollStarted(this);
				mScrollStarted = true;
			}
			break;
		case MotionEvent.ACTION_UP:
			mScrollStarted = false;
			if (mScrollViewListener != null) {
				mScrollViewListener.onScrollFinished(this);
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		super.onScrollChanged(x, y, oldx, oldy);
		if (mScrollViewListener != null) {
			mScrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
		}
	}
}
