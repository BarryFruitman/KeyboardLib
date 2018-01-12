/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary.updater;

import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.res.Resources;

import com.comet.keyboard.R;

public class DictionaryItem {
	private Context mContext;
	private Resources mRes;
	
	public boolean isNeedUpdate = false;
	public boolean isInstalled = false;
	
	public long id;
	public float version;
	public String lang;
	public ArrayList<DictionaryFileItem> fileItems;
	
	public DictionaryItem(Context context) {
		mContext = context;
		mRes = mContext.getResources();
		
		isNeedUpdate = false;
		version = 0;
		lang = "";
		fileItems = new ArrayList<DictionaryFileItem>();
	} 
	
	/**
	 * Parse dictionary item info
	 * @param xml
	 */
	boolean parseDicInfo(Node node, int idx) {
		NodeList nodeList;
		boolean result = false;
		try {
			// Retrieve properties
			NamedNodeMap propMap;
			propMap = node.getAttributes();
			
			version = Float.parseFloat(propMap.getNamedItem(
					mRes.getString(R.string.xml_dictionary_property_version))
					.getNodeValue());
					
			lang = propMap.getNamedItem(
					mRes.getString(R.string.xml_dictionary_property_lang))
					.getNodeValue();
			
			nodeList = node.getChildNodes();
			if (nodeList.getLength() == 0)
				return false;
			
			// Retrieve file entry
			DictionaryFileItem newItem;
			for (int i = 0; i < nodeList.getLength(); i++) {
				node = nodeList.item(i);
				
				if (node.getNodeName().equals(mRes.getString(R.string.xml_dictionary_entry_filename))) {
					newItem = new DictionaryFileItem(mContext);
	
					if (newItem.parseDicInfo(node))
						fileItems.add(newItem);
					
					result = true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		
		return result;
	}
	
	/**
	 * Get total of dictionary file size
	 * @return
	 */
	public long getDicTotalSize() {
		int totalSize = 0;
		
		for (int i = 0 ; i < fileItems.size() ; i++) {
			DictionaryFileItem fileItem = fileItems.get(i);
			
			totalSize += fileItem.size;
		}
		
		return totalSize;
	}
	
	public String toString() {
		String str;
		
		str = "id = " + id + ", version = " + version + ", lang = " + lang;
		return str;
	}
}
