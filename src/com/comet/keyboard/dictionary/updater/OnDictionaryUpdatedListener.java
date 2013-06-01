/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary.updater;

import java.util.ArrayList;

public interface OnDictionaryUpdatedListener {
	public void onDictionaryItemUpdated(DictionaryItem item);
	public void onDictionaryUpdated(ArrayList<DictionaryItem> items);
}
