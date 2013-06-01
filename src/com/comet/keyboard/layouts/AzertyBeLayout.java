/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class AzertyBeLayout extends FullLayout {
	
	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_azerty_be);

	private final String[] adjacentKeys = {"aàz","bnv","cçvx","dsf","eéèrz","fdg","gfh","hgj","iuo","jhk","kjl","lkm","ml","nb","oipç","poà","qs","ret","sdq","try","uyiè","vcb","wx","xcw","ytu","zaeé"};
	private final String[] surroundingKeys = {"aàzq","bjhnv","cçfgvx","desrfx","eéèsrdz","frtdgc","gfhtyv","huygjb","iuojk","juinhk","kiojl","lopkm","mpl","nbkj","oiplk","polm","qasz","retdf","sdewzq","tryfg","uyihjè","vcbhg","wsdx","xfdcw","ytugh","zaeésq"};
	private final String adjacentToSpaceBar = "vbn";

	public String getId() {
		return mId;
	}
	
	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getAdjacentKeys(char key) {
		if((key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z')) {
			if(Character.isLowerCase(key))
				return adjacentKeys[key - 'a'];
			else
				return adjacentKeys[Character.toLowerCase(key) - 'a'];
		}

		switch (key) {
		case 'à':
		case 'À':
			return "à";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'é':
		case 'É':
			return "é";
		case 'è':
		case 'È':
			return "è";
		default:
			return "" + key;
		}
	}



	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getSurroundingKeys(char key) {
		if((key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z')) {
			if(Character.isLowerCase(key))
				return surroundingKeys[key - 'a'];
			else
				return surroundingKeys[Character.toLowerCase(key) - 'a'];
		}

		switch (key) {
		case 'à':
		case 'À':
			return "à";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'é':
		case 'É':
			return "é";
		case 'è':
		case 'È':
			return "è";
		default:
			return "" + key;
		}
	}



	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
