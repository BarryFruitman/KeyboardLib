/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.KillActivityReceiver;
import com.comet.keyboard.util.Utils;

public class Home extends Activity /* implements Runnable */{
	// private Thread waitThread = null;
	private KillActivityReceiver mKillReceiver;

	public final static String IGNORE_VERSION = "ignore_version";

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// android.os.Debug.waitForDebugger();
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.home_title));

		mKillReceiver = new KillActivityReceiver(this);
		registerReceiver(mKillReceiver,
				IntentFilter.create("killMyActivity", "text/plain"));

		// Get parameter
		boolean isIgnoreVersionCheck = false;
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			isIgnoreVersionCheck = bundle.getBoolean(IGNORE_VERSION);
		}
		// Check version number
		SharedPreferences sharedPrefs = getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		String version = sharedPrefs.getString("version", "");
		if (!isIgnoreVersionCheck && version.equals("")) {
			// This is a new install or upgrade. Redirect to installer.
			Intent intent = new Intent();
			intent.setAction(getString(R.string.installer_action));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}

		Welcome.showWelcome(this);

		setContentView(R.layout.home);
	}

	
	
	/**********************************************************************
	 * Build UI
	 ***********************************************************************/
	private void onUpdateUI() {
		// Hide the instruction views, for now
		findViewById(R.id.homeInstall).setVisibility(View.GONE);
		findViewById(R.id.homeEnableTK).setVisibility(View.GONE);
		findViewById(R.id.homeSelectIME).setVisibility(View.GONE);
		findViewById(R.id.homeDone).setVisibility(View.GONE);

		((TextView) findViewById(R.id.homeLabelEnable)).setText(Utils.formatStringWithAppName(this, R.string.home_label_enable));
		((TextView) findViewById(R.id.homeLabelSelectIme)).setText(Utils.formatStringWithAppName(this, R.string.home_label_select_ime));
		((TextView) findViewById(R.id.homeLabelDone)).setText(Utils.formatStringWithAppName(this, R.string.home_label_done));

		// Assign a listener to the activate button
		((Button) findViewById(R.id.btn_enable)).setText(
				Utils.formatStringWithAppName(this, R.string.home_btn_activate));

/*
		if () {
			// Show the complete-setup button
			findViewById(R.id.homeInstall).setVisibility(View.VISIBLE);

			// Assign button listeners
			findViewById(R.id.btn_install).setOnClickListener(mInstallListener);
		} else
*/
		if(!Utils.isEnabledIME(this)) {
			// Show the enable section
			findViewById(R.id.homeEnableTK).setVisibility(View.VISIBLE);

			findViewById(R.id.btn_enable).setOnClickListener(mEnableListener);
		} else if (!Utils.isSelectedToDefault(this)) {
			// Show the set-default section
			findViewById(R.id.homeSelectIME).setVisibility(View.VISIBLE);

			// Assign button listeners
			findViewById(R.id.btn_set_default).setOnClickListener(mSelectListener);
		} else {
			// Keyboard is default IME. We're done!
			findViewById(R.id.homeDone).setVisibility(View.VISIBLE);
		}

		findViewById(R.id.btn_about).setOnClickListener(mAboutListener);
		findViewById(R.id.btn_settings).setOnClickListener(mSettingsListener);
	}



	/**********************************************************************
	 * Action & Event Listener
	 ***********************************************************************/
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus)
			onUpdateUI();
	}

	
	
	// Activate button click listener
	private OnClickListener mEnableListener = new OnClickListener() {
		public void onClick(View v) {
			// Launch the keyboard settings app
			Intent intent = new Intent();
			intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
			startActivity(intent);

			// waitForEnable();
		}
	};

	
	
	public void onDestroy() {
		super.onDestroy();

		// Unregister the kill receiver
		unregisterReceiver(mKillReceiver);
	}

	
	
	/**
	 * Select final install button
	 */
	private OnClickListener mInstallListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setAction(getString(R.string.installer_action));
			startActivity(intent);
		}
	};

	
	
	// Select IME button click listener
	private OnClickListener mSelectListener = new OnClickListener() {
		public void onClick(View v) {
			launchPicker();
		}
	};

	
	
	// This is a hack I found at
	// http://code.google.com/p/android/issues/detail?id=5005
	public void launchPicker() {
		Runnable run = new Runnable() {
			@Override
			public void run() {
				InputMethodManager inputManager;
				inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputManager.showInputMethodPicker();
			}
		};

		android.os.Handler h = new android.os.Handler();
		h.postDelayed(run, 250);
	}

	
	// Make default button click listener
	private OnClickListener mSettingsListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Launch TK settings
			launchSettings();
		}
	};

	
	private void launchSettings() {
		// Launch settings
		Intent intent = new Intent();
		intent.setAction(Home.this.getString(R.string.settings_intent));
		startActivity(intent);
	}

	
	private OnClickListener mAboutListener = new OnClickListener() {
		public void onClick(View v) {
			// Launch the about page
			// Intent intent = new Intent(Home.this,
			// com.comet.keyboard.install.wizard.Installer.class);
			Intent intent = new Intent(Home.this, About.class);
			startActivity(intent);
		}
	};

	
	
	public void onClickFacebook(View view) {
		launchBrowser(getResources().getString(
				R.string.settings_follow_facebook_url));
	}

	
	
	public void onClickTwitter(View view) {
		launchBrowser(getResources().getString(
				R.string.settings_follow_twitter_url));
	}

	
	
	public void onClickGooglePlus(View view) {
		launchBrowser(getResources().getString(
				R.string.settings_follow_google_plus_url));
	}

	
	
	public void launchBrowser(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
}
