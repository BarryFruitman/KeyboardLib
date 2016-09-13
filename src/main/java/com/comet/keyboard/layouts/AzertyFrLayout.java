/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class AzertyFrLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_azerty_fr);

	private final String[] adjacentKeys = {"aàâz","bnv","cçvx","dsf","eéèêëzt","fdg","gfh","hgj","iîïuo","jhk","kjl","lmk","ml","nb","oôip","po","qs","ret","sdq","try","uùûüyi","vcb","wx","xcw","yÿtu","zae"};
	private final String[] surroundingKeys = {"aàâzq","bghnv","cçfvx","desxf","eéèêëztd","frdgc","gfhtv","hygjbv","iîïuok","junbhk","kiomjl","lomk","mpl","njkb","oôipl","pom","qasz","retf","sdqzw","tryg","uùûüyij","vcbg","wsx","xdcw","yÿtuh","zaes"};
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
			return "àâ";
		case 'â':
		case 'Â':
			return "âà";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'é':
		case 'É':
			return "éèêë";
		case 'è':
		case 'È':
			return "èéêë";
		case 'ê':
		case 'Ê':
			return "êéèë";
		case 'ë':
		case 'Ë':
			return "ëéèê";
		case 'î':
		case 'Î':
			return "îï";
		case 'ï':
		case 'Ï':
			return "ïî";
		case 'ô':
		case 'Ô':
			return "ô";
		case 'ù':
		case 'Ù':
			return "ùûü";
		case 'û':
		case 'Û':
			return "ûùü";
		case 'ü':
		case 'Ü':
			return "üùû";
		case 'ÿ':
		case 'Ÿ':
			return "ÿ";
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
			return "àâ";
		case 'â':
		case 'Â':
			return "âà";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'é':
		case 'É':
			return "éèêë";
		case 'è':
		case 'È':
			return "èéêë";
		case 'ê':
		case 'Ê':
			return "êéèë";
		case 'ë':
		case 'Ë':
			return "ëéèê";
		case 'î':
		case 'Î':
			return "îï";
		case 'ï':
		case 'Ï':
			return "ïî";
		case 'ô':
		case 'Ô':
			return "ô";
		case 'ù':
		case 'Ù':
			return "ùûü";
		case 'û':
		case 'Û':
			return "ûùü";
		case 'ü':
		case 'Ü':
			return "üùû";
		case 'ÿ':
		case 'Ÿ':
			return "ÿ";
		default:
			return "" + key;
		}
	}



	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
