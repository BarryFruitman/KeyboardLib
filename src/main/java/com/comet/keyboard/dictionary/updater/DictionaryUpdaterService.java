/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard.dictionary.updater;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;

/**
 * Dictionary Updater Service
 * 
 */
public class DictionaryUpdaterService extends Service {

	// first run
	public static long TIME_INIT_DELAY = 5 * 60 * 1000L; // runs checking updates timer in 5 min   

	// repeat check period
	public static long TIME_REPEAT_CHECK = 1 * 60 * 60 * 1000L; // period of checking updates timer (every 1 hour )
	

	private Timer timer;

	private KeyboardApp mApp;

	private TimerTask updateTask = new TimerTask() {
		@Override
		public void run() {
			
			DictionaryUpdater updater = mApp.getUpdater();

			if (updater != null && mApp.getUpdater().isNeedCheckingUpdate())
				updater.loadDictionaryList();

		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mApp = (KeyboardApp) getApplicationContext();

		timer = new Timer();
		timer.schedule(updateTask, mApp.getUpdater().getDicCheckTime() != 0 ? TIME_INIT_DELAY : 0, TIME_REPEAT_CHECK);

		Log.v(KeyboardApp.LOG_TAG, "created dictionary updater service " +  mApp.getUpdater().getDicCheckTime() );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		timer.cancel();
		timer = null;

		Log.v(KeyboardApp.LOG_TAG, "destroyed dictionary updater service");
	}
}