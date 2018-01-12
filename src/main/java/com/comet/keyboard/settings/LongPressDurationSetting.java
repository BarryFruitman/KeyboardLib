/*
 * Comet Keyboard Library Copyright (C) 2011-2018 Comet Inc. All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.KeyboardView;
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
import android.view.ViewConfiguration;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Long Press Duration Setting Activity
 * 
 * It allows to setup long key press time
 * 
 * @author Kuban Dzhakipov <kuban.dzhakipov@sibers.com>
 * @version $Id: VibrateSetting.java 2278 2012-05-25 11:45:36Z kuban $
 */
public class LongPressDurationSetting extends Activity implements OnSeekBarChangeListener,
View.OnClickListener, DialogInterface.OnClickListener {
	// Defines preference key name
	public static final String PREFERENCE_LONG_PRESS_DURATION = "long_press_duration";  

	// Load current long press duration, otherwise it returns default time in ms
	public static int getLongPressDurationPreference(Context context) {
		return context.getSharedPreferences(Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE).getInt(PREFERENCE_LONG_PRESS_DURATION,
						ViewConfiguration.getLongPressTimeout());
	}


	// views
	private AlertDialog mKeyboardSettingDialog;
	private SeekBar mSBLongPressDuration;
	private View mSettingLayout;
	private TextView mTvLongPressDurationSummary;

	private int mLongPressDuration = 0, mLongPressDurationPrev = 0; // in ms

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
		mLongPressDuration = progress + ViewConfiguration.getLongPressTimeout()/2;

		updateSummary();
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	public void updateSummary(){
		String summary = getString(R.string.long_press_duration_summary, mLongPressDuration);

		mTvLongPressDurationSummary.setText(summary);	  
	}

	// Save vibrate time preference
	public void putLongPressDurationPreference() {

		SharedPreferences.Editor preferenceEditor =
				getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(PREFERENCE_LONG_PRESS_DURATION, KeyboardView.LONG_PRESS_TIMEOUT =  mLongPressDuration);
		preferenceEditor.commit();

		Log.v(KeyboardApp.LOG_TAG, "saved long press duration = " + mLongPressDuration);

	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get configs
		mLongPressDuration = mLongPressDurationPrev = getLongPressDurationPreference(this);

		Log.v(KeyboardApp.LOG_TAG, "get long press duration = " + mLongPressDuration);

		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View) getLayoutInflater().inflate(R.layout.settings_long_press_duration, null);

		// setup views
		mTvLongPressDurationSummary = (TextView) mSettingLayout.findViewById(R.id.tvLongPressDurationSummary);
		mSBLongPressDuration = (SeekBar) mSettingLayout.findViewById(R.id.sbLongPressDuration);    
		mSBLongPressDuration.setMax((int) (ViewConfiguration.getLongPressTimeout() * 1.5));        
		mSBLongPressDuration.setProgress(mLongPressDuration - ViewConfiguration.getLongPressTimeout()/2);
		mSBLongPressDuration.setOnSeekBarChangeListener(this);

		updateSummary();

		showCandidateDialog();
	}

	/**
	 * Perform button clicks in dialog view
	 */
	public void onClick(View v) {
		if (v.getId() == R.id.btTimeIncrease) {
			// increase time
			if(mSBLongPressDuration.getProgress() < mSBLongPressDuration.getMax())
				mSBLongPressDuration.setProgress(mSBLongPressDuration.getProgress() + 1);
		} else if (v.getId() == R.id.btTimeDecrease) {
			// decrease time
			if(mSBLongPressDuration.getProgress() > 0)
				mSBLongPressDuration.setProgress(mSBLongPressDuration.getProgress() - 1);
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
			putLongPressDurationPreference();

			mKeyboardSettingDialog.dismiss();
			finish();
			break;

			/**
			 * Cancel
			 */
		case DialogInterface.BUTTON_NEGATIVE:
			// Store vibrate time value to shared preferences
			mLongPressDuration = mLongPressDurationPrev;
			putLongPressDurationPreference();

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

		builder.setTitle(R.string.settings_title_long_press_duration);
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
					mLongPressDuration = mLongPressDurationPrev;
					putLongPressDurationPreference();

					finish();
					return true;
				}
				return false;
			}
		});

		mKeyboardSettingDialog.setCancelable(false);

		// Show keyboard setting dialog
		mKeyboardSettingDialog.show();
	}
}
