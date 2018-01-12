/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.wizard;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


public class EnableIME extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			startBgThread();
	}



	private void showInfoDialog() {
		AlertDialog.Builder builder;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		else
			builder = new AlertDialog.Builder(this);
		String message = Utils.formatStringWithAppName(this, getString(R.string.install_wizard_enable_ime_dialog, getString(R.string.ime_name)));
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			// Tell pre-ICS users to press Back
			message += getString(R.string.install_wizard_enable_ime_dialog_press_back);
		builder.setMessage(message);
		builder.setTitle(getString(R.string.ime_short_name));
		AlertDialog dialog = builder.create();
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
						// Show a reminder
						Toast.makeText(KeyboardApp.getApp(), getString(R.string.install_wizard_enable_ime_toast_reminder), Toast.LENGTH_LONG).show();
					// Open settings activity and exit
					launchSetting();
//					finish();
				}
			}
		);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}



	public void launchSetting() {
		// Launch the keyboard settings app
		Intent intent = new Intent();
		intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
		startActivity(intent);
	}

	// Info message for dum-dums
	public void onClickImage(View view) {
		Toast.makeText( this, getString(R.string.install_info_message_for_dumb),
				Toast.LENGTH_LONG).show();
	}

	
	
	@Override
	protected void onResume() {
		super.onResume();

		if(!Utils.isEnabledIME(this))
			showInfoDialog();
		else
			defaultIME();
	}



	private void defaultIME() {
		Installer.logInstallStep("enabled");
		
		// Offer to make it the default IME
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), DefaultIME.class);
		startActivity(intent);

//		finish();
	}
	
	
	private static IMERunnable mRunnable = null;
	private Thread mBgThread;
	private void startBgThread() {
		if(mRunnable != null)
			mRunnable.req = IMERunnable.REQ_CMD_STOP;

		mRunnable = new IMERunnable();
		mBgThread = new Thread(mRunnable);
		mBgThread.setName("EnableIME");
		mBgThread.start();
		
		mRunnable.req = IMERunnable.REQ_CMD_START;
	}
	
	
	
	/**
	 * Wait while user accept the TK IME
	 */
	public class IMERunnable implements Runnable {
		private static final int REQ_CMD_NONE = 0;
		private static final int REQ_CMD_START = 1;
		private static final int REQ_CMD_STOP = 2;
		
		public int req = REQ_CMD_NONE;
		
		@Override
		public void run() {
			final long TIMEOUT = 300000;
			long now = System.currentTimeMillis();
			final long start = now;
			while(req != REQ_CMD_STOP && now - start < TIMEOUT) {
				try {
					if (req == REQ_CMD_START) {
						if (Utils.isEnabledIME(EnableIME.this)) {
							EnableIME.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									defaultIME();
								}
							});
							return;
						}
					}
					Thread.sleep(500);
					now = System.currentTimeMillis();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

}
