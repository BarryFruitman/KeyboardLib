/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.util.DatabaseHelper;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * LanguageProfileManager handles creating, updating and deleting language profiles.
 * Profiles are persisted to an sqlite database, so many of the methods are wrappers
 * for methods in DatabaseHelper.
 * 
 * @author Barry Fruitman
 *
 */
public class LanguageProfileManager {
	// Profile manager instance.
	private static LanguageProfileManager mProfileManager = null;
	
	// Main context.
	private Context mContext;
	
	// Database manager.
	private DatabaseHelper mDBHelper;
	
	// Listener for language change
	private OnLanguageChangedListener mOnLanguageChangedListener;
	
	
	public LanguageProfileManager(Context context) {
		this.mContext = context;
		
		mProfileManager = this;
		mDBHelper = DatabaseHelper.safeGetDatabaseHelper(mContext);
	}
	
	public static LanguageProfileManager getProfileManager() {
		Assert.assertTrue(mProfileManager != null);
		
		return mProfileManager;
	}



	/**
	 * Load profiles from database
	 */
	public ArrayList<LanguageProfile> loadProfiles() {
		return mDBHelper.getProfileItems();
	}
	
	
	
	/**
	 * Add new profile to the database.
	 * If a profile already exists for that language, it updates the keyboard.
	 * 
	 * @param lang			A language code.
	 * @param keyboard		A keyboard code.
	 */
	public void addProfile(String lang, String keyboard) {
		mDBHelper.addProfileItem(lang, keyboard);
	}
	
	
	
	/**
	 * Get a language profile from the database.
	 * 
	 * @param lang	A language code.
	 * @return		A language profile.
	 */
	public LanguageProfile getProfile(String lang) {
		LanguageProfile profile = mDBHelper.getProfileItem(lang);
		
		return profile;
	}
	
	
	
	/**
	 * Update a profile in the database with a new keyboard.
	 * If there is no profile for that language, it creates one.
	 * 
	 * @param lang		A language code.
	 * @param keyboard	A keyboard code.
	 */
	public void updateProfile(String lang, String keyboard) {
		mDBHelper.updateProfileItem(lang, keyboard);
	}
	
	

	/**
	 * Remove a profile from the database.
	 * 
	 * @param lang	A language code.
	 */
	public void removeProfile(String lang) {
		mDBHelper.removeProfileItem(lang);
	}
	
	
	
	/**
	 * Get the next profile after the current profile.
	 * Profiles are ordered alphabetically by language code.
	 * 
	 * @return	The next language profile.
	 */
	public LanguageProfile getNextProfile() {
		// Get profile list
    	SharedPreferences pref = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
    	String currentProfile = pref.getString("language", "");
    	ArrayList<LanguageProfile> profileList = LanguageProfileManager.getProfileManager().loadProfiles();
    	LanguageProfile nextProfile = null;
    	
    	if (profileList == null || profileList.size() == 0)
    		return null;
    	
    	// Get the current profile index 
		for (int i = 0 ; i < profileList.size() ; i++) {
			String strName = profileList.get(i).getLang();
			
			if (strName.equals(currentProfile)) {
				if ((i + 1) < profileList.size()) {
					nextProfile = profileList.get(i + 1);
				}
				
				break;
			}
		}
		
		if (nextProfile == null)
			nextProfile = profileList.get(0);
		
		return nextProfile;
	}
	
	
	
	/**
	 * Set the current language profile.
	 * 
	 * @param language	A language code.
	 */
	public void setCurrentProfile(String language) {
		LanguageProfile profile = getProfile(language);
		SharedPreferences sharedPrefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		// Set current language
		editor.putString("language", profile.getLang());
		editor.commit();

		// Set current keyboard
		KeyboardLayout.setCurrentLayout(profile.getKeyboard());
		
		if(mOnLanguageChangedListener != null)
			mOnLanguageChangedListener.onLanguageChanged(language);
	}



	/**
	 * Returns the current profile, based on the current language in shared prefs
	 * 
	 * @return	The current language profile
	 */
	public LanguageProfile getCurrentProfile() {
		LanguageProfile profile = getProfile(getCurrentLanguageFromPrefs());
		if(profile == null)
			// Profile doesn't exist. Create a new one based on current values in shared prefs
			profile = new LanguageProfile(getCurrentLanguageFromPrefs(), getCurrentKeyboardFromPrefs());
		
		return profile;
	}

	
	
	/**
	 * Returns the current language from shared prefs
	 * 
	 * @return	A language code
	 */
	private String getCurrentLanguageFromPrefs() {
		return mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).getString("language", mContext.getString(R.string.lang_code_default));
	}

	
	
	/**
	 * Returns the current language from shared prefs
	 * 
	 * @return	A language code
	 */
	private String getCurrentKeyboardFromPrefs() {
		return mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).getString("keyboard", mContext.getString(R.string.kb_id_default));
	}



	public void setOnLanguageChangedListener(OnLanguageChangedListener listener) {
		mOnLanguageChangedListener = listener;
	}



	public interface OnLanguageChangedListener {
		public void onLanguageChanged(String language);
	}



	/*
	 * HACKS
	 */

	/**
	 * This method is a hack for users upgrading from locale profiles to language profiles. 
	 * It creates a default language profile for a downloaded language that has no locale profile.
	 * DO NOT USE THIS METHOD.
	 * 
	 * @param context	A context to use to access shared prefs.
	 * @param langID	A language code.
	 * @return			A new LanguageProfile.
	 */
	@Deprecated
	public static LanguageProfile createDefaultProfile(Context context, String lang) {
		String keyboard = getKeyboardNameForLang(context, lang);
		LanguageProfile newItem = new LanguageProfile(lang, keyboard);
		
		return newItem;
	}

	
	
	/**
	 * This method is a hack for users upgrading from locale profiles to language profiles.
	 * It guesses what keyboard to use with a language. 
	 * DO NOT USE THIS METHOD.
	 * 
	 * @param context	A context to use to access shared prefs.
	 * @param langID	A language code.
	 * @return			A keyboard code.
	 */
	@Deprecated
	private static String getKeyboardNameForLang(Context context, String langID) {
		String[] langIDs = context.getResources().getStringArray(R.array.default_keyboard_for_language_names); 
		String[] keyboardIDs = context.getResources().getStringArray(R.array.default_keyboard_for_language_ids);
		
		for (int i = 0 ; i < langIDs.length ; i++) {
			String lang = langIDs[i];
			if (lang.equals(langID)) {
				String keyboardID = keyboardIDs[i];
				return keyboardID;
			}
		}
		
		return null;
	}
}
