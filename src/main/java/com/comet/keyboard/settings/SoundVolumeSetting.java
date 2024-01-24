/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;

/**
 * Volume Setting Activity
 * 
 * It allows to setup key click volume
 * 
 * @author Kuban Dzhakipov <kuban.dzhakipov@sibers.com>
 * @version $Id: VibrateSetting.java 2511 2012-07-27 05:14:33Z kuban $
 */
public class SoundVolumeSetting extends Activity implements
		OnSeekBarChangeListener, View.OnClickListener,
		DialogInterface.OnClickListener  {
	// Defines preference key name
	public static final String PREFERENCE_VIBRATE = "sound_volume";
	
	public static final int STREAM_TYPE = AudioManager.STREAM_SYSTEM;

	private SoundPool mSoundPool;
	private SparseIntArray mSoundsMap;

	// Load current vibrate setting time, otherwise it returns default time in ms
	public static float getVolumePreference(Context context) {
		return context.getSharedPreferences(Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE).getFloat(PREFERENCE_VIBRATE, 0);
	}

	// views
	private AlertDialog mKeyboardSettingDialog;
	private SeekBar mSBVolume;
	private View mSettingLayout;
	private TextView mTvVolumeSummary;

	private int mVolumeVal = 0, mVolumeValPrev = 0;

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
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		mVolumeVal = progress;

		String summary = getString(R.string.volume_val_summary, progress);

		mTvVolumeSummary.setText(summary);
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
	public void putVolumeValPreference() {

		SharedPreferences.Editor preferenceEditor = getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		
		float newVal = (float) mVolumeVal / 100;

		preferenceEditor.putFloat(PREFERENCE_VIBRATE, newVal);
		preferenceEditor.commit();
		
		if(KeyboardService.IME != null){
			KeyboardService.IME.setSoundVolume(newVal);
		}

		Log.v(KeyboardApp.LOG_TAG, "saved volume val=" + mVolumeVal);

	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get configs
		mVolumeVal = mVolumeValPrev = (int) (getVolumePreference(this) * 100);

		Log.v(KeyboardApp.LOG_TAG, "get volume val=" + mVolumeVal);
		
		// Sound
		mSoundPool = new SoundPool(4, STREAM_TYPE, 100);
		mSoundsMap = KeyboardService.loadSounds(this, mSoundPool);

		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View) getLayoutInflater().inflate(
				R.layout.settings_volume, null);

		// setup views
		mTvVolumeSummary = (TextView) mSettingLayout
				.findViewById(R.id.tvVolumeSummary);
		mTvVolumeSummary.setText(getString(R.string.volume_val_summary, mVolumeVal));
		
		mSBVolume = (SeekBar) mSettingLayout.findViewById(R.id.sbVolume);
		mSBVolume.setOnSeekBarChangeListener(this);
		mSBVolume.setProgress(mVolumeVal);

		showCandidateDialog();
	}

	/**
	 * Perform button clicks in dialog view
	 */
	public void onClick(View v) {
		if (v.getId() == R.id.btVolumeIncrease) {
			// increase time
			if (mVolumeVal < 100)
				mSBVolume.setProgress(mVolumeVal + 1);
		} else if (v.getId() == R.id.btVolumeDecrease) {
			// decrease time
			if (mVolumeVal > 0)
				mSBVolume.setProgress(mVolumeVal - 1);
		} else if (v.getId() == R.id.btVolumeMute) {
			mSBVolume.setProgress(0);
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
			putVolumeValPreference();

			mKeyboardSettingDialog.dismiss();
			finish();
			break;

		/**
		 * Cancel
		 */
		case DialogInterface.BUTTON_NEGATIVE:
			// Store vibrate time value to shared preferences
			mVolumeVal = mVolumeValPrev;
			putVolumeValPreference();

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

		builder.setTitle(R.string.settings_title_sound_key_click);
		builder.setView(mSettingLayout);

		mSettingLayout.findViewById(R.id.btVolumeDecrease).setOnClickListener(
				this);
		mSettingLayout.findViewById(R.id.btVolumeIncrease).setOnClickListener(
				this);
		mSettingLayout.findViewById(R.id.btVolumeMute).setOnClickListener(
				this);

		mKeyboardSettingDialog = builder.create();

		// set new vibrate time
		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getResources().getString(R.string.ok), this);

		// cancel
		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getResources().getString(R.string.cancel), this);

		// cancel
		mKeyboardSettingDialog
				.setOnKeyListener(new DialogInterface.OnKeyListener() {
					public boolean onKey(DialogInterface dialog, int keyCode,
							KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK
								&& event.getAction() == KeyEvent.ACTION_DOWN) {
							// Store vibrate time value to shared preferences
							mVolumeVal = mVolumeValPrev;
							putVolumeValPreference();

							finish();
							return true;
						}
						return false;
					}
				});

		// test
		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
				getResources().getString(R.string.test), this);

		// it helps to avoid from closing dialog after click on Test Vibration
		// button
		mKeyboardSettingDialog
				.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						mKeyboardSettingDialog.getButton(
								DialogInterface.BUTTON_NEUTRAL).setSoundEffectsEnabled(false);
						
						mKeyboardSettingDialog.getButton(
								DialogInterface.BUTTON_NEUTRAL)
								.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										/**
										 * Play sound
										 */
										mSoundPool.play(mSoundsMap.get(KeyboardService.SOUND_CLICK), (float) mVolumeVal/100, (float) mVolumeVal/100, 1, 0, 1); 
									}
								});
					}
				});

		mKeyboardSettingDialog.setCancelable(false);

		// Show keyboard setting dialog
		mKeyboardSettingDialog.show();
	}
}
