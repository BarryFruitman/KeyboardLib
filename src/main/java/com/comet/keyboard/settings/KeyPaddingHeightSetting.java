/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.R;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class KeyPaddingHeightSetting extends Activity implements OnSeekBarChangeListener, OnClickListener {
//	private AlertDialog mKeyboardSettingDialog;
	private View mSettingLayout;
	
	// Defines preference key name
	public static final String PREFERENCE_KEY_PADDING = "key_padding";
	
	// Input method
	InputMethodManager mInputMgr;
	
	private LinearLayout mLLNoDefaultIME;
	
	// Height controller
	private SeekBar mSBKeyHeight;
	private int mKeyPaddingHeight = 0, mPrevKeyPaddingHeight;
	private int mKeyPaddingHeightMax, mKeyPaddingHeightMin;
	
	private boolean isSaved = false;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load current keyboard height
		mPrevKeyPaddingHeight = mKeyPaddingHeight = getKeyPaddingHeightPreference(this);
		
		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View)getLayoutInflater().inflate(R.layout.settings_key_padding_height, null);
		
		((TextView) mSettingLayout.findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));
		
		// mETDemo = (EditText)mSettingLayout.findViewById(R.id.etDemo);
		mSBKeyHeight = (SeekBar)mSettingLayout.findViewById(R.id.sbKeyPaddingHeight);
		mSBKeyHeight.setOnSeekBarChangeListener(this);
		
		mLLNoDefaultIME = (LinearLayout)mSettingLayout.findViewById(R.id.llDefaultIME);
		
		// Set range of keyboard height
		mKeyPaddingHeightMin = (int)getResources().getDimensionPixelSize(R.dimen.min_key_padding_height);
		mKeyPaddingHeightMax = (int)getResources().getDimensionPixelSize(R.dimen.max_key_padding_height);
		mSBKeyHeight.setMax(mKeyPaddingHeightMax - mKeyPaddingHeightMin);
		mSBKeyHeight.setProgress(mKeyPaddingHeight - mKeyPaddingHeightMin);
		
		mSettingLayout.findViewById(R.id.btKeyPaddingHeightIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btKeyPaddingHeightDecrease).setOnClickListener(this);
	
		mSettingLayout.findViewById(R.id.btKeyHeightOk).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						// Store height value to shared preferences
						putKeyHeightPreference();
						
						isSaved = true;
						
						// key_height
						finish();
					}
				});
		
		mSettingLayout.findViewById(R.id.btKeyHeightCancel).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						finish();
					}
				});

		
		// Show preview keyboard dialog
//		showKeyboardDialog();
		setContentView(mSettingLayout);
		
		if (Utils.isSelectedToDefault(this)) {
			// Show input dialog forcely
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			mInputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mInputMgr.showSoftInput(mSettingLayout, InputMethodManager.SHOW_FORCED);
			mLLNoDefaultIME.setVisibility(View.GONE);
		} else {
			mLLNoDefaultIME.setVisibility(View.VISIBLE);
		}
				
	}

	public void onDestroy() {
		super.onDestroy();
		
		if (!isSaved) {
			mKeyPaddingHeight = mPrevKeyPaddingHeight;
			putKeyHeightPreference();
		}
		
		// Refresh input view
		if (KeyboardService.IME != null) {
			KeyboardService.IME.updateKeyPaddingHeight();
		}	}
	
	
	
	// Load current keyboard padding
	public static int getKeyPaddingHeightPreference(Context context) {
		int keyHeight;
		
		// Get preferences
		SharedPreferences preference = context.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			keyHeight = preference.getInt(PREFERENCE_KEY_PADDING,
				(int) context.getResources().getDimensionPixelSize(R.dimen.key_padding_height));
		else
			keyHeight = (int) context.getResources().getDimension(R.dimen.key_padding_height_landscape); // landscape height
		
		return keyHeight;
	}

	// Save current keyboard height
	public void putKeyHeightPreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(PREFERENCE_KEY_PADDING, mKeyPaddingHeight);
		preferenceEditor.commit();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			mKeyPaddingHeight = progress + mKeyPaddingHeightMin;
			putKeyHeightPreference();
		}
		
		// Refresh input view
		if (KeyboardService.IME != null)
			KeyboardService.IME.updateKeyPaddingHeight();
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
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
	}

  @Override
  public void onClick(View v) {
    // plus and minus buttons handlers
    if (v.getId() == R.id.btKeyPaddingHeightIncrease
        && mSBKeyHeight.getProgress() < mSBKeyHeight.getMax()) {
      mSBKeyHeight.setProgress(mSBKeyHeight.getProgress() + 1);
    } else if (v.getId() == R.id.btKeyPaddingHeightDecrease && mSBKeyHeight.getProgress() > 0) {
      mSBKeyHeight.setProgress(mSBKeyHeight.getProgress() - 1);
    }
  }
}
