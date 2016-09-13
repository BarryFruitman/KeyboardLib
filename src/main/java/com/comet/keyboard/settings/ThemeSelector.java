package com.comet.keyboard.settings;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class ThemeSelector extends Activity implements OnClickListener {
	// Selecting Dialog
	private RadioGroup mRGTheme;
	private ScrollView mSVThemes;
	private LinearLayout mLLNoDefaultIME;
	
	// available theme names and values
	private String[] mThemeNames;
	private String[] mThemeValues;
	
	// selected theme name
	private String mCurrTheme;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.theme_selector);
		
		((TextView) findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));
		
		// Load theme
		mThemeNames = getResources().getStringArray(R.array.theme_names);
		mThemeValues = getResources().getStringArray(R.array.theme_ids);
		mCurrTheme = KeyboardThemeManager.getCurrentTheme().getThemeName();
		
		Assert.assertTrue(mThemeNames != null);
		Assert.assertTrue(mThemeValues != null);
		Assert.assertTrue(mCurrTheme != null);
		
		initUI();
	}

	private void initUI() {
		// Get component from dialog resource
		mRGTheme = (RadioGroup) findViewById(R.id.rgThemes);
		mSVThemes = (ScrollView) findViewById(R.id.svThemes);
		mLLNoDefaultIME = (LinearLayout)findViewById(R.id.llDefaultIME);
		
		// Load language options
		for (int i = 0 ; i < mThemeNames.length ; i++) {
			RadioButton newOption = new RadioButton(this);
			newOption.setId(i);
			newOption.setText(mThemeNames[i]);
			newOption.setOnClickListener(this);
			mRGTheme.addView(newOption);
			
			if (mCurrTheme.equals(mThemeNames[i])) {
				mRGTheme.check(i);
			}
		}
		
//		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		showSoftInput(mRGTheme, true);
		
		// We need to wait until keyboard showed
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				onWindowFocusChanged(true);
			}
			
		}, 500);
	}

	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		View currItem = mRGTheme.getChildAt(mRGTheme.getCheckedRadioButtonId());
		if (currItem != null) {
			currItem.requestFocus();
			int left = currItem.getLeft();
			int top = currItem.getTop();
			mSVThemes.scrollTo(left, top);
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
	
	public void onBackPressed () {
		finish();
	}
	
	@Override
	public void onDestroy() {
		showSoftInput(mRGTheme, false);
		
		super.onDestroy();
	}
	/**************************************************************************
	 * UI Event Handler
	 *************************************************************************/
	@Override
	public void onClick(View arg0) {
		int selectedID;

		selectedID = mRGTheme.getCheckedRadioButtonId();
		mCurrTheme = mThemeNames[selectedID];

		WallpaperPhoto.removeWallpaper(this);
		KeyboardThemeManager.setCurrKeyboardThemeByName(this, (String)mCurrTheme);
		if(KeyboardService.getIME() != null)
			KeyboardService.getIME().updateKeyboardView();
	}
	
	public void showSoftInput(View view, boolean isShow) {
		if (view == null)
			return;
		
		if (Utils.isSelectedToDefault(this)) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			if (isShow) {
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
			} else {
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
				imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
			mLLNoDefaultIME.setVisibility(View.GONE);
		} else {
			mLLNoDefaultIME.setVisibility(View.VISIBLE);
		}
	}
}
