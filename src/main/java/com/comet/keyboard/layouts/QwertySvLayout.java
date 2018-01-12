/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertySvLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwerty_sv);

	private final String[] adjacentKeys = {"aåäs","bnv","cvx","dsf","erw","fdg","gfh","hgj","iuoö","jhk","kjl","lk","mn","nmb","oöip","poö","qw","ret","saåäd","try","uyi","vcb","weq","xcz","ytu","zx"};
	private final String[] surroundingKeys = {"aåäswq","bhnv","cfvx","desrxf","esrdw","frtdgc","gfhtyv","huygjb","iuoöjk","juinhk","kioömjl","loöpk","mnk","nmbj","oöiplk","poöl","qaåäw","retdf","saåädewz","tryfg","uyihj","vcbg","waåäseq","xdcz","ytugh","zaåäsx"};
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
		case 'å':
		case 'Å':
			return "å";
		case 'ä':
		case 'Ä':
			return "ä";
		case 'ö':
		case 'Ö':
			return "ö";
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
		case 'å':
			return "Å";
		case 'ä':
			return "Ä";
		case 'ö':
			return "Ö";
		default:
			return "" + key;
		}
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
