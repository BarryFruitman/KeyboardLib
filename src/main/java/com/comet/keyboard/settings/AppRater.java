/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.R;
import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardApp.AppStore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppRater extends Activity {
	private final static int MILLISECONDS_PER_DAY = 60 * 60 * 24 * 1000;
	private final static int WAIT_BETWEEN_REQUESTS = 2 * MILLISECONDS_PER_DAY;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		showRateDialog(this);
	}


	public static void promptUserToRate(Context context) {
		AppStore appStore = KeyboardApp.getApp().mAppStore; 
		if(appStore == AppStore.Google) {
			if (isReadyToShow(context)) {
				Intent intent = new Intent(context, AppRater.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}
		} else if(appStore == AppStore.Amazon)
			;
		else if(appStore == AppStore.Nook)
			;
		else if(appStore == AppStore.Opera)
			;
	}
	
	
	private static boolean isReadyToShow(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
		if (prefs.getBoolean("dont_show_again", false)) {
			// User either rated app, or doesn't want to
			return false;
		}

		// Check time of last request
		long now = System.currentTimeMillis();
		long last_request = prefs.getLong("last_request", 0);
		if(last_request == 0) {
			// This is the first call. Save the time of this request.
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong("last_request", now);
			editor.commit();
		} else if(now - last_request > WAIT_BETWEEN_REQUESTS)
			return true;

		return false;
	}
	

	/**
	 * Show the rating dialog.
	 * @param mContext
	 */
	@SuppressLint("CommitPrefEdits")
	public void showRateDialog(final Context mContext) {
		final Dialog dialog = new Dialog(mContext);
		String rateApp = String.format(mContext.getResources().getString(R.string.settings_rate_app), 
				KeyboardApp.appName);
		
		dialog.setTitle(rateApp);
		dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);

		SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
		final SharedPreferences.Editor editor = prefs.edit();
		
		LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);

		// The dialog message
		TextView tv = new TextView(mContext);
		String description = String.format(mContext.getResources().getString(R.string.settings_rate_description), 
				KeyboardApp.appName);
		tv.setText(description);
		tv.setWidth(240);
		tv.setPadding(4, 0, 4, 10);
		ll.addView(tv);

		Button b1 = new Button(mContext);
		b1.setText(rateApp);
		b1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, 
						Uri.parse(KeyboardApp.getApp().getRatingUrl())));
				if (editor != null) {
					editor.putBoolean("dont_show_again", true);
					editor.commit();
				}
				dialog.dismiss();
				finish();
			}
		});
		ll.addView(b1);

		Button b2 = new Button(mContext);
		b2.setText(mContext.getResources().getString(R.string.settings_rate_remind_me_later));
		b2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (editor != null) {
					editor.putLong("last_request", System.currentTimeMillis());
					editor.commit();
				}
				dialog.dismiss();
				finish();
			}
		});
		ll.addView(b2);

		Button b3 = new Button(mContext);
		b3.setText(mContext.getResources().getString(R.string.settings_rate_no_thanks));
		b3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (editor != null) {
					editor.putBoolean("dont_show_again", true);
					editor.commit();
				}
				dialog.dismiss();
				finish();
			}
		});
		ll.addView(b3);
		
		dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					AppRater.this.finish();
					return true;
				}
				return false;
			}
		});
		
		dialog.setContentView(ll);
		dialog.show();
		dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);
	}
}