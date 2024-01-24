/*
 * Comet Keyboard Library Copyright (C) 2011-2018 Comet Inc. All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.models.KeyHeight;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class KeyHeightSetting extends Activity implements OnSeekBarChangeListener,
OnCheckedChangeListener, OnClickListener {

	private View mSettingLayout;

	// Screen Mode
	protected static final String PARMS_ORIENTATION_MODE = "orientation_mode";
	protected boolean mIsPortraitMode = true;

	// Defines preference key name
	protected static final String PREFERENCE_LANDSCAPE_FULLSCREEN_MODE = "keyboard_fullscreen";
	protected static final String PREFERENCE_PORTRAIT_FULLSCREEN_MODE = "portrait_keyboard_fullscreen";
	// common row key in configs
	protected static final String PREFERENCE_LANDSCAPE_KEY_HEIGHT = "landscape_key_height";
	protected static final String PREFERENCE_PORTRAIT_KEY_HEIGHT = "portrait_key_height";

	// bottom row key in configs
	protected static final String PREFERENCE_LANDSCAPE_KEY_HEIGHT_ROW_BOTTOM = "landscape_key_row_bottom_factor";
	protected static final String PREFERENCE_PORTRAIT_KEY_HEIGHT_ROW_BOTTOM = "portrait_key_row_bottom_factor";

	// Input method

	private LinearLayout mLLNoDefaultIME;

	// Height controller
	private SeekBar mSBKeyHeight;
	private SeekBar mSBKeyHeightBottomRow;
	private CheckBox mCBFullScreen;

	private TextView mTVKeyboardBottomRow;

	private KeyHeight mKeyHeight;
	private KeyHeight mPrevKeyHeight;

	private int mKeyHeightMax, mKeyHeightMin;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int orientation = display.getOrientation();
		
		// Retrieve intent parameters
		Bundle extra = getIntent().getExtras();
		if (extra != null) {
			String mode = extra.getString(PARMS_ORIENTATION_MODE);
			if (mode.equals(PREFERENCE_PORTRAIT_KEY_HEIGHT)) {
				mIsPortraitMode = true;
				if (orientation != Surface.ROTATION_0 && orientation != Surface.ROTATION_180) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			} else if (mode.equals(PREFERENCE_LANDSCAPE_KEY_HEIGHT)) {
				mIsPortraitMode = false;
				if (orientation != Surface.ROTATION_90 && orientation != Surface.ROTATION_270) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
			}
		}
		
		if (mIsPortraitMode)
			setTitle(R.string.settings_title_portrait_key_height);
		else
			setTitle(R.string.settings_title_landscape_key_height);
		
		// Load current keyboard height
		mKeyHeight = getKeyHeightPreference(this, !mIsPortraitMode);

		try {
			mPrevKeyHeight = mKeyHeight.clone();
		} catch (Exception e) {
			Log.e(KeyboardApp.LOG_TAG, "error", e);
		}

		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View) getLayoutInflater().inflate(R.layout.settings_key_height, null);
		
		((TextView) mSettingLayout.findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));

		mTVKeyboardBottomRow = (TextView) mSettingLayout.findViewById(R.id.tvKeyboardBottomRow);

		mSBKeyHeight = (SeekBar) mSettingLayout.findViewById(R.id.sbKeyboardHeight);
		mSBKeyHeight.setOnSeekBarChangeListener(this);

		mSBKeyHeightBottomRow = (SeekBar) mSettingLayout.findViewById(R.id.sbKeyboardHeightBottomRow);
		mSBKeyHeightBottomRow.setOnSeekBarChangeListener(this);

		mCBFullScreen = (CheckBox) mSettingLayout.findViewById(R.id.cbFullScreen);
		mCBFullScreen.setChecked(isFullScreenMode(this, !mIsPortraitMode));
		mCBFullScreen.setOnCheckedChangeListener(this);

		mLLNoDefaultIME = (LinearLayout) mSettingLayout.findViewById(R.id.llDefaultIME);

		// add click listener
		mSettingLayout.findViewById(R.id.btKeyHeightIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btKeyHeightDecrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btKeyHeightBottomRowIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btKeyHeightBottomRowDecrease).setOnClickListener(this);

		// Set range of keyboard height
		if (mIsPortraitMode) {
			mKeyHeightMin = (int) getResources().getDimensionPixelSize(R.dimen.min_key_height);
			mKeyHeightMax = (int) getResources().getDimensionPixelSize(R.dimen.max_key_height);
		} else {
			mKeyHeightMin = (int) getResources().getDimensionPixelSize(R.dimen.min_key_height_landscape);
			mKeyHeightMax = (int) getResources().getDimensionPixelSize(R.dimen.max_key_height_landscape);
		}

		mSBKeyHeight.setMax(mKeyHeightMax - mKeyHeightMin);

		mSettingLayout.findViewById(R.id.btKeyHeightOk).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						// Store height value to shared preferences
						putKeyHeightPreference();
						finish();
					}
				});

		mSettingLayout.findViewById(R.id.btKeyHeightCancel).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						finish();
					}
				});

		
		setContentView(mSettingLayout);

		if (Utils.isSelectedToDefault(this)) {
			InputMethodManager mInputMgr= (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mInputMgr.showSoftInput(mSettingLayout, InputMethodManager.SHOW_FORCED);
			mLLNoDefaultIME.setVisibility(View.GONE);
		} else {
			mLLNoDefaultIME.setVisibility(View.VISIBLE);
		}

		if (mKeyHeight.getScaleBottom() <= 1)
			mSBKeyHeightBottomRow.setProgress((int) ((mKeyHeight.getScaleBottom() - 0.5) * 100));
		else
			mSBKeyHeightBottomRow.setProgress(50 + (int) ((mKeyHeight.getScaleBottom() - 1.0) * 50));

		onUpdateUI();
	}

	/**
	 * Counts relative factor for bottom row
	 * 
	 * @return
	 */
	public float countRelativeFactor() {
		float progress = mSBKeyHeightBottomRow.getProgress();
		return progress <= 50 ? 0.5f + progress / 100 : progress / 100 * 2;
	}

	@Override
	public void finish() {
		// Refresh input view
		if (KeyboardService.IME != null)
			KeyboardService.IME.updateKeyHeight();

		super.finish();
	}

	public void onUpdateUI() {
		// mSBKeyHeight.setEnabled(!mCBFullScreen.isChecked());
		mSBKeyHeight.setProgress(mKeyHeight.getRowDefault() - mKeyHeightMin);

		if (mKeyHeight.getScaleBottom() <= 1)
			mTVKeyboardBottomRow.setText(getString(R.string.key_height_bottom_row,
					String.valueOf(50 + (int) ((mKeyHeight.getScaleBottom() - 0.5) * 100)))
					+ "%");
		else
			mTVKeyboardBottomRow.setText(getString(R.string.key_height_bottom_row,
					String.valueOf(100 + (int) ((mKeyHeight.getScaleBottom() - 1.0) * 100)))
					+ "%");

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (R.id.sbKeyboardHeight == seekBar.getId()) {
			// main height
			if (fromUser) {
				mKeyHeight.setRowDefault(progress + mKeyHeightMin);
				updateRowBottom();
				putKeyHeightPreference();
			}
		} else if (R.id.sbKeyboardHeightBottomRow == seekBar.getId()) {
			if (fromUser) {
				// height of bottom row
				updateRowBottom();
				putKeyHeightPreference();
			}
		}

		onUpdateUI();

		// Refresh input view
		if (KeyboardService.IME != null)
			KeyboardService.IME.updateKeyHeight();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		onUpdateUI();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// User is exiting.
			// finish();
			return true;
		}

		return false;
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
	}

	// Load current keyboard height
	public static KeyHeight getKeyHeightPreference(Context context) {
		return getKeyHeightPreference(context, Utils.isLandscapeScreen(context));
	}

	private static KeyHeight getKeyHeightPreference(Context context, boolean isLandscape) {
		KeyHeight kh = new KeyHeight();

		// Get preferences
		SharedPreferences preference = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		if (isLandscape) {
			kh.setRowDefault(preference.getInt(PREFERENCE_LANDSCAPE_KEY_HEIGHT,
					(int) context.getResources().getDimensionPixelSize(R.dimen.key_height_landscape)));
			float factorBottom = preference.getFloat(PREFERENCE_LANDSCAPE_KEY_HEIGHT_ROW_BOTTOM, 1.0f);
			if (factorBottom > 2)
				factorBottom = 2;
			if (factorBottom < 0.5f)
				factorBottom = 0.5f;
			kh.setScaleBottom(factorBottom);
			// }
		} else {
			kh.setRowDefault(preference.getInt(PREFERENCE_PORTRAIT_KEY_HEIGHT,
					(int) context.getResources().getDimensionPixelSize(R.dimen.key_height)));
			float factorBottom = preference.getFloat(PREFERENCE_PORTRAIT_KEY_HEIGHT_ROW_BOTTOM, 1.0f);
			if (factorBottom > 2)
				factorBottom = 2;
			if (factorBottom < 0.5f)
				factorBottom = 0.5f;
			kh.setScaleBottom(factorBottom);
		}

		return kh;
	}

	// Save current keyboard height
	private void putKeyHeightPreference() {
		SharedPreferences.Editor preferenceEditor =
				getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		// Save key height
		if (!mIsPortraitMode) {
			preferenceEditor.putInt(PREFERENCE_LANDSCAPE_KEY_HEIGHT, mKeyHeight.getRowDefault());
			preferenceEditor.putFloat(PREFERENCE_LANDSCAPE_KEY_HEIGHT_ROW_BOTTOM, mKeyHeight.getScaleBottom());
			preferenceEditor.putBoolean(PREFERENCE_LANDSCAPE_FULLSCREEN_MODE, mCBFullScreen.isChecked());
		} else {
			preferenceEditor.putInt(PREFERENCE_PORTRAIT_KEY_HEIGHT, mKeyHeight.getRowDefault());
			preferenceEditor.putFloat(PREFERENCE_PORTRAIT_KEY_HEIGHT_ROW_BOTTOM, mKeyHeight.getScaleBottom());
			preferenceEditor.putBoolean(PREFERENCE_PORTRAIT_FULLSCREEN_MODE, mCBFullScreen.isChecked());
		}

		preferenceEditor.commit();
	}

	public static boolean isFullScreenMode(Context context) {
		return isFullScreenMode(context, Utils.isLandscapeScreen(context));
	}

	private static boolean isFullScreenMode(Context context, boolean landscapeMode) {
		SharedPreferences preference = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		return landscapeMode ? preference.getBoolean(PREFERENCE_LANDSCAPE_FULLSCREEN_MODE, false) : preference.getBoolean(PREFERENCE_PORTRAIT_FULLSCREEN_MODE, false);
	}

	public void updateRowBottom() {
		mKeyHeight.setScaleBottom(countRelativeFactor());
	}

	@Override
	public void onClick(View v) {

		/**
		 * main row seekbar
		 */
		if (v.getId() == R.id.btKeyHeightIncrease) {
			if (mKeyHeight.getRowDefault() < mKeyHeightMax) {
				mKeyHeight.setRowDefault(mKeyHeight.getRowDefault() + 1);
				updateRowBottom();
			}
		} else if (v.getId() == R.id.btKeyHeightDecrease) {
			if (mKeyHeight.getRowDefault() > mKeyHeightMin) {
				mKeyHeight.setRowDefault(mKeyHeight.getRowDefault() - 1);
				updateRowBottom();
			}
		}

		/**
		 * bottom row seekbar
		 */
		if (v.getId() == R.id.btKeyHeightBottomRowIncrease) {
			if (mSBKeyHeightBottomRow.getProgress() < 100) {
				mSBKeyHeightBottomRow.setProgress(mSBKeyHeightBottomRow.getProgress() + 1);
				updateRowBottom();
			}
		} else if (v.getId() == R.id.btKeyHeightBottomRowDecrease) {
			if (mSBKeyHeightBottomRow.getProgress() > 0) {
				mSBKeyHeightBottomRow.setProgress(mSBKeyHeightBottomRow.getProgress() - 1);
				updateRowBottom();
			}
		}

		if (!mKeyHeight.equals(mPrevKeyHeight)) {
			putKeyHeightPreference();

			onUpdateUI();

			// Refresh input view
			if (KeyboardService.IME != null)
				KeyboardService.IME.updateKeyHeight();
		}
	}
}