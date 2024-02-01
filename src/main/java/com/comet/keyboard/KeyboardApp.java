/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.comet.keyboard.announcements.AnnouncementsManager;
import com.comet.data.api.DictionaryItem;
import com.comet.keyboard.dictionary.updater.DictionaryUpdater;
import com.comet.keyboard.dictionary.updater.OnDictionaryUpdatedListener;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.theme.KeyboardThemeManager;



public class KeyboardApp extends Application implements OnDictionaryUpdatedListener {
	public final static String FREE_APP_PACKAGE = "com.comet.android.TypeSmart";
	public final static String PAID_APP_PACKAGE = "com.comet.TypeSmart";
	public final static String FREE_APP_PATH = "data/data/" + FREE_APP_PACKAGE + "/";
	public final static String PAID_APP_PATH = "data/data/" + PAID_APP_PACKAGE + "/";
	
	protected static KeyboardApp mKeyboardApp;
	
	public static String version = "";
	public static String appName = "";
	public static String packageName = "";
	public final static String LOG_TAG = "Comet Keyboard";

	// Dictionary Updater
	private DictionaryUpdater mUpdater;
	private Intent mUpdaterService;
	
	public enum AppStore {
		Google,
		Amazon,
		Nook,
		Opera
	};
	// Set this variable to the targeted app store
	public final AppStore mAppStore = AppStore.Google;



	@Override
	public void onCreate() {
		super.onCreate();
		
//		android.os.Debug.waitForDebugger();
		
		mKeyboardApp = this;
		appName = getString(R.string.ime_name);
		version = getString(R.string.version);
		packageName = getString(R.string.ime_packagename);


		/*
		 *  BAD	BAD	BAD	BAD	BAD	BAD	BAD	BAD	BAD 
		 */
		
        // build profile manager
     	new LanguageProfileManager(this);
     	
		// Start dictionary updater
		mUpdater = getUpdater();

        // Create theme manager
        new KeyboardThemeManager(this);
        
//        mUpdaterService = new Intent(this, DictionaryUpdaterService.class);
//        startForegroundService(mUpdaterService);
        
        Settings.enableDebugMode(false);
        
		AnnouncementsManager.postAnnouncements(this);
	}

	
	
	public void stopProcessing() {
		mUpdater.stopUpdate();
	}

	
	
	public void onTerminate() {
		super.onTerminate();
		
		Log.v(KeyboardApp.LOG_TAG, "on terminate");
		
		stopService(mUpdaterService);
		
		stopProcessing();
		// registrar.stop();
	}
	
	
	
	public static KeyboardApp getApp() {
		return mKeyboardApp;
	}
	
	
	
	@Override
	public void onDictionaryItemUpdated(DictionaryItem item) {
	}


	
	@Override
	public void onDictionaryUpdated(ArrayList<DictionaryItem> items) {
		Log.v(KeyboardApp.LOG_TAG, "Dictionaries is not up-to-date - " + items.size());

		if(getUpdater().checkNeedUpdate(this)){
			if(KeyboardService.IME != null){
				KeyboardService.IME.showSuggestionDictionaryUpdateOnUi();
			}

		}
	}

	
	
	public void removeNotificationTry() {
		if(KeyboardService.IME != null)
			KeyboardService.IME.setNeedUpdateDicts(false);

		getUpdater().setUpdatedStatus(false);
	}
	
	
	
	public DictionaryUpdater getUpdater() {
		if(mUpdater == null) {
			mUpdater = new DictionaryUpdater(this);
			mUpdater.setOnDictionaryUpdatedListener(this);
		}

		return mUpdater;
	}
	
	

	public String getAppStoreUrl() {
		String ratingUrl = getString(R.string.google_play_url);
		if(mAppStore == AppStore.Amazon)
			ratingUrl = getString(R.string.amazon_appstore_url);
		else if(mAppStore == AppStore.Nook)
			;
		
		return ratingUrl;
	}



	public String getRatingUrl() {
	String ratingUrl = getString(R.string.google_rating_url);
		if(mAppStore == AppStore.Amazon)
			ratingUrl = getString(R.string.amazon_rating_url);
		else if(mAppStore == AppStore.Nook)
			;
		
		return ratingUrl;
	}



	public String getProduct() {
		return getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).getString("product", getString(R.string.product));
	}



	public String getProductType() {
		return getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).getString("product_type", getString(R.string.product_type));
	}



	public void setProductType(String productType) {
		getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit().putString("product_type", productType).commit();
	}
}
