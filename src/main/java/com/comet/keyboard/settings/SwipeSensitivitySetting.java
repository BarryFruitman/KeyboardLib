/*
 * Comet Keyboard Library Copyright (C) 2011-2018 Comet Inc. All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Swipe Sensitivity Setting Activity
 * 
 * It allows to setup swipe sensitivity
 * 
 * @author Kuban Dzhakipov <kuban.dzhakipov@sibers.com>
 * @version $Id: SwipeSensitivitySetting.java 3056 2012-11-21 23:31:31Z cometinc $
 */
public class SwipeSensitivitySetting extends Activity implements OnSeekBarChangeListener,
View.OnClickListener, DialogInterface.OnClickListener {
	// Defines preference key name
	public static final String PREFERENCE_SWIPE_SENSITIVITY = "swipe_sensitivity";

	// default value
	public static final int SWIPE_SENSITIVITY_DEFVAL = 400;

	// Load current swipe sensitivity
	public static int getSwipeSensitivityPreference(Context context) {
		return context.getSharedPreferences(Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE).getInt(PREFERENCE_SWIPE_SENSITIVITY,
						SWIPE_SENSITIVITY_DEFVAL);
	}

	// views
	private AlertDialog mKeyboardSettingDialog;
	private SeekBar mSBSwipeSensitivity;
	private View mSettingLayout;
	private TextView mSwipeSensitivitySummary;

	private int mSwipeSensitivityVal = 0, mSwipeSensitivityPrevVal = 0; // in ms

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// User is exiting.
			finish();
			return true;
		}

		return false;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mSwipeSensitivityVal = (int) (SWIPE_SENSITIVITY_DEFVAL * 1.5) - progress;

	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	// Save vibrate time preference
	public void putSwipeSensitivityPreference() {

		SharedPreferences.Editor preferenceEditor =
				getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(PREFERENCE_SWIPE_SENSITIVITY, mSwipeSensitivityVal);

		preferenceEditor.commit();

		if(KeyboardService.getIME() != null && KeyboardService.getIME().getKeyboardView() != null)
			KeyboardService.getIME().getKeyboardView().setSwipeThreshold(mSwipeSensitivityVal);

		Log.v(KeyboardApp.LOG_TAG, "saved swipe sensitivity val = " + mSwipeSensitivityVal);

	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get configs
		mSwipeSensitivityVal = mSwipeSensitivityPrevVal = getSwipeSensitivityPreference(this);

		Log.v(KeyboardApp.LOG_TAG, "get swipe sensitivity val = " + mSwipeSensitivityVal);

		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View) getLayoutInflater().inflate(R.layout.settings_swipe_sensitivity, null);

		// setup views
		mSwipeSensitivitySummary = (TextView) mSettingLayout.findViewById(R.id.tvMainSummary);
		mSBSwipeSensitivity = (SeekBar) mSettingLayout.findViewById(R.id.sbMain);    
		mSBSwipeSensitivity.setMax(SWIPE_SENSITIVITY_DEFVAL);        
		mSBSwipeSensitivity.setProgress((int) (SWIPE_SENSITIVITY_DEFVAL*1.5) - mSwipeSensitivityVal);
		mSBSwipeSensitivity.setOnSeekBarChangeListener(this);

		showCandidateDialog();

		if (KeyboardService.getIME() != null)
			KeyboardService.getIME().setPreviewMode(true);
	}

	/**
	 * Perform button clicks in dialog view
	 */
	public void onClick(View v) {
		if (v.getId() == R.id.btTimeIncrease) {
			// increase time
			if(mSBSwipeSensitivity.getProgress() < mSBSwipeSensitivity.getMax())
				mSBSwipeSensitivity.setProgress(mSBSwipeSensitivity.getProgress() + 1);
		} else if (v.getId() == R.id.btTimeDecrease) {
			// decrease time
			if(mSBSwipeSensitivity.getProgress() > 0)
				mSBSwipeSensitivity.setProgress(mSBSwipeSensitivity.getProgress() - 1);
		}
	}

	/**
	 * Perform dialog button clicks
	 * 
	 * @param dialog
	 * @param which
	 */
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		/**
		 * Ok
		 */
		case DialogInterface.BUTTON_POSITIVE:
			// Store vibrate time value to shared preferences
			putSwipeSensitivityPreference();

			mKeyboardSettingDialog.dismiss();
			finish();
			break;

			/**
			 * Cancel
			 */
		case DialogInterface.BUTTON_NEGATIVE:
			// Store vibrate time value to shared preferences
			mSwipeSensitivityVal = mSwipeSensitivityPrevVal;
			putSwipeSensitivityPreference();

			mKeyboardSettingDialog.dismiss();
			finish();
			break;
		}
	}

	/**
	 * Show preview keyboard+candidate dialog
	 */
	private void showCandidateDialog() {
		// Create new setting dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.settings_title_swipe_sensitivity);
		builder.setView(mSettingLayout);

		mSettingLayout.findViewById(R.id.btTimeDecrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btTimeIncrease).setOnClickListener(this);

		mKeyboardSettingDialog = builder.create();

		// set new vibrate time
		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getResources().getString(R.string.ok), this);

		// cancel
		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getResources().getString(R.string.cancel), this);

		// cancel
		mKeyboardSettingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					// Store vibrate time value to shared preferences
					mSwipeSensitivityVal = mSwipeSensitivityPrevVal;
					putSwipeSensitivityPreference();

					finish();
					return true;
				}
				return false;
			}
		});

		// Show keyboard setting dialog
		mKeyboardSettingDialog.show();
	}
}
