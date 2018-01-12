/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install;

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
	private AlertDialog mInfoDialog;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Utils.formatStringWithAppName(this, R.string.install_enable_ime_dialog_message));
		builder.setTitle(Utils.formatStringWithAppName(this, R.string.enable_ime_title));

		mInfoDialog = builder.create();
		mInfoDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					launchSetting();
					finish();
				}
			}
		);
		
		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mInfoDialog.show();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			startBgThread();
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



	private void startBgThread() {
		mIMEThread = new Thread(mHandler);
		mIMEThread.start();
		
		mHandler.req = IMERunnable.REQ_CMD_START;
	}
	
	
	
//	private void selectIME() {
//		// Offer to make it the default IME
//		Intent intent = new Intent();
//		intent.setClass(getApplicationContext(), DefaultIME.class);
//		startActivity(intent);
//		
//		finish();
//	}
	
	
	/**
	 * Wait while user accept the TK IME
	 */
	private IMERunnable mHandler = new IMERunnable();
	private Thread mIMEThread;
	public class IMERunnable implements Runnable {
		private static final int REQ_CMD_NONE = 0;
		private static final int REQ_CMD_START = 1;
		private static final int REQ_CMD_STOP = 2;
		
		public int req = REQ_CMD_NONE;
		
		@Override
		public void run() {
			while(req != REQ_CMD_STOP) {
				try {
					if (req == REQ_CMD_START) {
						if (Utils.isEnabledIME(EnableIME.this)) {
							EnableIME.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// Show a reminder
									Toast.makeText(KeyboardApp.getApp(), getString(R.string.install_wizard_enable_ime_toast_reminder2), Toast.LENGTH_SHORT).show();
								}
							});
							return;
						}
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

}
