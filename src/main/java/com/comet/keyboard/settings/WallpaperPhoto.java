/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.comet.keyboard.R;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;

public class WallpaperPhoto extends Activity implements OnClickListener {
	private final static int DEFAULT_ALPHA = 128;
	
	private String			mImagePath;
	private BitmapDrawable	mPreviewWallpaper;
	private BitmapDrawable	mWallpaper;
	private int 			mAlpha = DEFAULT_ALPHA;
	private boolean			mWallpaperFit = false;
	private boolean			mIsBlackTheme = true;

	private static boolean mIsShowing = false;

	// UI Components
	private ImageView		mImageView;
	private SeekBar			mSeekAlpha;
	private LinearLayout	mLLNoDefaultIME;
	private ToggleButton	mTBWhiteTheme;
	private ToggleButton	mTBBlackTheme;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_wallpaper_photo);

		((TextView) findViewById(R.id.tvNoDefaultIME)).setText(Utils.formatStringWithAppName(this, R.string.default_ime_not_ready));

		// Get UI Components
		mLLNoDefaultIME = (LinearLayout)findViewById(R.id.llDefaultIME);
		mTBWhiteTheme = (ToggleButton)findViewById(R.id.tbWhiteTheme);
		mTBWhiteTheme.setOnClickListener(this);

		mTBBlackTheme = (ToggleButton)findViewById(R.id.tbBlackTheme);
		mTBBlackTheme.setOnClickListener(this);

		mImageView = (ImageView) findViewById(R.id.photo);
		mImagePath = getIntent().getStringExtra("image_path");
		
		if(mImagePath == null) {
			showErrorDialog(getString(R.string.settings_wallpaper_load_error));
			return;
		}

		// Load wallpaper
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int ht = displaymetrics.heightPixels;
		int wt = displaymetrics.widthPixels;
		mWallpaper = Utils.safeGetBitmapDrawable(this, mImagePath, wt, ht);
		if (mWallpaper == null) {
			Utils.showOutOfMemoryDlg(this);
		} else {
			if(!saveWallpaperToPNG(mWallpaper)) {
				showErrorDialog(getString(R.string.settings_wallpaper_save_error));
				return;
			}
		}

		// Show create static drawable to prevent resize from keyboard view.
		mPreviewWallpaper = Utils.safeGetBitmapDrawable(this, mImagePath, wt, ht);
		if (mPreviewWallpaper == null) {
			Utils.showOutOfMemoryDlg(this);
		}

		mImageView.setImageDrawable(mPreviewWallpaper);
		CheckBox cbFit = (CheckBox) findViewById(R.id.cbFit);
		cbFit.setChecked(mWallpaperFit);
		mSeekAlpha = (SeekBar) findViewById(R.id.seekAlpha);
		mSeekAlpha.setProgress(mAlpha);
		mSeekAlpha.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int alpha, boolean fromUser) {
				mAlpha = alpha;
				putWallpaperPreferences();
				onUpdateUI();
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {	/* Do nothing */ }
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {	/* Do nothing */ }
		});

		cbFit.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mWallpaperFit = isChecked;
				if(KeyboardService.getIME() != null)
					KeyboardService.getIME().applyWallpaper(mWallpaper, mAlpha, mWallpaperFit);
			}
		});

		if(Utils.isSelectedToDefault(this)) {
			// Apply wallpaper and show keyboard
			if(KeyboardService.getIME() != null)
				KeyboardService.getIME().applyWallpaper(mWallpaper, mAlpha, mWallpaperFit);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			mLLNoDefaultIME.setVisibility(View.GONE);
		} else {
			// TODO: Display a message and button to select default IME. Disable other controls.
			mLLNoDefaultIME.setVisibility(View.VISIBLE);
		}

		onUpdateUI();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// default theme
				onClick(mTBBlackTheme);

				onUpdateUI();
			}

		}, 500);
	}



	public void onUpdateUI() {
		if (mIsBlackTheme) {
			if (!mTBBlackTheme.isChecked())
				mTBBlackTheme.setChecked(true);
			if (mTBWhiteTheme.isChecked())
				mTBWhiteTheme.setChecked(false);
		} else {
			if (mTBBlackTheme.isChecked())
				mTBBlackTheme.setChecked(false);
			if (!mTBWhiteTheme.isChecked())
				mTBWhiteTheme.setChecked(true);
		}

		if(KeyboardService.getIME() != null)
			KeyboardService.getIME().applyWallpaper(mWallpaper, mAlpha, mWallpaperFit);
		
		if(KeyboardService.getIME() != null)
			KeyboardService.getIME().updateKeyboardView();
	}

	
	
	private AlertDialog mErrorDialog;
	private void showErrorDialog(String message) {
		mErrorDialog = new AlertDialog.Builder(this).create();
		mErrorDialog.setTitle(R.string.error);
		mErrorDialog.setMessage(message);
		// mAlertDialog.setIcon(R.drawable.icon);
		mErrorDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mErrorDialog.dismiss();
				WallpaperPhoto.this.finish();
			}
		}
				);
		mErrorDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					WallpaperPhoto.this.finish();
					return true;
				}
				return false;
			}
		});

		mErrorDialog.show();
	}



	// Load current wallpaper drawable id
	public String getWallpaperPreference(Context context) {
		SharedPreferences preference = context.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		String imagePath = preference.getString("wallpaper_path", "");

		return imagePath;
	}


	
	// Save wallpaper and stretch/fit flag
	public void putWallpaperPreferences() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt("wallpaper_alpha", mAlpha);
		preferenceEditor.putBoolean("wallpaper_fit", mWallpaperFit);
		preferenceEditor.commit();
	}



	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		mIsShowing = hasFocus;
	}



	public static BitmapDrawable loadWallpaper(Context context) {
		BitmapDrawable wallpaper = null;

		try {
			// Load wallpaper from file
			InputStream fileInputStream = context.openFileInput(context.getString(R.string.wallpaper_filename));
			wallpaper = new BitmapDrawable(context.getResources(), fileInputStream);
		} catch (FileNotFoundException e) {
			// Do nothing
		}

		return wallpaper;
	}



	protected boolean saveWallpaperToPNG(BitmapDrawable wallpaper) {
		try {
			Bitmap bitmap = wallpaper.getBitmap();
			FileOutputStream fileOutStream = openFileOutput(getString(R.string.wallpaper_filename), MODE_PRIVATE);
			bitmap.compress(Bitmap.CompressFormat.PNG, 0, fileOutStream);
			fileOutStream.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}



	public static void removeWallpaper(Activity activity) {
		// Delete wallpaper file
		activity.deleteFile(activity.getString(R.string.wallpaper_filename));
		// Reset wallpaper preferences
		SharedPreferences.Editor preferenceEditor = activity.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		preferenceEditor.putInt("key_background_alpha", 255);
		preferenceEditor.putBoolean("wallpaper_fit", true);
		preferenceEditor.commit();
		// Reset the view
		if(KeyboardService.getIME() != null)
			KeyboardService.getIME().reloadWallpaper();
	}



	public static boolean isShowing() {
		return mIsShowing;
	}



	@Override
	protected void onStop() {
		super.onStop();

		if (KeyboardService.getIME() != null) {
			// Always reload the wallpaper when closing
			KeyboardService.getIME().reloadWallpaper();
		}
	}


	
	@Override
	public void onClick(View v) {
		if (v == mTBWhiteTheme) {
			mIsBlackTheme = false;
			mTBWhiteTheme.setChecked(true);
			mTBBlackTheme.setChecked(false);

			KeyboardThemeManager.setCurrKeyboardThemeByValue(this, getResources().getString(R.string.theme_id_white));
		} else if (v == mTBBlackTheme) {
			mIsBlackTheme = true;
			mTBWhiteTheme.setChecked(false);
			mTBBlackTheme.setChecked(true);

			KeyboardThemeManager.setCurrKeyboardThemeByValue(this, getResources().getString(R.string.theme_id_black));
		}

		// Reset alpha
		mAlpha = DEFAULT_ALPHA;
		mSeekAlpha.setProgress(mAlpha);

		putWallpaperPreferences();
		onUpdateUI();
	}
}
