package com.comet.keyboard.install;

import com.comet.keyboard.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class LanguageSelector extends com.comet.keyboard.settings.LanguageSelector {
	private AlertDialog mTryLaterDialog;

	@Override
	protected void done() {
		saveCurrentLanguage();
		saveTranslateLanguage();

		Installer.setCurrStep(this, Installer.InstallStep.INSTALL_FINISHED);
	}

	@Override
	protected void selectKeyboard() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), KeyboardSelector.class);
		
		startActivity(intent);
	}

	
	protected void fail() {
		showTryLaterDialog();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mTryLaterDialog != null && mTryLaterDialog.isShowing())
			mTryLaterDialog.dismiss();
	}

	

	/**
	 * Create the alert dialog to notice the information
	 */
	private void showTryLaterDialog() {
		mTryLaterDialog = new AlertDialog.Builder(this).create();
		mTryLaterDialog.setCancelable(false);
		mTryLaterDialog.setTitle(R.string.install_select_language_notice_title);
		mTryLaterDialog.setMessage(getResources().getString(R.string.install_select_language_notice_description));
		mTryLaterDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.install_select_language_notice_ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mTryLaterDialog.dismiss();
				done();
			}
		});

		mTryLaterDialog.show();
	}

}