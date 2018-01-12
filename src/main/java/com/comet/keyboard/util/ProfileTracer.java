/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;

import com.comet.keyboard.KeyboardApp;

import android.util.Log;

public class ProfileTracer {
	private long mStartTime;
	private long mLastTime;
	private String mTag = KeyboardApp.LOG_TAG;

	public ProfileTracer() {
		reset();
	}

	public ProfileTracer(String tag) {
		super();
		mTag = tag;
	}

	public void log(String message) {
		long now = System.currentTimeMillis();
		long elapsedTotal = now - mStartTime;
		long elapsedLast = now - mLastTime;
		write(message + ": (elapsed: " + elapsedLast + "/" + elapsedTotal + " ms)");
		mStartTime += System.currentTimeMillis() - now; // This should cancel out the time it took to write to the file
		mLastTime = System.currentTimeMillis();
	}
	
	protected void write(String message) {
		Log.d(mTag, message);
	}

	public void log(String message, boolean condition) {
		if(condition)
			log(message);
	}

	public void reset() {
		mStartTime = mLastTime = System.currentTimeMillis();
	}
	
	public long getElapsed() {
		return System.currentTimeMillis() - mStartTime;
	}
	
	public long getElapsedLast() {
		return System.currentTimeMillis() - mLastTime;
	}
}
