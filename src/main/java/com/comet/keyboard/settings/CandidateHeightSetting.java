/*
 * Comet Keyboard Library Copyright (C) 2011-2018 Comet Inc. All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.R;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class CandidateHeightSetting extends Activity implements OnSeekBarChangeListener,
OnClickListener {
	private View mSettingLayout;

	// Defines preference key name
	public static final String PREFERENCE_CANDIDATE_FONT_SIZE = "candiates_font_size";

	// Input method
	InputMethodManager mInputMgr;

	// Preview keyboard
	// private LatinKeyboardView keyboardView;
	// private LatinKeyboard sampleKeyboard;

	// Demo text control to show the keyboard
	// private EditText mETDemo;

	private LinearLayout mLLNoDefaultIME;

	// Height controller
	private SeekBar mSBFontSize;
	private int mFontSize = 0, mPrevFontSize;
	private int mFontSizeMax, mFontSizeMin;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load current keyboard height
		mPrevFontSize = mFontSize = getFontSizePreference(this);

		setTitle(R.string.settings_candidate_height_title);

		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View) getLayoutInflater().inflate(R.layout.settings_suggestion_height, null);
		
		((TextView) mSettingLayout.findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));
		
		// mETDemo = (EditText)mSettingLayout.findViewById(R.id.etDemo);
		mSBFontSize = (SeekBar) mSettingLayout.findViewById(R.id.sbKeyboardHeight);
		mSBFontSize.setOnSeekBarChangeListener(this);

		mLLNoDefaultIME = (LinearLayout) mSettingLayout.findViewById(R.id.llDefaultIME);

		// Set range of keyboard height
		mFontSizeMin =
				(int) getResources().getDimensionPixelSize(R.dimen.candidate_small_font_height);
		mFontSizeMax =
				(int) getResources().getDimensionPixelSize(R.dimen.candidate_large_font_height);
		mSBFontSize.setMax(mFontSizeMax - mFontSizeMin);
		mSBFontSize.setProgress(mFontSize - mFontSizeMin);

		// add click listeners
		mSettingLayout.findViewById(R.id.btHeightIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btHeightDecrease).setOnClickListener(this);
		
		mSettingLayout.findViewById(R.id.btSuggestionsHeightOk).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						// Store height value to shared preferences
						putFontSizePreference();
						finish();
					}
				});

		mSettingLayout.findViewById(R.id.btSuggestionsHeightCancel).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						// Restore height value to shared preferences
						mFontSize = mPrevFontSize;
						putFontSizePreference();
						finish();
					}					
				});


		setContentView(mSettingLayout);

		if (Utils.isSelectedToDefault(this)) {
			// Force keyboard to appear
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			mInputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mInputMgr.showSoftInput(mSettingLayout, InputMethodManager.SHOW_FORCED);
			mLLNoDefaultIME.setVisibility(View.GONE);
		} else {
			mLLNoDefaultIME.setVisibility(View.VISIBLE);
		}

		// Show preview keyboard dialog
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Refresh input view
				if (KeyboardService.IME != null) {
					KeyboardService.IME.setCandidatesViewShown(true);
					KeyboardService.IME.showSampleSuggestion();
					KeyboardService.IME.setPreviewMode(true);
				}
			}
		}, 1000);
	}

//	/**
//	 * Show preview keyboard+candidate dialog
//	 */
//	private void showFontSizeDialog() {
//		// Create new setting dialog
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//		builder.setTitle(R.string.settings_candidate_height_title);
//		builder.setView(mSettingLayout);
//
//		mKeyboardSettingDialog = builder.create();
//		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_POSITIVE,
//				getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//				// Store height value to shared preferences
//				putFontSizePreference();
//				mKeyboardSettingDialog.dismiss();
//				finish();
//			}
//		});
//		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
//				getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//				// Restore height value to shared preferences
//				mFontSize = mPrevFontSize;
//				putFontSizePreference();
//				mKeyboardSettingDialog.dismiss();
//				finish();
//			}
//		});
//		mKeyboardSettingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
//
//					// Store height value to shared preferences
//					mFontSize = mPrevFontSize;
//					putFontSizePreference();
//					finish();
//					return true;
//				}
//				return false;
//			}
//		});
//
//		// Show keyboard setting dialog
//		mKeyboardSettingDialog.show();
//	}

	// Load current font size of candidate
	public static int getFontSizePreference(Context context) {
		// Get preferences
		SharedPreferences preference = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		int fontSize = preference.getInt(PREFERENCE_CANDIDATE_FONT_SIZE, (int) context.getResources()
						.getDimensionPixelSize(R.dimen.candidate_font_height));

		return fontSize;
	}

	// Save current keyboard height
	public void putFontSizePreference() {
		SharedPreferences.Editor preferenceEditor =
				getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(PREFERENCE_CANDIDATE_FONT_SIZE, mFontSize);
		preferenceEditor.commit();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			mFontSize = progress + mFontSizeMin;
			putFontSizePreference();
		}

		// Refresh input view
		if (KeyboardService.IME != null) {
			KeyboardService.IME.setCandidatesViewShown(true);
			KeyboardService.IME.showSampleSuggestion();
		}
	}

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
	protected void onStop() {
		super.onStop();

		if (KeyboardService.IME != null)
			KeyboardService.IME.clearMessage();
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// Do nothing
	}

	@Override
	public void onClick(View v) {
		// plus and minus buttons handlers
		if (v.getId() == R.id.btHeightIncrease
				&& mSBFontSize.getProgress() < mSBFontSize.getMax()) {
			mSBFontSize.setProgress(mSBFontSize.getProgress() + 1);
			mFontSize += 1;
		} else if (v.getId() == R.id.btHeightDecrease && mSBFontSize.getProgress() > 0) {
			mSBFontSize.setProgress(mSBFontSize.getProgress() - 1);
			mFontSize -= 1;
		}

		putFontSizePreference();

		// Refresh input view
		if (KeyboardService.IME != null) {
			KeyboardService.IME.setCandidatesViewShown(true);
			KeyboardService.IME.showSampleSuggestion();
		}
	}
}
