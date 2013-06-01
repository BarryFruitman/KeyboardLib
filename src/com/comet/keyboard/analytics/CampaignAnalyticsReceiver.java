/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2013 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.analytics;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.settings.Settings;
import com.google.analytics.tracking.android.AnalyticsReceiver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class CampaignAnalyticsReceiver extends AnalyticsReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(KeyboardApp.LOG_TAG, "CampaignAnalyticsReceiver: Enter");
		
		Uri uri = intent.getData();
		String campaign_uri = "null";
		if(uri != null)
			campaign_uri = uri.toString();
		Log.v(KeyboardApp.LOG_TAG, "campaign_uri=" + campaign_uri);


		String referrer = "null";
		Bundle extras = intent.getExtras();
		if(extras != null)
			referrer = extras.getString("referrer");
		Log.v(KeyboardApp.LOG_TAG, "referrer=" + referrer);
		
		// Save referrer
		KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit().putString("referrer", referrer).commit();
		
		KeyboardApp.getApp().getLicenseClient().updateLicenseAsync();

		super.onReceive(context, intent);
	}
}
