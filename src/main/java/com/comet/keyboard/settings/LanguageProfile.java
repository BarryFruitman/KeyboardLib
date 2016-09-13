/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;


/**
 * This class represents a language profile. A language profile is a language/keyboard pair.
 * 
 * @author Barry Fruitman
 *
 */
public class LanguageProfile {
	private String mLang;
	private String mKeyboard;

	public LanguageProfile(String lang, String keyboard) {
		mLang = lang;
		mKeyboard = keyboard;
	}

	public String getLang() {
		return mLang;
	}

	public String getKeyboard() {
		return mKeyboard;
	}

	public void setLang(String lang) {
		mLang = lang;
	}

	public void setKeyboard(String keyboard) {
		mKeyboard = keyboard;
	}
}
