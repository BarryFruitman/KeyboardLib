/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.old;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

public class DefaultIME extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setContentView(R.layout.select_ime);
		launchPicker();
		
		// Assign a listener to the activate button
		// findViewById(R.id.btnSelectIM).setOnClickListener(mActivateListener);
		// findViewById(R.id.btnExit).setOnClickListener(mActivateListener);
	}

	// Activate button click listener
	/*
	private OnClickListener mActivateListener = new OnClickListener() {
		public void onClick(View view) {
			if (view == findViewById(R.id.btnSelectIM))
				launchPicker();
			else
				finish();
		}
	};
	*/

	// This is a hack I found at
	// http://code.google.com/p/android/issues/detail?id=5005
	public void launchPicker() {
		InputMethodManager inputManager;
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputManager.showInputMethodPicker();

		finish();
		/*
		 * Runnable run = new Runnable() {
		 * 
		 * @Override public void run() { InputMethodManager inputManager;
		 * inputManager = (InputMethodManager)
		 * getSystemService(INPUT_METHOD_SERVICE);
		 * inputManager.showInputMethodPicker(); } };
		 * 
		 * android.os.Handler h = new android.os.Handler();
		 * h.postDelayed(run,250);
		 */
	}
}
