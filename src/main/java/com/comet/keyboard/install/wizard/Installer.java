/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.wizard;

import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


public class Installer extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Save the version number to preferences
		putVersionPreference();

		// Log event
		logInstallStep("start");

		// Launch first step
		Intent intent = new Intent(this, LanguageSelector.class);
		startActivity(intent);
	}



	/*
	 * Save version number to preference file
	 */
	public void putVersionPreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		preferenceEditor.putString("version", getString(R.string.version));
		preferenceEditor.commit();
	}



	public static void logInstallStep(String step) {
	}

}
