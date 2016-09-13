/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary.updater;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.comet.keyboard.R;

import android.content.Context;
import android.content.res.Resources;

public class DictionaryFileItem {
	private Context mContext;
	private Resources mRes;
	
	public String filename = "";
	public long size;
	
	public DictionaryFileItem(Context context) {
		mContext = context;
		mRes = mContext.getResources();
		
		size = 0;
	}
	
	/**
	 * Parse dictionary item info
	 * @param xml
	 */
	boolean parseDicInfo(Node node) {
		NamedNodeMap propMap;
		boolean result = true;
		
		try {
			propMap = node.getAttributes();

			// Retrieve properties
			filename = propMap.getNamedItem(mRes.getString(R.string.xml_file_property_name)).getNodeValue();
			size = Long.parseLong(propMap.getNamedItem(mRes.getString(R.string.xml_file_property_size)).getNodeValue());
		} catch (Exception e) {
			return false;
		}
		
		return result;
	}
	
	public String toString() {
		String str;
		
		str = "filename = " + filename + ", size = " + size;
		return str;
	}
}
