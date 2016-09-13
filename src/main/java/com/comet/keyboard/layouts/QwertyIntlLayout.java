/* 
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.layouts;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

public class QwertyIntlLayout extends FullLayout {

	private static String mId = KeyboardApp.getApp().getString(R.string.kb_id_qwerty_intl);

	private final String[] adjacentKeys = {"aáàâäsš","bnñv","cçčvx","dsšf","eéèêërw","fdg","gfh","hgj","iíîïuúùûüoóôö","jhk","kjl","lk","mnñ","nñmb","oóôöiíîïp","poóôö","qw","reéèêët","sšaáàâäd","try","uúùûüyíiîï","vcçčb","weéèêëq","xcçčzž","ytuúùûü","zžx"};
	private final String[] surroundingKeys = {"aáàâäsš","bnñv","cçčvx","dsšf","eéèêërw","fdg","gfh","hgj","iíîïuúùûüoóôö","jhk","kjl","lk","mnñ","nñmb","oóôöiíîïp","poóôö","qw","reéèêët","sšaáàâäd","try","uúùûüyíiîï","vcçčb","weéèêëq","xcçčzž","ytuúùûü","zžx"};
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
		case 'á':
		case 'Á':
			return "á";
		case 'à':
		case 'À':
			return "à";
		case 'â':
		case 'Â':
			return "â";
		case 'ä':
		case 'Ä':
			return "ä";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'č':
		case 'Č':
			return "č";
		case 'é':
		case 'É':
			return "é";
		case 'è':
		case 'È':
			return "è";
		case 'ê':
		case 'Ê':
			return "ê";
		case 'ë':
		case 'Ë':
			return "ë";
		case 'í':
		case 'Í':
			return "í";
		case 'î':
		case 'Î':
			return "î";
		case 'ï':
		case 'Ï':
			return "ï";
		case 'ñ':
		case 'Ñ':
			return "ñ";
		case 'ó':
		case 'Ó':
			return "ó";
		case 'ô':
		case 'Ô':
			return "ô";
		case 'ö':
		case 'Ö':
			return "ö";
		case 'š':
		case 'Š':
			return "š";
		case 'ú':
		case 'Ú':
			return "ú";
		case 'ù':
		case 'Ù':
			return "ù";
		case 'û':
		case 'Û':
			return "ü";
		case 'ü':
		case 'Ü':
			return "ü";
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
		case 'á':
		case 'Á':
			return "á";
		case 'à':
		case 'À':
			return "à";
		case 'â':
		case 'Â':
			return "â";
		case 'ä':
		case 'Ä':
			return "ä";
		case 'ç':
		case 'Ç':
			return "ç";
		case 'č':
		case 'Č':
			return "č";
		case 'é':
		case 'É':
			return "é";
		case 'è':
		case 'È':
			return "è";
		case 'ê':
		case 'Ê':
			return "ê";
		case 'ë':
		case 'Ë':
			return "ë";
		case 'í':
		case 'Í':
			return "í";
		case 'î':
		case 'Î':
			return "î";
		case 'ï':
		case 'Ï':
			return "ï";
		case 'ñ':
		case 'Ñ':
			return "ñ";
		case 'ó':
		case 'Ó':
			return "ó";
		case 'ô':
		case 'Ô':
			return "ô";
		case 'ö':
		case 'Ö':
			return "ö";
		case 'š':
		case 'Š':
			return "š";
		case 'ú':
		case 'Ú':
			return "ú";
		case 'ù':
		case 'Ù':
			return "ù";
		case 'û':
		case 'Û':
			return "ü";
		case 'ü':
		case 'Ü':
			return "ü";
		case 'ž':
		case 'Ž':
			return "ž";
		default:
			return "" + key;
		}

	}



	@Override
	public boolean isAdjacentToSpaceBar(char key) {
		return adjacentToSpaceBar.indexOf((int) key) >= 0;
	}
}
