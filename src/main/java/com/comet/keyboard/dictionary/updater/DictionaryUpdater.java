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

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.install.LanguageSelector;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.Utils;

public class DictionaryUpdater {
	public final static int HTTP_CONNECTION_MTIMEOUT = 1000 * 20;

	public final static long CHECK_LAST = 24 * 60 * 60 * 1000L; // check new updates every 1 day

	public final static long CHECK_AFTER_UPDATE = 7 * 24 * 60 * 60 * 1000L; // after updating don't check  again for 7 days.

	// Context & Resource Manager
	private Context mContext;
	private Resources mRes;

	// Current dictionary list
	private ArrayList<DictionaryItem> mDicList;

	// OnUpdatedListener
	private OnDictionaryUpdatedListener mDicUpdatedListener;

	// Period of updating time
	private Thread mEventThread = null;
	private static Handler mUIHandler = null;

	// Is marked as unread for the updating info
	public boolean isNeedUpdate = false;

	public ReentrantLock lock = new ReentrantLock();

	public DictionaryUpdater(Context context) {
		mContext = context;
		mRes = mContext.getResources();
		if (mUIHandler == null)
			mUIHandler = new Handler();

		mDicList = new ArrayList<DictionaryItem>();

		// load dictionary list from database
		refreshDiclistFromDb();

	}
	
	public void refreshDiclistFromDb(){
		try {
			lock.lock();
			DatabaseHelper.safeGetDatabaseHelper(mContext).loadDicInfo(mContext,
					mDicList);
		} finally {
			lock.unlock();
		}
	}

	public void stopUpdate() {
		if (mEventThread != null)
			try {
				mEventThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Load dictionary list from Internet
	 */
	public synchronized boolean loadDictionaryList() {
		Log.v(KeyboardApp.LOG_TAG, "started loading dictionary list");

		boolean result = true;

		try {
			ArrayList<DictionaryItem> newDicList = new ArrayList<DictionaryItem>();
			ArrayList<DictionaryItem> temp;

			URL xmlURL = new URL(mRes.getString(R.string.install_dic_list_url));

			// Parse dictionary info
			result = parseDicInfo(newDicList, xmlURL);

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
						DatabaseHelper.safeGetDatabaseHelper(mContext)
								.saveDicInfos(mDicList);
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
			result = false;
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}

		Log.v(KeyboardApp.LOG_TAG, "finished loading dictionary list");

		return result;
	}


	/**
	 * Returns if checking update is required
	 * 
	 * @return
	 */
	public boolean isNeedCheckingUpdate() {
		long curtime = Utils.getTimeMilis();
		
		boolean afterUpdate = (getDicUpdatedTime() + CHECK_AFTER_UPDATE) < curtime;
		boolean afterLastCheck = (getDicCheckTime() + CHECK_LAST < curtime);
		boolean isNeedAppUpgradeOrDictUpdate = isNeedUpgrade() || isNeedUpdate();

		Log.v(KeyboardApp.LOG_TAG, "isNeedCheckingUpdate(): afterUpdate " + afterUpdate + "; afterLastCheck " + afterLastCheck + "; isNeedAppUpgradeOrDictUpdate "  + isNeedAppUpgradeOrDictUpdate );

		return isNeedAppUpgradeOrDictUpdate ? false : (afterUpdate ? afterLastCheck : false ) ;
	}

	/**
	 * Notify diction list changed
	 * 
	 * @param newDicList
	 * @param oldDicList
	 */
	private boolean markAndNotifyUpdatedState(
			ArrayList<DictionaryItem> newDicList,
			ArrayList<DictionaryItem> oldDicList) {
		Assert.assertTrue(newDicList != null);
		Assert.assertTrue(oldDicList != null);

		isNeedUpdate = false;
		if (mDicUpdatedListener != null) {
			ArrayList<DictionaryItem> needUpdateList = new ArrayList<DictionaryItem>();
			// Compare 2 dictionary info
			DictionaryItem newItem = null, oldItem = null;
			int i = 0;
			for (i = 0; i < newDicList.size(); i++) {
				newItem = newDicList.get(i);
				oldItem = getDictionaryItemPrim(oldDicList, newItem.lang);
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
				isNeedUpdate = true;

				// Send updated event
				mDicUpdatedListener.onDictionaryUpdated(needUpdateList);
				for (i = 0; i < needUpdateList.size(); i++) {
					mDicUpdatedListener.onDictionaryItemUpdated(needUpdateList.get(i));
				}
			} else {
				Log.v(KeyboardApp.LOG_TAG,
						"There is no need to update dictionaries");
			}
		}

		return isNeedUpdate;
	}

	/**
	 * Parse diction info from xml string
	 * 
	 * @param
	 * @return
	 */
	boolean parseDicInfo(ArrayList<DictionaryItem> dicList, URL url) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			int index = 0;

			doc.getDocumentElement().normalize();

			// Clear dictionary list
			dicList.clear();

			NodeList nodeList = doc.getElementsByTagName(mRes.getString(R.string.xml_entry_dictionaries));

			Element root = (Element) nodeList.item(0);

			nodeList = root.getChildNodes();

			int minAppVersionCode = 0;

			try {
				minAppVersionCode = Integer.parseInt(root.getAttribute(mRes.getString(R.string.xml_dictionaries_property_min_version_code)));
			} catch (Exception e) {
				Log.e(KeyboardApp.LOG_TAG, "couldn't detect min version code to use up-to-date dictionaries", e);
			}

			setNeedUpdrade(minAppVersionCode);
			
			Node node;
			DictionaryItem newItem;
			for (int i = 0; i < nodeList.getLength(); i++) {
				node = nodeList.item(i);

				if (node.getChildNodes().getLength() > 0) {
					newItem = new DictionaryItem(mContext);
					if (newItem.parseDicInfo(node, index++))
						dicList.add(newItem);
				}
			}
		} catch (Exception e) {
			Log.e(KeyboardApp.LOG_TAG, "parse dic info", e);
			return false;
		}

		return true;
	}

	// Load updated time
	public long getDicUpdatedTime() {
		// Set update time into preference value
		Assert.assertTrue(mContext != null);
		SharedPreferences preference = mContext.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		long updatedTime = preference.getLong(
				mRes.getString(R.string.dic_updated_time), 0);

		return updatedTime;
	}

	// Save current wallpaper drawable id
	public void saveDicUpdatedTime(long updatedTime) {
		Assert.assertTrue(mContext != null);
		SharedPreferences.Editor preferenceEditor = mContext
				.getSharedPreferences(Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE).edit();

		preferenceEditor.putLong(mRes.getString(R.string.dic_updated_time),
				updatedTime);

		preferenceEditor.commit();
	}

	// Save current wallpaper drawable id
	public void saveDicCheckTime(long updatedTime) {
		Assert.assertTrue(mContext != null);
		SharedPreferences.Editor preferenceEditor = mContext
				.getSharedPreferences(Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE).edit();

		preferenceEditor.putLong(mRes.getString(R.string.dic_checked_time),
				updatedTime);

		preferenceEditor.commit();
	}

	// Load checked time
	public long getDicCheckTime() {
		// Set update time into preference value
		Assert.assertTrue(mContext != null);
		SharedPreferences preference = mContext.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		long checkedTime = preference.getLong(
				mRes.getString(R.string.dic_checked_time), 0);

		return checkedTime;
	}

	/**
	 * Retrieve current dictionary list
	 * 
	 * @return
	 */
	public ArrayList<DictionaryItem> getDictionaryList() {
		ArrayList<DictionaryItem> newDicList;

		lock.lock();
		try {
			newDicList = new ArrayList<DictionaryItem>(mDicList);
		} finally {
			lock.unlock();
		}

		return newDicList;
	}

	/**
	 * Retrieve dictionary item by dictionary name
	 * 
	 * @param dicName
	 * @return
	 */
	public DictionaryItem getDictionaryItem(String dicName) {
		DictionaryItem item;

		lock.lock();
		try {
			item = getDictionaryItemPrim(mDicList, dicName);
		} finally {
			lock.unlock();
		}

		return item;
	}

	private DictionaryItem getDictionaryItemPrim(
			ArrayList<DictionaryItem> list, String dicName) {
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
	 * 
	 * @param listener
	 */
	public void setOnDictionaryUpdatedListener(OnDictionaryUpdatedListener listener) {
		mDicUpdatedListener = listener;
	}

	public void markAsReadAll() {
		isNeedUpdate = false;
		DictionaryItem item;
		
		for (int i = 0; i < mDicList.size(); i++) {
			item = mDicList.get(i);

			item.isNeedUpdate = false;
		}

		DatabaseHelper.safeGetDatabaseHelper(mContext).saveDicInfos(mDicList);
	}

	/**
	 * Specified dictionary exist or not
	 * 
	 * @param context
	 * @param langID
	 * @return
	 */
	public boolean isDictionaryExist(Context context, String langID) {
		if(langID.equals(context.getResources().getString(R.string.lang_code_other))){
			return true;
		}		
		
		DictionaryItem mDicItem = getDictionaryItem(langID);

		return isDictionaryExist(context, mDicItem);
	}

	public static boolean isDictionaryExist(Context context, DictionaryItem item) {
		if (item == null) {
			return false;
		}
		
		ArrayList<DictionaryFileItem> fileItems = item.fileItems;
		DictionaryFileItem fileItem;

		// Check dictionary existing
		for (int i = 0; i < fileItems.size(); i++) {
			fileItem = fileItems.get(i);
			String folder = "databases";
			if(fileItem.filename.contains("2.idx") || fileItem.filename.contains("1.dic") || fileItem.filename.contains("2.dic"))
				folder = "files"; // Old dictionary location
			File file = new File(Utils.getInternalFilePath(context, folder + "/" + fileItem.filename));
			if (!file.exists()){
				Log.v(KeyboardApp.LOG_TAG, "!file.exists() "  + file.getAbsolutePath());
				return false;
			}

			if (file.length() < fileItem.size){
				// File can be bigger but not smaller.
				Log.v(KeyboardApp.LOG_TAG, "file.length() < fileItem.size");
				return false;
			}
		}

		return true;
	}

	/**
	 * Retrieve dictionary version info
	 * 
	 * @return
	 */
	public List<String> getInstalledDictionaryNames() {
		List<String> installed = new ArrayList<String>();
		
		try {
			if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
				DictionaryItem item;
				String lang;

				for (int i = 0; i < mDicList.size(); i++) {
					item = mDicList.get(i);

					if (!item.isNeedUpdate) {
						lang = Settings.getNameFromValue(mContext, "language", item.lang);
						installed.add(String.format("%s (v%d)", lang, (int) item.version));
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}

		return installed;
	}
	
	

	/**
	 *  It returns true if there is a new dictionary available, otherwise false
	 *  
	 * @return
	 */
	public boolean isNeedUpdate() {
		return getUpdatedStatus() ;
	} 
	
	
	
	public boolean checkNeedUpdate(Context context){
		// TODO: Move this to a method in DatabaseHelper
		Cursor c = DatabaseHelper.safeGetDatabaseHelper(context).mDB.query(DatabaseHelper.DIC_LANGUAGES_TABLE_NAME, null, DatabaseHelper.DIC_LANGUAGES_FIELD_IS_NEED_UPDATE + " = 1 AND " + DatabaseHelper.DIC_LANGUAGES_FIELD_IS_INSTALLED + " = 1", null, null, null, null);
		int count = c.getCount();
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
	 * 
	 * @return
	 */
	public boolean isNeedUpgrade(){
		SharedPreferences preference = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		int requiredVersionCode = preference.getInt(mContext.getString(R.string.dic_app_version_code), 0);

		try {
			String pkg = mContext.getPackageName();
	        int mVersionNumber = mContext.getPackageManager().getPackageInfo(pkg, 0).versionCode;
	        return requiredVersionCode > mVersionNumber;
	    } catch (NameNotFoundException e) {
	    	Log.e(KeyboardApp.LOG_TAG, "error", e);
	    }
		
		return false;
	}
	
	
	
	/**
	 * Dictionary has minimum application version code to up-to-date 
	 * 
	 * @param version
	 */
	public void setNeedUpdrade(int version){
		SharedPreferences.Editor preferenceEditor = mContext.getSharedPreferences(Settings.SETTINGS_FILE,
						Context.MODE_PRIVATE).edit();

		preferenceEditor.putInt(mContext.getString(R.string.dic_app_version_code),
				version);

		preferenceEditor.commit();
		
		// notify service
		if(KeyboardService.getIME() != null){
			boolean isNeedUpgrade = isNeedUpgrade();
			KeyboardService.getIME().setNeedUpgradeApp(isNeedUpgrade);
			if(isNeedUpgrade)
				KeyboardService.getIME().showSuggestionAppUpdateOnUi();
		}
				
	}
	
	
	
	public boolean isDicDownloaded() {
		String currLang = LanguageSelector.getLanguagePreference(mContext);

		if (currLang.equals(mContext.getResources().getString(R.string.lang_code_other))) {
			// "Other" language doesn't have a dictionary
			return true;
		}

		DictionaryItem dicItem = getDictionaryItem(currLang);
		if (dicItem != null) {// && !dicItem.isMarkedAsUnread) {
			return true;
		}
		
		return false;
	}
	
	
	
	public boolean getUpdatedStatus() {
		// Get preferences
		SharedPreferences preference = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);

		// Check language
        String currLang = preference.getString("language", mContext.getResources().getString(R.string.lang_code_default));
        
		if (currLang.equals(mContext.getResources().getString(R.string.lang_code_other))) {
			// "Other" language doesn't have a dictionary
			return false;
		}
        
		boolean prefix = preference.getBoolean(mContext.getResources().getString(R.string.prefs_is_dic_updated), 
				false);
		
		
		return prefix;
	}

	
	
	public void setUpdatedStatus(boolean status) {
		
		SharedPreferences.Editor preferenceEditor = mContext.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putBoolean(mContext.getResources().getString(R.string.prefs_is_dic_updated), status);
		
		preferenceEditor.commit();
	}
}