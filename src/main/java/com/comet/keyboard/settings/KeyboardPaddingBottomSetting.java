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
import android.content.Context;
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

public class KeyboardPaddingBottomSetting extends Activity implements OnSeekBarChangeListener, OnClickListener {
	
	private static final int 	PADDING_BUTTON_ABOVE = 0;
	private static final int 	PADDING_BUTTON_BELOW = 1;
	
	private View mSettingLayout;
	
	// Defines preference key name
	public static final String PREFERENCE_KEYBOARD_BOTTOM_PADDING_ABOVE = "keyboard_bottom_padding_above";
	public static final String PREFERENCE_KEYBOARD_BOTTOM_PADDING_BELOW = "keyboard_bottom_padding_below";
	
	// Input method
	InputMethodManager mInputMgr;
	
	// Preview keyboard
	// private LatinKeyboardView keyboardView;
	// private LatinKeyboard sampleKeyboard;
	
	// Demo text control to show the keyboard
	// private EditText mETDemo;
	
	private LinearLayout mLLNoDefaultIME;
	
	// Height controller
	private SeekBar mSBKeyHeightAbove, mSBKeyHeightBelow;
	private int [] mKeyboardPaddingBottom = new int [2];
	private int [] mPrevKeyboardPaddingBottom = new int [2];
	private int mKeyboardPaddingBottomMax, mKeyboardPaddingBottomMin;

	private boolean isSaved = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.settings_keyboard_bottom_padding_height_title);

		// Load current keyboard height
		int [] keyboardPaddingBottom = getKeyboardPaddingBottomPreference(this);
		for (int i = 0; i < 2; i ++) {
			mPrevKeyboardPaddingBottom[i] = mKeyboardPaddingBottom[i] = keyboardPaddingBottom[i];
		}
		// Load layouts
		// Inflate the overlapped keyboard view
		mSettingLayout = (View)getLayoutInflater().inflate(R.layout.settings_keyboard_padding_bottom, null);
		
		((TextView) mSettingLayout.findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));
		
		// mETDemo = (EditText)mSettingLayout.findViewById(R.id.etDemo);
		mSBKeyHeightAbove = (SeekBar)mSettingLayout.findViewById(R.id.sbKeyboardPaddingBottomAbove);
		mSBKeyHeightBelow = (SeekBar)mSettingLayout.findViewById(R.id.sbKeyboardPaddingBottomBelow);
		mSBKeyHeightAbove.setOnSeekBarChangeListener(this);
		mSBKeyHeightBelow.setOnSeekBarChangeListener(this);
		
		mSettingLayout.findViewById(R.id.btPaddingBottomAboveIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btPaddingBottomAboveDecrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btPaddingBottomBelowIncrease).setOnClickListener(this);
		mSettingLayout.findViewById(R.id.btPaddingBottomBelowDecrease).setOnClickListener(this);
		
		mLLNoDefaultIME = (LinearLayout)mSettingLayout.findViewById(R.id.llDefaultIME);
		
		// Set range of keyboard height
		mKeyboardPaddingBottomMin = (int)getResources().getDimensionPixelSize(R.dimen.min_keyboard_padding_bottom);
		mKeyboardPaddingBottomMax = (int)getResources().getDimensionPixelSize(R.dimen.max_keyboard_padding_bottom);
		
		mSBKeyHeightAbove.setMax(mKeyboardPaddingBottomMax - mKeyboardPaddingBottomMin);
		mSBKeyHeightBelow.setMax(mKeyboardPaddingBottomMax - mKeyboardPaddingBottomMin);		
		mSBKeyHeightAbove.setProgress(mKeyboardPaddingBottom[PADDING_BUTTON_ABOVE] - mKeyboardPaddingBottomMin);
		mSBKeyHeightBelow.setProgress(mKeyboardPaddingBottom[PADDING_BUTTON_BELOW] - mKeyboardPaddingBottomMin);
		
		mSettingLayout.findViewById(R.id.btKeyPaddingBottomHeightOk).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						// Store height value to shared preferences
						putKeyboardPaddingBottomPreference();
						
						isSaved = true;
						
						finish();
					}
				});

		mSettingLayout.findViewById(R.id.btKeyPaddingBottomHeightCancel).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						finish();
					}					
				});
		

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
			mKeyboardPaddingBottom = mPrevKeyboardPaddingBottom;
			putKeyboardPaddingBottomPreference();
		}
		
		// Refresh input view
		if (KeyboardService.IME != null)
			KeyboardService.IME.updateKeyboardBottomPaddingHeight();
	}
	
	
	
	
	
//	/**
//	 * Show preview keyboard dialog
//	 */
//	private void showKeyboardDialog() {
//		// Create new setting dialog
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		
//		builder.setTitle(R.string.settings_keyboard_bottom_padding_height_title);
//		builder.setView(mSettingLayout);
//
//		mKeyboardSettingDialog = builder.create();
//		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
//				getResources().getString(R.string.ok), 
//			new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int which) {
//					// Store height value to shared preferences
//					putKeyboardPaddingBottomPreference();
//					mKeyboardSettingDialog.dismiss();
//					
//					isSaved = true;
//					
//					// key_height
//					finish();
//				}
//			}
//		);
//		mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
//				getResources().getString(R.string.cancel), 
//			new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int which) {
//					mKeyboardSettingDialog.dismiss();
//					finish();
//				}
//			}
//		);
//		mKeyboardSettingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//			public boolean onKey(DialogInterface dialog, int keyCode,
//					KeyEvent event) {
//				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
//					mKeyboardSettingDialog.dismiss();
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
	
	// Load current keyboard padding bottom
	public static int [] getKeyboardPaddingBottomPreference(Context context) {
		int [] bottomGap = new int [2];
		
		// Get preferences
		SharedPreferences preference = context.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			bottomGap[0] = preference.getInt(PREFERENCE_KEYBOARD_BOTTOM_PADDING_ABOVE,
				(int) context.getResources().getDimensionPixelSize(R.dimen.keyboard_padding_bottom_above));
			bottomGap[1] = preference.getInt(PREFERENCE_KEYBOARD_BOTTOM_PADDING_BELOW,
				(int) context.getResources().getDimensionPixelSize(R.dimen.keyboard_padding_bottom_below));
		} else {
			bottomGap[0] = (int) context.getResources().getDimension(R.dimen.keyboard_padding_bottom_landscape_above); // landscape height
			bottomGap[1] = (int) context.getResources().getDimension(R.dimen.keyboard_padding_bottom_landscape_below); // landscape height
		}
		return bottomGap;
	}
	
//	// Load current keyboard padding bottom
//	public static int getKeyboardPaddingBottomPreference(Context context, int aboveBelow) {
//		int gap;
//		
//		// Get preferences
//		SharedPreferences preference = context.getSharedPreferences(
//				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
//
//		String key = (aboveBelow == PADDING_BUTTON_ABOVE) ? 
//				PREFERENCE_KEYBOARD_BOTTOM_PADDING_ABOVE : PREFERENCE_KEYBOARD_BOTTOM_PADDING_BELOW;
//
//		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
//			gap = preference.getInt(key, (int) context.getResources().getDimensionPixelSize(R.dimen.keyboard_padding_bottom));
//		else
//			gap = (int) context.getResources().getDimension(R.dimen.keyboard_padding_bottom_landscape); // landscape height
//		
//		return gap;
//	}

	// Save current keyboard height
	public void putKeyboardPaddingBottomPreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(PREFERENCE_KEYBOARD_BOTTOM_PADDING_ABOVE, mKeyboardPaddingBottom[PADDING_BUTTON_ABOVE]);
		preferenceEditor.putInt(PREFERENCE_KEYBOARD_BOTTOM_PADDING_BELOW, mKeyboardPaddingBottom[PADDING_BUTTON_BELOW]);
		preferenceEditor.commit();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		
		// Set the bar height as indicated.
		if (fromUser) {
			int index = (seekBar == mSBKeyHeightAbove) ?
					PADDING_BUTTON_ABOVE : PADDING_BUTTON_BELOW;
			saveChanges(index, progress);
		}
		
		// Refresh input view
		if (KeyboardService.IME != null)
			KeyboardService.IME.updateKeyboardBottomPaddingHeight();
	}
	
	protected void saveChanges(int index, int progress){
    mKeyboardPaddingBottom[index] = progress + mKeyboardPaddingBottomMin;
    putKeyboardPaddingBottomPreference();	  
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
    if (v.getId() == R.id.btPaddingBottomAboveIncrease
        && mSBKeyHeightAbove.getProgress() < mSBKeyHeightAbove.getMax()) {
      int progress = mSBKeyHeightAbove.getProgress() + 1;
      mSBKeyHeightAbove.setProgress(progress);
      saveChanges(PADDING_BUTTON_ABOVE,  progress);      
    } else if (v.getId() == R.id.btPaddingBottomAboveDecrease && mSBKeyHeightAbove.getProgress() > 0) {
      int progress = mSBKeyHeightAbove.getProgress() - 1;
      mSBKeyHeightAbove.setProgress(progress);
      saveChanges(PADDING_BUTTON_ABOVE,  progress);      
    }
    
    if (v.getId() == R.id.btPaddingBottomBelowIncrease
        && mSBKeyHeightBelow.getProgress() < mSBKeyHeightBelow.getMax()) {
      int progress = mSBKeyHeightBelow.getProgress() + 1;
      mSBKeyHeightBelow.setProgress(progress);
      saveChanges(PADDING_BUTTON_BELOW,  progress);
    } else if (v.getId() == R.id.btPaddingBottomBelowDecrease && mSBKeyHeightBelow.getProgress() > 0) {
      int progress = mSBKeyHeightBelow.getProgress() - 1;
      mSBKeyHeightBelow.setProgress(progress);
      saveChanges(PADDING_BUTTON_BELOW,  progress);      
    }    
  }
}
