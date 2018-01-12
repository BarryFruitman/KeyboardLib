/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class KillActivityReceiver extends BroadcastReceiver {
	
	private Activity mVictim;
	
	public KillActivityReceiver(Activity victim) {
		mVictim = victim;
	}

	@Override
	public void onReceive(Context content, Intent intent) {
		mVictim.finish();
	}

}
