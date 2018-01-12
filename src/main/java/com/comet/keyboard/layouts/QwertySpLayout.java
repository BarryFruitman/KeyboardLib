/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertySpLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwerty_sp);

	private final String[] adjacentKeys = {"as","bnv","cvx","dsf","erw","fdg","gfh","hgj","iuo","jhk","kjl","l","mn","nmb","oip","po","qw","ret","sad","try","uyi","vcb","weq","xcz","ytu","zx"};
	private final String[] surroundingKeys = {"aswqz","bghnv","cdfvx","desrxcf","esrdw","frtdgcv","gfhtyvb","huyngjb","iuojk","juinmhk","kiomjl","lopkm","mnlk","nmbhj","oiplk","pol","qaw","retdf","sadewzx","tryfg","uyihj","vcbfg","waseq","xsdcz","ytugh","zasx"};
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
		
		return String.valueOf(key);
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

		return "" + key;
	}


	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
