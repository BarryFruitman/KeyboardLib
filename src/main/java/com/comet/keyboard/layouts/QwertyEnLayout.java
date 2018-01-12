/* 
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertyEnLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwerty_en);

	private final String[] adjacentKeys = {"as","bnv","cvx","dsf","erw","fdg","gfh","hgj","iou","jhk","kjl","lk","mn","nmb","oip","po","qw","ret","sad","try","uyi","vcb","weq","xcz","ytu","zx"};
	private final String[] surroundingKeys = {"aswq","bhnv","cfvx","desrfx","esrdw","frtdgc","gtfhyv","huygjb","iuojk","juinhk","kiomjl","lopk","mnk","nmbj","oiplk","pol","qaw","retdf","saedwz","tryfg","uyihj","vcbg","waesq","xdcz","ytugh","zsx"};
	private final String adjacentToSpaceBar = "vbn";


	public String getId() {
		return mId;
	}
	
	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getAdjacentKeys(char key) {
		if(key >= 'a' && key <= 'z')
			return adjacentKeys[key - 'a'];

		if(key >= 'A' && key <= 'Z')
			return adjacentKeys[Character.toLowerCase(key) - 'a'];

		// This is not a letter key. Return itself.
		return String.valueOf(key);
	}



	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getSurroundingKeys(char key) {
		if(key >= 'a' && key <= 'z')
			return surroundingKeys[key - 'a'];

		if(key >= 'A' && key <= 'Z')
			return surroundingKeys[Character.toLowerCase(key) - 'a'];

		// This is not a letter key. Return itself.
		return String.valueOf(key);
	}


	
	@Override
	public boolean showSuperLetters() {
		return false;
	}


	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
