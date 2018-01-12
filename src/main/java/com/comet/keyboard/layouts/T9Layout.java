/* 
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class T9Layout extends KeyboardLayout {

	private final String[] adjacentKeys = {"abc","bac","cab","def","edf","fde","ghi","hgi","igh","jkl","kjl","ljk","mno","nmo","omn","pqrs","qprs","rpqs","spqr","tuv","utv","vtu","wxyz","xwyz","ywxz","zwxy"};
	private final String adjacentToSpaceBar = "tuvwxyz";

	
	public String getId() {
		return KeyboardApp.getApp().getString(R.string.kb_id_t9);
	}
	
	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getAdjacentKeys(char key) {
		if(!((key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z'))) 
			return "" + key;

		if(Character.isLowerCase(key))
			return adjacentKeys[key - 'a'];
		else
			return adjacentKeys[Character.toLowerCase(key) - 'a'];
	}



	// Returns a string containing key and its adjacent keyboard keys
	@Override
	protected String getSurroundingKeys(char key) {
		return getAdjacentKeys(key);
	}
	
	
	
	public int getSubstituteEditDistance() {
		return 0;
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
