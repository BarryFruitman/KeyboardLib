/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.wizard;

import com.comet.keyboard.Home;
import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


public class DefaultIME extends Activity {

	AlertDialog mDialog;
	View mLayout;
	boolean mShowInfo = true;
	boolean mShowPicker = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}



	@Override
	public void onWindowFocusChanged (boolean focus) {
		Log.d(KeyboardApp.LOG_TAG, "DefaultIME.onWindowFocusChanged(" + focus + ")");
		
		if(!focus)
			return;
		
		if(Utils.isSelectedToDefault(this)) {
			// Move to next step in setup
			done();
			return;
		}

		if(mShowInfo) {
			// Show the info dialog
			showInfoDialog();
			mShowInfo = false;
			mShowPicker = true;
		} else if(mShowPicker) {
			// Show the picker
			showPicker();
			mShowInfo = false;
			mShowPicker = false;
		} else
			// User failed. Prompt to try again.
			tryAgain();
	}
	
	

	/**
	 * Show the initial info dialog that tells the user what to do
	 */
	private void showInfoDialog() {
		
		AlertDialog.Builder builder;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		else
			builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.ime_short_name));
		builder.setMessage(getString(R.string.install_wizard_default_ime_dialog, getString(R.string.ime_name)));
		AlertDialog dialog = builder.create();
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Dialog is dismissed. onWindowFocusChanged() will show the picker
				}
			}
		);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	

	/**
	 * The user failed to make TS the default. Prompt to try again or skip.
	 */
	private void showTryAgainDialog() {
		AlertDialog.Builder builder;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		else
			builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.warning);
		builder.setMessage(getString(R.string.confirm_enable_ime_description, getString(R.string.ime_short_name)));
		AlertDialog dialog = builder.create();
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.yes), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Reset dialogs
					mShowInfo = true;
					mShowPicker = false;
				}
			}
		);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
				getResources().getString(R.string.no), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Move to next step
					done();
				}
			}
		);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}



	private void showPicker() {
		InputMethodManager inputManager;
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);            
		inputManager.showInputMethodPicker();
	}



	private void done() {
		Installer.logInstallStep("set_default");
		
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), Home.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

		finish();
	}
	
	
	
	private void tryAgain() {
		showTryAgainDialog();
	}
}
