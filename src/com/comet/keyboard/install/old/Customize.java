/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.old;

import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class Customize extends Activity {
	private Button mBtnCustomizeSetAnyKeyAction;
	private Button mBtnCustomizeAppleWallpaper;
	private Button mBtnCustomizeExit;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.customize);

		// Assign button listeners
		mBtnCustomizeSetAnyKeyAction = (Button)findViewById(R.id.btnCustomizeSetAnyKeyAction);
		mBtnCustomizeSetAnyKeyAction.setOnClickListener(mSelectListener);
		
		mBtnCustomizeAppleWallpaper = (Button)findViewById(R.id.btnCustomizeApplyWallpaper);
		mBtnCustomizeAppleWallpaper.setOnClickListener(mSelectListener);
		
		mBtnCustomizeExit = (Button)findViewById(R.id.btnCustomizeExit);
		mBtnCustomizeExit.setOnClickListener(mSelectListener);
	}
	
	
	// Select IME button click listener
	private OnClickListener mSelectListener = new OnClickListener() {
		public void onClick(View v) {
			Intent newIntent = new Intent(Customize.this, Settings.class);
			
			if (v == mBtnCustomizeSetAnyKeyAction) {
				newIntent.putExtra(Settings.LAUNCH_ACTIVITY_KEY, "any_key_action");
			} else if (v == mBtnCustomizeAppleWallpaper) {
				newIntent.putExtra(Settings.LAUNCH_ACTIVITY_KEY, "customize");
			} else if (v == mBtnCustomizeExit) {
				finish();
				return;
			}
			
			startActivity(newIntent);
		}
	};
}
