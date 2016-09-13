/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.theme;

import android.content.Context;

import java.util.ArrayList;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.util.ProfileTracer;

/**
 * Manage all keyboard themes. Mainly this class just maintains a list of
 * available themes, and the current theme.
 * 
 * TODO: Obsolete themes list arrays and get themes list from XML file. 
 * 
 * @author Barry
 *
 */
public class KeyboardThemeManager {
	private static KeyboardThemeManager mManager = null;
	
	// Current keyboard theme
	private static String currThemeName;
	private static String currThemeValue;
	private static KeyboardTheme mCurrTheme = null;
	
	private static Context mContext;
	
	private static String[] mPublicNames;
	private static String[] mPublicValues;
	
	private static ArrayList<String> mNames = new ArrayList<String>();
	private static ArrayList<String> mValues = new ArrayList<String>();
	
	public KeyboardThemeManager(Context context) {
		mManager = this;
		
		mContext = context;
		
		ProfileTracer tracer = new ProfileTracer();
		
		// Set default theme name & value
		currThemeName = context.getString(R.string.default_theme_name);
		currThemeValue = context.getString(R.string.default_theme_id);
		
		tracer.log("Downloading themes list...");
		downloadKeyboardThemesList();
		tracer.log("Loading themes...");
		reloadTheme();
		tracer.log("Done");
	}

	/**
	 * Retrieve theme mManager
	 * @return
	 */
	public static KeyboardThemeManager getThemeManager() {
		return mManager;
	}
	
	/**
	 * Retrieve current theme
	 * @return
	 */
	public static KeyboardTheme getCurrentTheme() {
		Assert.assertTrue(currThemeName != null && !currThemeName.trim().equals(""));
		Assert.assertTrue(currThemeValue != null && !currThemeValue.trim().equals(""));
		
		// Check if it's already loaded or not
		if (mCurrTheme != null &&
			mCurrTheme.getThemeName().equals(currThemeName)) {
			return mCurrTheme;
		}
		
		// reload current theme
		mCurrTheme = null;
		System.gc();
		mCurrTheme = new KeyboardTheme(mContext, currThemeName, currThemeValue);
		
		return mCurrTheme;
	}
	
	private void downloadKeyboardThemesList() {
		mPublicNames = mContext.getResources().getStringArray(R.array.theme_names);
		mPublicValues = mContext.getResources().getStringArray(R.array.theme_ids);
		
		for (String name : mPublicNames) {
			mNames.add(name);
		}
		for (String value : mPublicValues) {
			mValues.add(value);
		}
		
		// Add special theme
		/// white theme
		mNames.add(mContext.getResources().getString(R.string.theme_name_white));
		mValues.add(mContext.getResources().getString(R.string.theme_id_white));
		
		/// black theme
		mNames.add(mContext.getResources().getString(R.string.theme_name_black));
		mValues.add(mContext.getResources().getString(R.string.theme_id_black));
	}
	
	public void reloadTheme() {
		setCurrKeyboardThemeByValue(mContext, KeyboardTheme.getThemeKey(mContext));
	}
	
	/**
	 * Retrieve keyboard theme from xml file
	 * @param xmlResId
	 * @return
	 */
	public static String getKeyboardThemeName(Context context, String themeValue) {
		int index;
		for (index = 0 ; index < mValues.size() ; index++) {
			String value = mValues.get(index); 
			if (value.equals(themeValue)) {
				return mNames.get(index);
			}
		}
		
		return null;
	}
	
	public static String getKeyboardThemeValue(Context context, String themeName) {
		int index;
		for (index = 0 ; index < mValues.size() ; index++) {
			String name = mNames.get(index); 
			if (name.equals(themeName)) {
				return mValues.get(index);
			}
		}
		
		return null;
	}
	
	public static void setCurrKeyboardThemeByValue(Context context, String themeValue) {
		if(mManager == null)
			return;

		currThemeValue = themeValue;
		currThemeName = getKeyboardThemeName(context, currThemeValue);
		if(currThemeName == null) {
			// Use defaults
			currThemeName = context.getString(R.string.default_theme_name);
			currThemeValue = context.getString(R.string.default_theme_id);
		}
		
		KeyboardTheme.setThemeKey(context, currThemeValue);
	}
	
	public static void setCurrKeyboardThemeByName(Context context, String themeName) {
		if(mManager == null)
			return;
		
		currThemeName = themeName;
		currThemeValue = getKeyboardThemeValue(context, currThemeName);
		if(currThemeValue == null) {
			// Use defaults
			currThemeName = context.getString(R.string.default_theme_name);
			currThemeValue = context.getString(R.string.default_theme_id);
		}

		KeyboardTheme.setThemeKey(context, currThemeValue);
	}
}
