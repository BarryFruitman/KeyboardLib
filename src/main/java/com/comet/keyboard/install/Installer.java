/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.Home;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class Installer extends Activity implements OnClickListener {
	// Defines step of installing
	public enum InstallStep {
		INSTALL_STEP_1,
		INSTALL_STEP_2,
		INSTALL_STEP_3,
		INSTALL_FINISHED,
	}
	
	public enum Error {
		ERROR_NONE,
		ERROR_NOT_ENABLED,
		ERROR_INTERNAL,
	}
	
	// Command ID
	private final static int REQ_ENABLE_KEYBOARD_ID = 10000;
	private final static int REQ_SET_DEFAULT_KEYBOARD_ID = 10001;
	private final static int REQ_SELECT_LANGUAGE_ID = 10002;
	
	// Current step
	protected InstallStep mCurrStep = InstallStep.INSTALL_STEP_1;
	protected boolean mIsPendingCmd = false;
	// private final static int WAITING_SLEEP_MTIME = 100;
	
	/// STEP1
	private AlertDialog mEnableIMEConfirmDialog;
	
	// Preference
	private final static String INSTALL_STEP_OPTION = "install_current_step"; 
	
	// UI Components
	Button mBtnEnable;
	Button mBtnDefault;
	Button mBtnLanguage;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(Utils.formatStringWithAppName(this, R.string.home_title));

		setContentView(R.layout.installer3);
		((TextView) findViewById(R.id.installer_title)).setText(Utils.formatStringWithAppName(this, R.string.installer_title));

		logInstallStep("start");
		
		// Initialize module
		init();

		// Save the version number to preferences
		putVersionPreference(this);

		moveStep(mCurrStep);

		// Update screen to first
		onUpdateUI();
	}
	
	
	/**
	 * Initialize all variable and object data
	 */
	protected void init() {
		initData();
		initUIComponents();
		
		// load current step from preference storage		
		mCurrStep = getCurrStep(this);
	}
	
	
	/**
	 * Initialize all object data
	 */
	private void initData() {
	}
	
	
	/**
	 * Initialize all UI components
	 */
	private void initUIComponents() {
		// Retrieve reference of components
		mBtnEnable = (Button)findViewById(R.id.btn_installer_enable);
		mBtnDefault = (Button)findViewById(R.id.btn_installer_default);
		mBtnLanguage = (Button)findViewById(R.id.btn_installer_language);
		
		Assert.assertTrue(mBtnEnable != null);
		Assert.assertTrue(mBtnDefault != null);
		Assert.assertTrue(mBtnLanguage != null);
		
		mBtnEnable.setText(Utils.formatStringWithAppName(this, R.string.installer_label_enable));
		
		// Register events from the components
		mBtnEnable.setOnClickListener(this);
		mBtnDefault.setOnClickListener(this);
		mBtnLanguage.setOnClickListener(this);
	}
	
	/**
	 * Get current install step
	 * @param context
	 * @return
	 */
	public static InstallStep getCurrStep(Context context) {
		InstallStep currStep;
		
		// load current step from preference storage
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, 
				Context.MODE_PRIVATE);
		
		currStep = InstallStep.valueOf(sharedPrefs.getString(INSTALL_STEP_OPTION, "INSTALL_STEP_1"));
		return currStep;
	}
	
	/**
	 * Set current install step
	 * @param context
	 * @return
	 */
	public static boolean setCurrStep(Context context, InstallStep currStep) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(INSTALL_STEP_OPTION, currStep.toString());
		editor.commit();
		
		return true;
	}
	
	/*********************************************************
	 * UI functions
	**********************************************************/
	/**
	 * Update page from the current step
	 */
	private void onUpdateUI() {
		// Update button status depends on the current step.
		/// All disable buttons
		mBtnEnable.setEnabled(false);
		mBtnDefault.setEnabled(false);
		mBtnLanguage.setEnabled(false);

		Intent intent;
		if (!mIsPendingCmd || mCurrStep == InstallStep.INSTALL_STEP_2) {
			switch (mCurrStep) {
			case INSTALL_STEP_1:
				mBtnEnable.setEnabled(true);
				logInstallStep("step_enable");
				break;
			case INSTALL_STEP_2:
				mBtnDefault.setEnabled(true);
				logInstallStep("step_default");
				break;
			case INSTALL_STEP_3:
				mBtnLanguage.setEnabled(true);
				logInstallStep("step_language");
				break;
			case INSTALL_FINISHED:
				finish();

				intent = new Intent(this, Home.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra(Home.IGNORE_VERSION, true);
				startActivity(intent);

				logInstallStep("finished");
				break;
			default:
				Assert.assertTrue("Not acceptable status:" + mCurrStep, false);
			};
		}
	}

	/*********************************************************
	 * Database functions
	**********************************************************/
	/**
	 * Load settings from the database
	 */
	protected boolean loadSettings() {
		mCurrStep = getCurrStep(this);
		
		return true;
	}
	
	/**
	 * Update current settings to the database
	 */
	protected boolean updateSettings() {
		updateStepOnDatabase();
		
		return true;
	}
	
	/**
	 * Update current step to the database
	 */
	private boolean updateStepOnDatabase() {
		return setCurrStep(this, mCurrStep);
	}
	
	/*********************************************************
	 * Step functions
	**********************************************************/
	/**
	 * Move to another step
	 * @param step - destination step
	 */
	protected void moveStep(InstallStep step) {
		moveStep(step, false);
	}
	
	protected void moveStep(InstallStep step, boolean force) {
		InstallStep prevStep = InstallStep.INSTALL_STEP_1;
		
		if (force)
			prevStep = mCurrStep;
		mCurrStep = step;

		// Skip step x if it's already completed
		switch (step) {
		case INSTALL_STEP_1:
			if (Utils.isEnabledIME(this))
				moveStep(InstallStep.INSTALL_STEP_2);
			break;
		case INSTALL_STEP_2:
			if (Utils.isSelectedToDefault(this))
				moveStep(InstallStep.INSTALL_STEP_3);
			break;
		case INSTALL_STEP_3:
			break;
		case INSTALL_FINISHED:
			break;
		}
		
		// Update UI
		onUpdateUI();
		
		// Restore previous status
		if (force)
			mCurrStep = prevStep;
		
		// Update database for the step
		updateStepOnDatabase();
		
	}
	
	/**
     * Handle the results from the recognition activity.
     */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		InstallStep nextStep = mCurrStep;
		
		if (!mIsPendingCmd) {
			// Changed orientation of activity
			return;
		}

		switch (requestCode) {
		case REQ_ENABLE_KEYBOARD_ID:
			nextStep = InstallStep.INSTALL_STEP_2;
			break;
		case REQ_SET_DEFAULT_KEYBOARD_ID:
			mIMEThread = new Thread(mHandler);
			mIMEThread.start();
			
			mHandler.req = IMERunnable.REQ_CMD_START;
			
			break;
		case REQ_SELECT_LANGUAGE_ID:
			nextStep = InstallStep.INSTALL_FINISHED;
			break;
		default:
			break;
		}
		
		if (requestCode != REQ_SET_DEFAULT_KEYBOARD_ID)
			mIsPendingCmd = false;
		moveStep(nextStep);
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		if (!hasFocus) {
			if (!mIsPendingCmd) {
				finish();
			}
		} else {
			onUpdateUI();
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
	        if (keyCode == KeyEvent.KEYCODE_HOME) {
	           finish();
	           return true;
	        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
	        	finish();
	        	
		        Intent intent = new Intent(this, Home.class);
		        intent.putExtra(Home.IGNORE_VERSION, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
	        }
		}
		
        return false;
    }

	
	/*********************************************************
	 * 1st Step: Enable keyboard
	**********************************************************/
	/**
	 * Enable keyboard option
	 */
	private Error enableKeyboard() {
		Assert.assertTrue(!mIsPendingCmd);
		
		// Launch the keyboard settings app
		Intent intent = new Intent();
		intent.setClass(this, EnableIME.class);
		
		mIsPendingCmd = true;
		
		/// Start delivery activity
		intent = new Intent();
		intent.setClass(this, EnableIME.class);
		startActivityForResult(intent, REQ_ENABLE_KEYBOARD_ID);
		
		return Error.ERROR_NONE;
	}
    
	/**
	 * Alert confirm message for the keyboard enabling
	 */
	public void confirmEnableKeyboard() {
		mEnableIMEConfirmDialog = new AlertDialog.Builder(this).create();
		mEnableIMEConfirmDialog.setTitle(Utils.formatStringWithAppName(this, R.string.confirm_enable_ime_title));
		mEnableIMEConfirmDialog.setMessage(Utils.formatStringWithAppName(this, R.string.confirm_enable_ime_description));
		// mAlertDialog.setIcon(R.drawable.icon);
		mEnableIMEConfirmDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
			getResources().getString(R.string.install_downloading_retry_yes), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mEnableIMEConfirmDialog.dismiss();
				}
			}
		);
		mEnableIMEConfirmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
				getResources().getString(
			R.string.install_downloading_retry_no), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mEnableIMEConfirmDialog.dismiss();
					Installer.this.moveStep(InstallStep.INSTALL_STEP_3);
				}
			}
		);
		mEnableIMEConfirmDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				// ???
				return true; 
			}
		});
		
		mEnableIMEConfirmDialog.show();
	}
	/*********************************************************
	 * 2nd Step: Set default keyboard
	**********************************************************/
	/**
	 * Set settings for the default keyboard
	 */
	private Error setDefaultKeyboard() {
		// Assert.assertTrue(!mIsPendingCmd);
		
		mIsPendingCmd = true;
		
		// Offer to make it the default IME
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), DefaultIME.class);
		
		/// Start delivery activity
		startActivityForResult(intent, REQ_SET_DEFAULT_KEYBOARD_ID);
		
		/* InputMethodManager inputManager;
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);            
		inputManager.showInputMethodPicker(); */
		
		return Error.ERROR_NONE;
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
			while(req != REQ_CMD_STOP && (mCurrStep == InstallStep.INSTALL_STEP_2)) {
				try {
					if (req == REQ_CMD_START) {
						if (Utils.isSelectedToDefault(Installer.this)) {
							Installer.this.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										mIsPendingCmd = false;
										moveStep(InstallStep.INSTALL_STEP_3);
									}
							});
							return;
						}
					}
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mIsPendingCmd = false;
//			Log.d(KeyboardApp.LOG_TAG, "destroy waiting thread");
		}
	};
	
	/*********************************************************
	 * 3rd Step: Choose language
	**********************************************************/
	/**
	 * Choose language of keyboard
	 */
	private Error setLanguage() {
		Assert.assertTrue(!mIsPendingCmd);
		
		mIsPendingCmd = true;
		
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), LanguageSelector.class);
		
//		LanguageSelector.setOnResultListener(new OnResultListener() {
//			public void onFail() {
//				// ???
//			}
//
//			public void onSuccess() {
//				KeyboardApp.getKeyboardApp().checkNeedUpdate(Installer.this);
//				
//				if(KeyboardService.getIME() != null/* && KeyboardService.getIME().isInputViewCreated()*/){
//					KeyboardService.getIME().getSuggestor().reloadLanguageDictionary();
//				}				
//			}
//		});
		
		startActivityForResult(intent, REQ_SELECT_LANGUAGE_ID);
		
		return Error.ERROR_NONE;
	}
	
	
	
	
	/*********************************************************
	 * Event Handler
	**********************************************************/
	public void onClick(View arg0) {
		if (mIsPendingCmd && arg0 != mBtnDefault)
			return;
		
		if (arg0 == mBtnEnable) {
			onClickEnableKeyboard();
		} else if (arg0 == mBtnDefault) {
			onClickDefaultKeyboard();
		} else if (arg0 == mBtnLanguage) {
			onClickLanguage();
		}
		
		onUpdateUI();
	}
	
	/**
	 * Clicked "Enable Keyboard" option
	 */
	private void onClickEnableKeyboard() {
		Error error;
		
		error = enableKeyboard();
		switch (error) {
		default:
			// Alert error message
			break;
		}
		
		logInstallStep("press_enable");
	}
	
	/**
	 * Clicked "Select Default" option
	 */
	private void onClickDefaultKeyboard() {
		Error error;
		
		error = setDefaultKeyboard();
		switch (error) {
		default:
			// Alert error message
			break;
		}
		
		logInstallStep("press_default");
	}
	
	/**
	 * Clicked "Enable Language" option
	 */
	private void onClickLanguage() {
		Error error;
		error = setLanguage();
		switch (error) {
		default:
			// Alert error message
			break;
		}
		
		logInstallStep("press_language");
	}
	
	/**
	 * Save version number to preference file
	 */
	public static void putVersionPreference(Context context) {
		SharedPreferences.Editor preferenceEditor = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		preferenceEditor.putString("version", context.getString(R.string.version));
		preferenceEditor.commit();
	}

	public void onClickComet(View view) {
		KeyboardService.onClickComet(this);
	}


	private void logInstallStep(String step) {
//		MyFlurryAgent.logEvent("setup3a_" + step);
	}
}
