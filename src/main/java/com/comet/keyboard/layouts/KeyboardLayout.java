/* 
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import android.content.Context;
import android.content.SharedPreferences;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.dictionary.EditDistance;
import com.comet.keyboard.settings.Settings;

public abstract class KeyboardLayout {
	private static KeyboardLayout mCurrentLayout = null;
	
	abstract protected String getAdjacentKeys(char key);
	abstract protected String getSurroundingKeys(char key);
	abstract public boolean isAdjacentToSpaceBar(char key);
	abstract public String getId();


	public boolean showSuperLetters() {
		return true;
	}

	public boolean isAdjacentTo(char key1, char key2) {
		return getSurroundingKeys(key1).indexOf(key2) >= 0;
	}

	
	static private String getCurrentLayoutId() {
		if(mCurrentLayout == null) {
			// Initialize the current layout from preferences
			SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
			mCurrentLayout = setCurrentLayout(sharedPrefs.getString("keyboard", KeyboardApp.getApp().getString(R.string.kb_id_default)));
		}

		return mCurrentLayout.getId();
	}

	
	static public KeyboardLayout setCurrentLayout(String keyboardLayoutId) {
		
		SharedPreferences.Editor preferenceEditor = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		preferenceEditor.putString("keyboard", keyboardLayoutId);
		preferenceEditor.commit();
		
		if(mCurrentLayout != null && keyboardLayoutId.equals(mCurrentLayout.getId()))
			return mCurrentLayout;

		Context context = KeyboardApp.getApp();
		if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwerty_en)))
			mCurrentLayout = new QwertyEnLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwerty_intl)))
			mCurrentLayout = new QwertyIntlLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwerty_sp)))
			mCurrentLayout = new QwertySpLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwerty_sl)))
			mCurrentLayout = new QwertySlLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwerty_sv)))
			mCurrentLayout = new QwertySvLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_azerty_fr)))
			mCurrentLayout = new AzertyFrLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_azerty_be)))
			mCurrentLayout = new AzertyBeLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwertz_de)))
			mCurrentLayout = new QwertzDeLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_qwertz_sl)))
			mCurrentLayout = new QwertzSlLayout();
		else if(keyboardLayoutId.equals(context.getString(R.string.kb_id_t9)))
			mCurrentLayout = new T9Layout();
		else
			// English QWERTY is the default
			mCurrentLayout = new QwertyEnLayout();

		return mCurrentLayout;
	}


	static public KeyboardLayout getCurrentLayout() {
		if(mCurrentLayout != null)
			return mCurrentLayout;
		
		return mCurrentLayout = setCurrentLayout(getCurrentLayoutId());
	}
}
