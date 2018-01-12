/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.old;

import com.comet.keyboard.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


public class EnableIME extends Activity {
	private final static int REQ_ENABLE_IME = 1000;
	
	private AlertDialog mNoticeDialog;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.install_enable_ime_dialog_message);
		builder.setTitle(R.string.enable_ime_title);

		mNoticeDialog = builder.create();
		mNoticeDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.install_select_language_notice_ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					launchSetting();
				}
			}
		);
		mNoticeDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					finish();
					return true;
				}
				return false;
			}
		});
		
		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mNoticeDialog.show();
	}
	
	public void launchSetting() {
		// Launch the keyboard settings app
		Intent intent = new Intent();
		intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
		startActivityForResult(intent, REQ_ENABLE_IME);
	}

	// Info message for dum-dums
	public void onClickImage(View view) {
		Toast.makeText( this, getString(R.string.install_info_message_for_dumb),
				Toast.LENGTH_LONG).show();
	}
	
	/**
     * Handle the results from the Setting activity.
     */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mNoticeDialog.dismiss();
		finish();
	}
}
