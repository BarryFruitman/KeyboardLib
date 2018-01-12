/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install;

import android.content.Intent;

import com.comet.keyboard.Home;

public class KeyboardSelector extends com.comet.keyboard.settings.KeyboardSelector {
	@Override
	protected void done() {
		Installer.setCurrStep(this, Installer.InstallStep.INSTALL_FINISHED);

		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setClass(getApplicationContext(), Home.class);
		startActivity(intent);
	}
}
