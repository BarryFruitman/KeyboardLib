/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.comet.keyboard.util.Utils;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class About extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(Utils.formatStringWithAppName(this, R.string.about_title));
		
		setContentView(R.layout.about);
		
		onUpdateUI();
		
	}
	
	protected void onUpdateUI() {
		// Set window title
		setTitle(Utils.formatStringWithAppName(this, R.string.about_title));
		
		// Show version info
		((TextView)findViewById(R.id.tvAboutTitle)).setText(Utils.formatStringWithAppName(this, R.string.about_title));
		((TextView)findViewById(R.id.tvProductVersion)).setText(getProductVersionInfo());
		((TextView)findViewById(R.id.tvDicVersion)).setText(getDicVersionInfo());
	}



	/**
	 * Get product version information 
	 */
	private String getProductVersionInfo() {
		return String.format(getString(R.string.about_product_version), 
				getString(R.string.version),
				getString(R.string.product_type));
	}



	/**
	 * Get dictionary version information
	 */
	private String getDicVersionInfo() {
		StringBuilder dicVersions = new StringBuilder();
		
		List<String> dics = KeyboardApp.getApp().getUpdater().getInstalledDictionaryNames();
		for(String dic : dics)
			dicVersions.append("\n\t").append(dic);

		return String.format(getString(R.string.about_dic_version), dicVersions.toString());
	}
}
