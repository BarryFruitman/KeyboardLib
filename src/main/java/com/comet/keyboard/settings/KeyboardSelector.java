/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class KeyboardSelector extends Activity implements OnClickListener {
	public static String INTENT_PARM_SAVE_PROFILE_KEY = "save_profile";
	
	// Selecting Dialog
	private AlertDialog mDlgSelecting;
	private RadioGroup mRGLanguage;
	
	// available keyboard names and values
	private String[] mKBNames;
	private String[] mKBValues;
	
	// selected keyboard name
	private String mCurrKeyboard;
	
	private boolean mInSettings = false;

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get parameter
		Bundle bundle = getIntent().getBundleExtra(Settings.BUNDLE_KEY);
		if (bundle != null) {
			mInSettings = bundle.getBoolean(Settings.IN_SETTINGS, false);
		}

		// Load keyboard
		mKBNames = getResources().getStringArray(R.array.keyboard_names);
		mKBValues = getResources().getStringArray(R.array.keyboard_ids);

		Assert.assertTrue(mKBNames != null);
		Assert.assertTrue(mKBValues != null);

		mCurrKeyboard = getKeyboardPreference(this);

		showSelectDialog();
	}

	/**
	 * Save last preference
	 */
	public void saveSelectedKeyboard() {
		// Update language profile
		updateLanguageProfile();
		
		// Save current keyboard to preference
		putKeyboardPreference();
		
		Intent intent = new Intent();
		setResult(Activity.RESULT_OK, intent);
	}
	
	
	/**
	 * Show selecting dialog
	 */
	private void showSelectDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View languageLayout = inflater.inflate(R.layout.keyboard_options,
				(ViewGroup) findViewById(R.id.llKeyboards));

		builder.setTitle(R.string.install_select_keyboard_title);
		builder.setView(languageLayout);

		mDlgSelecting = builder.create();
		mDlgSelecting.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					saveSelectedKeyboard();
					finish();
					return true;
				}
				return false;
			}
		});
		
		// Get component from dialog resource
		mRGLanguage = (RadioGroup) languageLayout.findViewById(R.id.rgKeyboards);
		// Load language options
		for (int i = 0 ; i < mKBNames.length ; i++) {
			RadioButton newOption = new RadioButton(this);
			if(KeyboardApp.getApp().mAppStore == KeyboardApp.AppStore.Nook)
				newOption.setTextColor(getResources().getColor(R.color.black));
			newOption.setId(i);
			newOption.setText(mKBNames[i]);
			newOption.setOnClickListener(this);
			mRGLanguage.addView(newOption);
		}
		
		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mDlgSelecting.show();
	}

	/**************************************************************************
	 * UI Event Handler
	 *************************************************************************/
	@Override
	public void onClick(View arg0) {
		int selectedID;

		Assert.assertTrue(mDlgSelecting != null);
		Assert.assertTrue(mDlgSelecting.isShowing());

		selectedID = mRGLanguage.getCheckedRadioButtonId();
		mCurrKeyboard = mKBValues[selectedID];

		saveSelectedKeyboard();
		done();
	}
	
	
	protected void done() {
		finish();
	}
	
	public void launchHome() {
		// Launch the keyboard settings app
		Intent intent = new Intent();
		intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
		startActivity(intent);
	}

	// Load keyboard preference
	public static String getKeyboardPreference(Context context) {
		return KeyboardLayout.getCurrentLayout().getId();
	}

	// Save keyboard preference
	public void putKeyboardPreference() {
		KeyboardLayout.setCurrentLayout(mCurrKeyboard);
	}

	// Update language profile
	private void updateLanguageProfile() {
		LanguageProfileManager manager = LanguageProfileManager.getProfileManager();
		LanguageProfile profile = manager.getCurrentProfile();
		manager.addProfile(profile.getLang(), mCurrKeyboard);
	}
}
