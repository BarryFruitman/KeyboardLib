/* 
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertzSlLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwertz_sl);

	private final String[] adjacentKeys = {"asš","bnv","cčćvx","dđsšf","erw","fdđg","gfh","hgj","iuo","jhk","kjl","lk","mn","nmb","oip","po","qw","ret","sšadđ","trzž","uzži","vcčćb","weq","xcčćy","yx","zžtu"};
	private final String[] surroundingKeys = {"asšqw","bhnv","cčćvfx","dđersšfx","esšrdđw","frtdđgcčć","gfhzžtv","hzžugjb","iuokj","juihkn","kiomjl","lopk","mnk","njbm","oiplk","pol","qaw","retfdđ","sšadđewy","trzžgf","uzžijh","vgcčćb","wesšqa","xcčćdđy","ysšx","zžuthg"};
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
		case 'č':
		case 'Č':
			return "č";
		case 'ć':
		case 'Ć':
			return "ć";
		case 'đ':
		case 'Đ':
			return "đ";
		case 'š':
		case 'Š':
			return "š";
		case 'ž':
		case 'Ž':
			return "ž";
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
		case 'č':
		case 'Č':
			return "č";
		case 'ć':
		case 'Ć':
			return "ć";
		case 'đ':
		case 'Đ':
			return "đ";
		case 'š':
		case 'Š':
			return "š";
		case 'ž':
		case 'Ž':
			return "ž";
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
