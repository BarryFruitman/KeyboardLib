/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.updating;

public class TypeSmartIntent {
	// Sync message
	public static final String ACTION_PING_TO_FREE_APP = "typesmart.intent.action.PING_TO_FREE_APP";
	public static final String ACTION_PING_TO_PAID_APP = "typesmart.intent.action.PING_TO_PAID_APP";
	
	// Request message
	public static final String ACTION_REQUEST_CONFIG_BACKUP = "typesmart.intent.action.REQUEST_CONFIG_BACKUP";
	
	// Update database & preference
	public static final String ACTION_SAVED_ON_SDCARD = "typesmart.intent.action.SAVED_ON_SDCARD";
	public static final String EXTRA_SAVED_PATH = "sdcard_saved_path";
	
	public static final String ACTION_UPDATED_FROM_SDCARD = "typesmart.intent.action.UPDATED_FROM_SDCARD";
}
