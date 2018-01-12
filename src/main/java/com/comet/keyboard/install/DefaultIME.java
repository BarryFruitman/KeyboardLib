/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install;

import com.comet.keyboard.R;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;


public class DefaultIME extends Activity {

	AlertDialog mDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		setContentView(R.layout.select_ime);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Utils.formatStringWithAppName(this, R.string.install_default_ime_dialog_message));
		builder.setTitle(R.string.install_default_ime_dialog_title);

		AlertDialog noticeDialog = builder.create();
		noticeDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					launchPicker();
					finish();
				}
			}
		);

		noticeDialog.show();
	}

	
	
	// This is a hack I found at http://code.google.com/p/android/issues/detail?id=5005
	public void launchPicker() {
		final Runnable run = new Runnable() {
			@Override
			public void run() {
				InputMethodManager inputManager;
				inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);            
				inputManager.showInputMethodPicker();                           
			}
		};

		android.os.Handler h = new android.os.Handler();
		h.postDelayed(run, 1000);
	}



	private void startBgThread() {
		mIMEThread = new Thread(mHandler);
		mIMEThread.start();
		
		mHandler.req = IMERunnable.REQ_CMD_START;
	}
	
	
	
	private void selectLanguage() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), LanguageSelector.class);
//		LanguageSelector.setOnResultListener(new OnResultListener() {
//			public void onFail() {
//				// ???
//			}
//
//			public void onSuccess() {
//				KeyboardApp.getKeyboardApp().checkNeedUpdate(DefaultIME.this);
//				
//				if(KeyboardService.getIME() != null/* && KeyboardService.getIME().isInputViewCreated()*/){
//					KeyboardService.getIME().getSuggestor().reloadLanguageDictionary();
//				}				
//			}
//		});
		
		startActivity(intent);

		finish();
	}

	
	
	
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
						if (Utils.isSelectedToDefault(DefaultIME.this)) {
							DefaultIME.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// Launch select IME dialog
									selectLanguage();
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
