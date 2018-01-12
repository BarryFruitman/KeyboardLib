/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertzDeLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwertz_de);

	private final String[] adjacentKeys = {"as","bnv","cvx","dsf","erw","fdg","gfh","hgj","iuo","jhk","kjl","lkö","mn","nmb","oip","poü","qw","ret","sad","trz","uzi","vcb","weq","xcy","yx","ztu"};
	private final String[] surroundingKeys = {"asq","bjnv","cvgx","desfy","erdw","frdgx","gfhtc","hzgjv","iuok","juhkb","kinjl","lomkö","mnl","nkbm","oipl","poöü","qaw","retf","sadw","trzg","uzij","vhcb","wesq","xcfy","ydx","zuth"};
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
		case 'ä':
		case 'Ä':
			return "ÄÖ";
		case 'ö':
		case 'Ö':
			return "ÖÄL";
		case 'ü':
		case 'Ü':
			return "ÜP";
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
		case 'ä':
		case 'Ä':
			return "ÄÖÜ";
		case 'ö':
		case 'Ö':
			return "ÖPLÄ";
		case 'ü':
		case 'Ü':
			return "ÜPÄ";
		default:
			return "" + key;
		}
	}


	
	@Override
	public boolean showSuperLetters() {
		return true;
	}



	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
