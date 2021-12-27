/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.dictionary.updater;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.Utils;

public class DictionaryUpdater {
	private final static long CHECK_LAST = 24 * 60 * 60 * 1000L; // check new updates every 1 day
	private final static long CHECK_AFTER_UPDATE = 7 * 24 * 60 * 60 * 1000L; // after updating don't check  again for 7 days.

	// Context & Resource Manager
	private final Context mContext;
	private final Resources mRes;

	// Current dictionary list
	private ArrayList<DictionaryItem> mDicList;

	private OnDictionaryUpdatedListener mDicUpdatedListener;

	// Period of updating time
	private static Handler mUIHandler = null;

	// Is marked as unread for the updating info
	private boolean mIsNeedUpdate = false;

	private final ReentrantLock lock = new ReentrantLock();


	public DictionaryUpdater(final Context context) {
		mContext = context;
		mRes = mContext.getResources();
		if (mUIHandler == null) {
			mUIHandler = new Handler();
		}

		mDicList = new ArrayList<>();

		// load dictionary list from database
		refreshDictionaryListFromDb();
	}


	void refreshDictionaryListFromDb() {
		try {
			lock.lock();
			DatabaseHelper
					.safeGetDatabaseHelper(mContext)
					.loadDicInfo(mContext, mDicList);
		} finally {
			lock.unlock();
		}
	}


	public void stopUpdate() {
		// TODO: Implement this or remove it.
	}


	/**
	 * Load dictionary list from Internet
	 */
	synchronized void loadDictionaryList() {
		Log.v(KeyboardApp.LOG_TAG, "started loading dictionary list");

		try {
			ArrayList<DictionaryItem> newDicList = new ArrayList<>();
			final ArrayList<DictionaryItem> temp;

			final URL xmlURL = new URL(mRes.getString(R.string.install_dic_list_url));

			// Parse dictionary info
			boolean result = parseDicInfo(newDicList, xmlURL);

			// Save into database
			if (result) {
				lock.lock();

				try {
					// Notify dictionary info changed
					temp = newDicList;
					newDicList = mDicList;
					mDicList = temp;

					if (markAndNotifyUpdatedState(mDicList, newDicList)) {
						// Save new diction list into database
						DatabaseHelper.safeGetDatabaseHelper(mContext).saveDicInfos(mDicList);
						saveDicUpdatedTime(Utils.getTimeMilis());
					}
				} finally {
					lock.unlock();
				}
			}

			if (result) {
				Log.v(KeyboardApp.LOG_TAG, "saving check dictionary time ");
				// Set update time into preference value
				saveDicCheckTime(Utils.getTimeMilis());
			}
		} catch (Exception e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}

		Log.v(KeyboardApp.LOG_TAG, "finished loading dictionary list");
	}


	/**
	 * Returns if checking update is required
	 */
	boolean isNeedCheckingUpdate() {
		final long now = Utils.getTimeMilis();

		final boolean afterUpdate = (getDicUpdatedTime() + CHECK_AFTER_UPDATE) < now;
		final boolean afterLastCheck = (getDicCheckTime() + CHECK_LAST < now);
		final boolean isNeedAppUpgradeOrDictUpdate = isNeedUpgrade() || isNeedUpdate();

		Log.v(KeyboardApp.LOG_TAG, "isNeedCheckingUpdate(): afterUpdate " + afterUpdate + "; afterLastCheck " + afterLastCheck + "; isNeedAppUpgradeOrDictUpdate "  + isNeedAppUpgradeOrDictUpdate );

		return !isNeedAppUpgradeOrDictUpdate && afterUpdate && afterLastCheck;
	}


	/**
	 * Notify diction list changed
	 */
	private boolean markAndNotifyUpdatedState(
			final ArrayList<DictionaryItem> newDicList,
			final ArrayList<DictionaryItem> oldDicList) {
		Assert.assertTrue(newDicList != null);
		Assert.assertTrue(oldDicList != null);

		mIsNeedUpdate = false;
		if (mDicUpdatedListener != null) {
			final ArrayList<DictionaryItem> needUpdateList = new ArrayList<>();
			// Compare 2 dictionary info
			for (int i = 0; i < newDicList.size(); i++) {
				final DictionaryItem newItem = newDicList.get(i);
				final DictionaryItem oldItem = getDictionaryItemPrim(oldDicList, newItem.lang);
				if (oldItem == null || (newItem.version > oldItem.version)) {
					Log.v(KeyboardApp.LOG_TAG, "new dictionary available " + newItem.lang);
					newItem.isNeedUpdate = true;
					if(oldItem != null){
						newItem.isInstalled = oldItem.isInstalled;
					}					
					needUpdateList.add(newItem);
				}
			}

			if (needUpdateList.size() > 0) {
				mIsNeedUpdate = true;

				// Send updated event
				mDicUpdatedListener.onDictionaryUpdated(needUpdateList);
				for (int i = 0; i < needUpdateList.size(); i++) {
					mDicUpdatedListener.onDictionaryItemUpdated(needUpdateList.get(i));
				}
			} else {
				Log.v(KeyboardApp.LOG_TAG,
						"There is no need to update dictionaries");
			}
		}

		return mIsNeedUpdate;
	}


	/**
	 * Parse diction info from xml string
	 */
	private boolean parseDicInfo(ArrayList<DictionaryItem> dicList, URL url) {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(new InputSource(url.openStream()));
			int index = 0;

			doc.getDocumentElement().normalize();

			// Clear dictionary list
			dicList.clear();

			NodeList nodeList = doc.getElementsByTagName(mRes.getString(R.string.xml_entry_dictionaries));

			final Element root = (Element) nodeList.item(0);

			nodeList = root.getChildNodes();

			int minAppVersionCode = 0;

			try {
				minAppVersionCode = Integer.parseInt(root.getAttribute(mRes.getString(R.string.xml_dictionaries_property_min_version_code)));
			} catch (Exception e) {
				Log.e(KeyboardApp.LOG_TAG, "couldn't detect min version code to use up-to-date dictionaries", e);
			}

			setNeedUpgrade(minAppVersionCode);

			for (int i = 0; i < nodeList.getLength(); i++) {
				final Node node = nodeList.item(i);

				if (node.getChildNodes().getLength() > 0) {
					final DictionaryItem newItem = new DictionaryItem(mContext);
					if (newItem.parseDicInfo(node, index++)) {
						dicList.add(newItem);
					}
				}
			}
		} catch (Exception e) {
			Log.e(KeyboardApp.LOG_TAG, "parse dic info", e);
			return false;
		}

		return true;
	}


	// Load updated time
	private long getDicUpdatedTime() {
		// Set update time into preference value
		Assert.assertTrue(mContext != null);
		final SharedPreferences preference = mContext.getSharedPreferences(
				Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE);

		return preference.getLong(mRes.getString(R.string.dic_updated_time), 0);
	}


	// Save current wallpaper drawable id
	public void saveDicUpdatedTime(final long updatedTime) {
		Assert.assertTrue(mContext != null);
		SharedPreferences.Editor preferenceEditor = mContext
				.getSharedPreferences(
						Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE)
				.edit();

		preferenceEditor.putLong(mRes.getString(R.string.dic_updated_time), updatedTime);
		preferenceEditor.apply();
	}


	// Save current wallpaper drawable id
	private void saveDicCheckTime(final long updatedTime) {
		Assert.assertTrue(mContext != null);
		SharedPreferences.Editor preferenceEditor = mContext
				.getSharedPreferences(
						Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE)
				.edit();

		preferenceEditor.putLong(mRes.getString(R.string.dic_checked_time), updatedTime);
		preferenceEditor.apply();
	}


	// Load checked time
	long getDicCheckTime() {
		// Set update time into preference value
		Assert.assertTrue(mContext != null);
		final SharedPreferences preference = mContext
				.getSharedPreferences(
						Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE);

		return preference.getLong(mRes.getString(R.string.dic_checked_time), 0);
	}


	/**
	 * Retrieve dictionary item by dictionary name
	 */
	public DictionaryItem getDictionaryItem(final String dicName) {
		final DictionaryItem item;

		lock.lock();
		try {
			item = getDictionaryItemPrim(mDicList, dicName);
		} finally {
			lock.unlock();
		}

		return item;
	}


	@Nullable
	private DictionaryItem getDictionaryItemPrim(
			final ArrayList<DictionaryItem> list,
			final String dicName) {
		DictionaryItem item;

		lock.lock();
		try {
			for (int i = 0; i < list.size(); i++) {
				item = list.get(i);
				if (item.lang.equals(dicName)) {
					return item;
				}
			}
		} finally {
			lock.unlock();
		}

		return null;
	}


	/**
	 * Set event listener
	 */
	public void setOnDictionaryUpdatedListener(OnDictionaryUpdatedListener listener) {
		mDicUpdatedListener = listener;
	}


	public void markAsReadAll() {
		mIsNeedUpdate = false;
		DictionaryItem item;

		for(int i = 0; i < mDicList.size(); i++) {
			item = mDicList.get(i);
			item.isNeedUpdate = false;
		}

		DatabaseHelper.safeGetDatabaseHelper(mContext).saveDicInfos(mDicList);
	}


	/**
	 * Specified dictionary exist or not
	 */
	public boolean isDictionaryExist(final Context context, final String langID) {
		return langID.equals(context.getResources().getString(R.string.lang_code_other)) || isDictionaryExist(
				context,
				getDictionaryItem(langID));
	}


	public static boolean isDictionaryExist(
			final Context context,
			final DictionaryItem item) {

		if (item == null) {
			return false;
		}

		final ArrayList<DictionaryFileItem> fileItems = item.fileItems;

		// Check dictionary existing
		for (int i = 0; i < fileItems.size(); i++) {
			final DictionaryFileItem fileItem = fileItems.get(i);
			String folder = "databases";

			final File file = new File(
					Utils.getInternalFilePath(
							context,
							folder + "/" + fileItem.filename));

			if (!file.exists()) {
				Log.v(KeyboardApp.LOG_TAG, "!file.exists() "  + file.getAbsolutePath());
				return false;
			}

			if (file.length() < fileItem.size) {
				// File can be bigger but not smaller.
				Log.v(KeyboardApp.LOG_TAG, "file.length() < fileItem.size");
				return false;
			}
		}

		return true;
	}


	/**
	 * Retrieve dictionary version info
	 */
	public List<String> getInstalledDictionaryNames() {
		final List<String> installed = new ArrayList<>();
		
		try {
			if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
				for (int i = 0; i < mDicList.size(); i++) {
					final DictionaryItem item = mDicList.get(i);
					if (!item.isNeedUpdate) {
						final String lang = Settings.getNameFromValue(
								mContext,
								"language",
								item.lang);
						installed.add(
								String.format(
										Locale.getDefault(),
										"%s (v%d)", lang, (int) item.version));
					}
				}
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}

		return installed;
	}
	

	/**
	 *  It returns true if there is a new dictionary available, otherwise false
	 */
	public boolean isNeedUpdate() {
		return getUpdatedStatus();
	} 


	public boolean checkNeedUpdate(final Context context){
		// TODO: Move this to a method in DatabaseHelper
		final Cursor c = DatabaseHelper
				.safeGetDatabaseHelper(context)
				.mDB.query(
						DatabaseHelper.DIC_LANGUAGES_TABLE_NAME,
						null,
						DatabaseHelper.DIC_LANGUAGES_FIELD_IS_NEED_UPDATE
								+ " = 1 AND "
								+ DatabaseHelper.DIC_LANGUAGES_FIELD_IS_INSTALLED
								+ " = 1",
						null,
						null,
						null,
						null);

		final int count = c.getCount();
		c.close();
		
		Log.v(KeyboardApp.LOG_TAG, "checkNeedUpdate() " + (count > 0) );
		
		setUpdatedStatus(count > 0);
		
		if(KeyboardService.getIME() != null){
			KeyboardService.getIME().setNeedUpdateDicts(count > 0);
		}
		
		return count > 0;
	}


	/**
	 * Checks application upgrading required or not  by dictionary minimum application version code   
	 */
	public boolean isNeedUpgrade() {
		final SharedPreferences preference = mContext.getSharedPreferences(
				Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE);

		final int requiredVersionCode = preference.getInt(mContext.getString(R.string.dic_app_version_code), 0);

		try {
			final String pkg = mContext.getPackageName();
			final int mVersionNumber = mContext.getPackageManager().getPackageInfo(pkg, 0).versionCode;
	        return requiredVersionCode > mVersionNumber;
	    } catch (final NameNotFoundException e) {
	    	Log.e(KeyboardApp.LOG_TAG, "error", e);
	    }

		return false;
	}
	

	/**
	 * Dictionary has minimum application version code to up-to-date 
	 */
	private void setNeedUpgrade(final int version){
		SharedPreferences.Editor preferenceEditor
				= mContext.getSharedPreferences(
						Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE)
				.edit();

		preferenceEditor.putInt(
				mContext.getString(R.string.dic_app_version_code),
				version);
		preferenceEditor.apply();
		
		// Notify service
		if(KeyboardService.getIME() != null){
			final boolean isNeedUpgrade = isNeedUpgrade();
			KeyboardService.getIME().setNeedUpgradeApp(isNeedUpgrade);
			if(isNeedUpgrade) {
				KeyboardService.getIME().showSuggestionAppUpdateOnUi();
			}
		}
	}
	
	
	private boolean getUpdatedStatus() {
		// Get preferences
		final SharedPreferences preference
				= mContext.getSharedPreferences(
				Settings.SETTINGS_FILE,
				Context.MODE_PRIVATE);

		// Check language
		final String currLang = preference.getString(
        		"language",
				mContext.getResources().getString(R.string.lang_code_default));

		return !currLang.equals(mContext.getResources().getString(R.string.lang_code_other)) && preference.getBoolean(
				mContext.getResources().getString(R.string.prefs_is_dic_updated),
				false);
	}


	public void setUpdatedStatus(final boolean status) {
		final SharedPreferences.Editor preferenceEditor
				= mContext.getSharedPreferences(
					Settings.SETTINGS_FILE,
					Context.MODE_PRIVATE).edit();

		preferenceEditor.putBoolean(mContext.getResources().getString(R.string.prefs_is_dic_updated), status);
		
		preferenceEditor.apply();
	}
}