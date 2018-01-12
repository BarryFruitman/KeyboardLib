/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;


public class Welcome extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(Utils.formatStringWithAppName(this, R.string.welcome_title));
		
		setContentView(R.layout.welcome);
		
		// Flag this page as shown
		setWelcomeShowedVal(this, true);
	}



	public void onClickOk(View v) {
		finish();
	}


	/**
	 * Show welcome screen (once only)
	 * @param context
	 * @return
	 */
	public static boolean showWelcome(Context context) {
		if(!wasWelcomeShown(context)) {
			// Flag this page as shown
			setWelcomeShowedVal(context, true);

			Intent newIntent = new Intent(context, Welcome.class);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newIntent);

			// Credit free translation credits
			int credits = TranslatorView.getCredits();
			if(credits == -1) {
				// HACK: THIS SHOULD BE DONE IN Purchase.putTransPoint() but it crashes at this point because IME is not set
				TranslatorView.setCredits(context.getResources().getInteger(R.integer.free_translations_credits));
			}
			
			return true;
		}
		
		return false;
	}


	/**
	 * Check if welcome screen showed or not
	 */
	private static boolean mWelcomeShown = false;
	private static boolean wasWelcomeShown(Context context) {
		if(mWelcomeShown)
			return true;

		SharedPreferences prefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mWelcomeShown = prefs.getBoolean("welcome_shown", false);
		return mWelcomeShown;
	} 

	
	
	private static void setWelcomeShowedVal(Context context, boolean value) {
		mWelcomeShown = value;

		SharedPreferences preference = context.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preference.edit();
		editor.putBoolean("welcome_shown", mWelcomeShown);
		editor.commit();
	}
	
	public void onClickComet(View view) {
		KeyboardService.onClickComet(this);
	}
}
