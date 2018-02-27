/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.layouts.KeyboardLayout;
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
		// Save current keyboard to preference
		putKeyboardPreference();
		
		Intent intent = new Intent();
		setResult(Activity.RESULT_OK, intent);
	}
	
	/**
	 * Get default keyboard name
	 */
//	private String getDefaultKeyboardName() {
//		Resources resource;
//		
//		String currLanguage, currCountry;
//		Locale currLocale;
//		String keyboard;
//		
//		resource = getResources();
//		currLocale = java.util.Locale.getDefault();
//		currLanguage = currLocale.getLanguage();
//		currCountry = currLocale.getCountry();
//		
//		if (currLanguage.equals(resource.getString(R.string.lang_code_english))) {
//			keyboard = resource.getString(R.string.kb_id_qwerty_en);
//		} else if (currLanguage.equals(resource.getString(R.string.lang_code_spanish))) {
//			keyboard = resource.getString(R.string.kb_id_qwerty_sp);
//		} else if (currLanguage.equals(resource.getString(R.string.lang_code_french))) {
//			if (currCountry.equals(resource.getString(R.string.country_belgium))) {
//				keyboard = resource.getString(R.string.kb_id_azerty_be);
//			} else if (currCountry.equals(resource.getString(R.string.country_canada))) {
//				keyboard = resource.getString(R.string.kb_id_qwerty_intl);
//			} else {
//				keyboard = resource.getString(R.string.kb_id_azerty_fr);
//			}
//		} else if (currLanguage.equals(resource.getString(R.string.lang_code_german))) {
//			keyboard = resource.getString(R.string.kb_id_qwertz_de);
//		} else {
//			keyboard = resource.getString(R.string.kb_id_qwerty_intl);
//		}
//		
//		return keyboard;
//	}
	
	/**
	 * Show selecting dialog
	 */
	private void showSelectDialog() {
//		String autoDetectedKeyboardName;
//		boolean isChecked = false;
		
//		autoDetectedKeyboardName = getDefaultKeyboardName();
		
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
			newOption.setId(i);
			newOption.setText(mKBNames[i]);
			newOption.setOnClickListener(this);
			mRGLanguage.addView(newOption);
			
//			if (autoDetectedKeyboardName.equals(mKBValues[i])) {
//				mRGLanguage.check(i);
//				isChecked = true;
//			}
		}
		
//		if (!isChecked) {
//			mRGLanguage.check(1);
//		}
		
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

		// Close selecting dialog
//		mDlgSelecting.dismiss();
//		mDlgSelecting = null;

		saveSelectedKeyboard();
		done();
	}
	
	
	private void done() {
		if(mInSettings)
			finish();
		else {
//			Installer.setCurrStep(this, Installer.InstallStep.INSTALL_FINISHED);

			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setClass(getApplicationContext(), EnableIME.class);
			startActivity(intent);
		}
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
}
