/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Assert;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.dictionary.ShortcutDictionary;
import com.comet.keyboard.dictionary.updater.DictionaryFileItem;
import com.comet.keyboard.dictionary.updater.DictionaryItem;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.settings.LanguageProfile;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.settings.ShortcutData;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	// Database Manager
	private static DatabaseHelper dbHelper;
	
	private Context mContext;

	// Defines database name and table names
	private static final String DB_NAME = "keyboard.db";
	
	// "shortcuts" table
	private static final String SHORTCUTS_TABLE_NAME = "shortcuts";
	private static final String SHORTCUTS_FIELD_KEYSTROKES = "keystrokes";
	private static final String SHORTCUTS_FIELD_EXPANSION = "expansion";
	
	// "dic_languages" table
	public static final String DIC_LANGUAGES_TABLE_NAME = "dic_languages";
	public static final String DIC_LANGUAGES_FIELD_ID = "id";
	public static final String DIC_LANGUAGES_FIELD_IS_NEED_UPDATE = "is_need_update";
	public static final String DIC_LANGUAGES_FIELD_IS_INSTALLED = "is_installed";
	public static final String DIC_LANGUAGES_FIELD_VERSION = "version";
	public static final String DIC_LANGUAGES_FIELD_LANG = "lang";
	
	// "dic_items" table
	private static final String DIC_ITEM_TABLE_NAME = "dic_items";
	private static final String DIC_ITEM_FIELD_LANGUAGE_ID = "language_id";
	private static final String DIC_ITEM_FIELD_NAME = "name";
	private static final String DIC_ITEM_FIELD_SIZE = "size";
	
	/// "profile" table
	private static final String PROFILE_TABLE_NAME = "profile";
	private static final String PROFILE_FIELD_LANG = "lang";
	private static final String PROFILE_FIELD_KEYBOARD = "keyboard";

	private static final int DB_VERSION = 7;

	// Defines db error
	public enum DBError {
		DB_ERROR_NONE, DB_ERROR_FAILED, DB_ERROR_ALREADY_EXIST, DB_ERROR_NOT_EXIST,
	};

	public SQLiteDatabase mDB;

	public DatabaseHelper(Context context) {
		super(context,DB_NAME, null, DB_VERSION);
		mContext = context;
		if(mDB == null || !mDB.isOpen())
			mDB = getWritableDatabase();
	}
	
	
	
	
	public static DatabaseHelper safeGetDatabaseHelper(Context context) {
		safeGetDatabaseHelper(context, false);
		
		return dbHelper;
	}
	
	public static DatabaseHelper safeGetDatabaseHelper(Context context,
			boolean force) {
		
		if (force && dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
		
		if (dbHelper == null)
			dbHelper = new DatabaseHelper(context);
		return dbHelper;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			Log.d(KeyboardApp.LOG_TAG, "DatabaseHelper.onCreate()...");
//			android.os.Debug.waitForDebugger();
			
			// Create shortcut table
			db.execSQL("CREATE TABLE IF NOT EXISTS "
					+ SHORTCUTS_TABLE_NAME
					+ "(keystrokes TEXT, expansion TEXT)");
			// Create dic_languages table
			createDicLanguagesTable(db);
			// Create dic_items table
			createDicItemsTable(db);
			// Create profile table
			createProfileTable(db);
			
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return;
		}
	}
	
	private void createDicLanguagesTable(SQLiteDatabase db) throws SQLiteException {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + DIC_LANGUAGES_TABLE_NAME
				+ "(" + DIC_LANGUAGES_FIELD_ID
				+ " INTEGER PRIMARY KEY, " + DIC_LANGUAGES_FIELD_VERSION
				+ " FLOAT, " + DIC_LANGUAGES_FIELD_LANG + " TEXT,"
				+ DIC_LANGUAGES_FIELD_IS_NEED_UPDATE + " INTEGER, " + DIC_LANGUAGES_FIELD_IS_INSTALLED + " INTEGER " + ")");	
	}


	private void createDicItemsTable(SQLiteDatabase db) throws SQLiteException {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + DIC_ITEM_TABLE_NAME
				+ "(" + DIC_ITEM_FIELD_LANGUAGE_ID
				+ " INTEGER, " + DIC_ITEM_FIELD_NAME + " TEXT, "
				+ DIC_ITEM_FIELD_SIZE + " INTEGER)");		
	}

	private void createProfileTable(SQLiteDatabase db) throws SQLiteException {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + PROFILE_TABLE_NAME + "("
				+ PROFILE_FIELD_LANG + " TEXT, " + PROFILE_FIELD_KEYBOARD
				+ " TEXT)");
	}

	
//	private void importLexicon(SQLiteDatabase db) throws SQLiteException {
//		// TODO: Import the custom dictionary to the lexicon table
//	}

	
	
	
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		mDB = db;
		
		Log.d(KeyboardApp.LOG_TAG, "DatabaseHelper.onUpgrade() old version " + oldVersion + "; new version " + newVersion );

		if(oldVersion < 7) {
			upgradeProfiles(db);
			
			// Recreate dictionary tables from scratch
			db.execSQL("DROP TABLE IF EXISTS dic_categories");
			db.execSQL("DROP TABLE IF EXISTS dic_languages");
			db.execSQL("DROP TABLE dic_items");
			createDicItemsTable(db);
			createDicLanguagesTable(db);
		}
	}
	


	// Upgrade database to v4
	@Deprecated
    public void upgradeProfiles(SQLiteDatabase db) {
    	// Create a new set of profiles
    	HashMap<String, LanguageProfile> newProfileMap = new HashMap<String, LanguageProfile>();

    	// Step 1: Create a default profile for every downloaded language
		ArrayList<String> dictionaries = getLanguagesForUpgrade(db, "dic_categories");
		if(dictionaries.size() == 0)
			dictionaries = getLanguagesForUpgrade(db, "dic_languages");
		
    	for(String dictionary : dictionaries) {
			LanguageProfile newItem = LanguageProfileManager.createDefaultProfile(mContext, dictionary);
			newProfileMap.put(newItem.getLang(), newItem);
    	}


    	// Step 2: Save the current language and keyboard as profile
		SharedPreferences sharedPrefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		String lang = sharedPrefs.getString("language", mContext.getResources().getString(R.string.lang_code_default));
		String keyboard = KeyboardLayout.getCurrentLayout().getId();
    	LanguageProfile currProfile = new LanguageProfile(lang, keyboard);
    	newProfileMap.put(lang, currProfile);

    	
    	// Step 3: Load existing profiles from db
    	ArrayList<LanguageProfile> profileList = getProfileItems();
    	for (LanguageProfile profile : profileList) {
   			newProfileMap.put(profile.getLang(), profile);
    	}
    	
    	
    	// Drop and re-create the table
    	db.execSQL("DROP TABLE " + PROFILE_TABLE_NAME);
    	// Create new table
    	createProfileTable(db);

    	
    	// Save the profiles back to the db
    	if (newProfileMap.size() > 0) {
	    	Object[] newProfileList = newProfileMap.values().toArray();
	    	for (Object object : newProfileList) {
	    		LanguageProfile profile = (LanguageProfile) object;	    		
	    		addProfileItem(profile.getLang(), profile.getKeyboard());
	    		Log.d(KeyboardApp.LOG_TAG, "upgradeProfiles(" + profile.getLang() + "," + profile.getKeyboard() + ")");
	    	}
    	}
    }
    
    
    
    @Deprecated
    private ArrayList<String> getLanguagesForUpgrade(SQLiteDatabase db, String table) {
    	ArrayList<String> dictionaries = new ArrayList<String>();

    	try {
    		Cursor cursor = db.query(table,
    				new String[]{DIC_LANGUAGES_FIELD_LANG},
    				"mark_as_unread=0", null, null, null, null);

    		while(cursor.moveToNext() == true) {
    			String dictionary = cursor.getString(0);
    			dictionaries.add(dictionary);
    		}

    		// Close query
    		cursor.close();

    	} catch (SQLiteException e) {
    		Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
    	}

    	return dictionaries;
    }



	@Override
	public synchronized void close() {
		if(mDB != null)
			mDB.close();

		super.close();
	}


	/********************************************************************
	 * Shortcuts methods
	 ********************************************************************/
	/**
	 * Retrieve shortcut items from database
	 * 
	 * @param list
	 * @return
	 */
	public DBError getShortcutItems(ArrayList<ShortcutData> list) {
		try {
			String columns[] = { SHORTCUTS_FIELD_KEYSTROKES,
					SHORTCUTS_FIELD_EXPANSION };
			Cursor cursor = mDB.query(SHORTCUTS_TABLE_NAME, columns, null,
					null, null, null, null);
			String keystrokes, expansion;
			ShortcutData newData;

			if (cursor == null)
				return DBError.DB_ERROR_FAILED;

			// Clear all items
			list.clear();

			if (cursor.moveToFirst()){
				do{
					keystrokes = cursor.getString(0);
					expansion = cursor.getString(1);

					newData = new ShortcutData(keystrokes, expansion);
					list.add(newData);
				} while(cursor.moveToNext());
			}

			cursor.close();
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Retrieve shortcut item from database
	 * 
	 * @param keystroke
	 * @return
	 */
	public DBError getShortcutItem(ShortcutData shortcut) {
		try {
			Cursor cursor = mDB.query(SHORTCUTS_TABLE_NAME, new String[] { SHORTCUTS_FIELD_EXPANSION },
					SHORTCUTS_FIELD_KEYSTROKES + "=?",
					new String[] { shortcut.mKeystroke }, null, null, null,
					null);

			if (cursor == null)
				return DBError.DB_ERROR_FAILED;

			boolean isExist = false;
			if (cursor.moveToFirst()){
				shortcut.mExpand = cursor.getString(0);
				isExist = true;
			}
			cursor.close();

			if (!isExist)
				return DBError.DB_ERROR_NOT_EXIST;

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Add one shortcut item
	 * 
	 * @param newShortcut
	 * @return
	 */
	public DBError addShortcutItem(ShortcutData newShortcut) {	
		Assert.assertTrue(newShortcut != null);

		try {
			// Check existing for the specified shortcut
			if (getShortcutItem(newShortcut) == DBError.DB_ERROR_NONE)
				return DBError.DB_ERROR_ALREADY_EXIST;

			// Append new shortcut item to database 
			ContentValues values = new ContentValues();
			values.put(SHORTCUTS_FIELD_KEYSTROKES, newShortcut.mKeystroke);
			values.put(SHORTCUTS_FIELD_EXPANSION, newShortcut.mExpand);

			long result = mDB.insert(SHORTCUTS_TABLE_NAME, null, values);
			if (result == -1)
				return DBError.DB_ERROR_FAILED;
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		// Force dictionary to reload from DB
		ShortcutDictionary.loadFromDatabase(mContext);

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Update specified shortcut item
	 * 
	 * @param newShortcut
	 * @return
	 */
	public DBError updateShortcutItem(ShortcutData orgShortcut, 
			ShortcutData updatedShortcut) {		
		Assert.assertTrue(updatedShortcut != null);

		try {
			// Check existing for the specified shortcut
			if (getShortcutItem(orgShortcut) != DBError.DB_ERROR_NONE)
				return DBError.DB_ERROR_ALREADY_EXIST;

			// Append new shortcut item to database 
			ContentValues values = new ContentValues();
			values.put(SHORTCUTS_FIELD_KEYSTROKES, updatedShortcut.mKeystroke);
			values.put(SHORTCUTS_FIELD_EXPANSION, updatedShortcut.mExpand);

			long result = mDB.update(SHORTCUTS_TABLE_NAME, values,
					SHORTCUTS_FIELD_KEYSTROKES + "=?",
					new String[] {orgShortcut.mKeystroke});
			if (result == -1)
				return DBError.DB_ERROR_FAILED;
			else if (result == 0)
				return DBError.DB_ERROR_NOT_EXIST;

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		// Force dictionary to reload from DB
		ShortcutDictionary.loadFromDatabase(mContext);

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Remove specified shortcut item
	 * 
	 * @param shortcut
	 * @return
	 */
	public DBError removeShortcutItems(ShortcutData shortcut) {
		try {
			int result = mDB.delete(SHORTCUTS_TABLE_NAME, 
					SHORTCUTS_FIELD_KEYSTROKES + "=?" + " and "
							+ SHORTCUTS_FIELD_EXPANSION + "=?", new String[] {
							shortcut.mKeystroke, shortcut.mExpand });

			if (result == -1)
				return DBError.DB_ERROR_FAILED;
			else if (result == 0)
				return DBError.DB_ERROR_NOT_EXIST;

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		// Force dictionary to reload from DB
		ShortcutDictionary.loadFromDatabase(mContext);

		return DBError.DB_ERROR_NONE;
	}

	
	/********************************************************************
	 * Dictionary methods
	 ********************************************************************/
	/**
	 * Load diction item from "dic_items" table
	 */
	public synchronized DBError loadDicItems(Context context,
			ArrayList<DictionaryFileItem> dicFileItems, int categoryID) {
		return loadDicItems(context, mDB, dicFileItems, categoryID);
	}
	
	public synchronized DBError loadDicItems(Context context,
			SQLiteDatabase db, ArrayList<DictionaryFileItem> dicFileItems,
			long categoryID) {
		// clear all dictionary item list
		Assert.assertTrue(dicFileItems != null);
		dicFileItems.clear();
		
		try {
			
			Cursor cursor = db.query(DIC_ITEM_TABLE_NAME, new String[] {DIC_ITEM_FIELD_NAME, DIC_ITEM_FIELD_SIZE},
					DIC_ITEM_FIELD_LANGUAGE_ID + "=" + categoryID, null, null,
					null, null, null);
			
			if (cursor == null)
				return DBError.DB_ERROR_FAILED;

			DictionaryFileItem newItem = new DictionaryFileItem(context);
			while(cursor.moveToNext() == true) {
				newItem.filename = cursor.getString(0);
				newItem.size = Long.parseLong(cursor.getString(1));
				dicFileItems.add(newItem);
			}

			// Add new dictionary item
			
			// Close query
			cursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}
		
		return DBError.DB_ERROR_NONE;
	}
	
	/**
	 * Load dictionary info from "dic_languages", "dic_items" table
	 * 
	 * @param dic
	 */
	public synchronized DBError loadDicInfo(Context context,
			ArrayList<DictionaryItem> dicList) {
		return loadDicInfo(context, mDB, dicList);
	}
	
	/**
	 * Load dictionary info from "dic_languages", "dic_items" table
	 * 
	 * @param dic
	 */
	private synchronized DBError loadDicInfo(Context context,
			ArrayList<DictionaryItem> dicList, String sqlWhere, String[] selectionArgs) {
		return loadDicInfo(context, mDB, dicList, sqlWhere, selectionArgs);
	}
	
	
	/**
	 * Gets dictionaries collection which needs to be updated. 
	 * 
	 * @param context
	 * @param dicList
	 * @return
	 */
	public DBError getDictionariesForUpdate(Context context, ArrayList<DictionaryItem> dicList) {
		return loadDicInfo(context, dicList,
				DatabaseHelper.DIC_LANGUAGES_FIELD_IS_NEED_UPDATE + " = ? AND " + DatabaseHelper.DIC_LANGUAGES_FIELD_IS_INSTALLED + " = ?" ,
				new String[] { "1", "1" });		
	}	
	

	public synchronized DBError loadDicInfo(Context context, SQLiteDatabase db,
			ArrayList<DictionaryItem> dicList, String sqlWhere, String[] selectionArgs) {
		// clear all dictionary list
		Assert.assertTrue(dicList != null);
		dicList.clear();
		
		try {
			DBError error;
			
			Cursor dicListCursor = db.query(DIC_LANGUAGES_TABLE_NAME, new String[]{DIC_LANGUAGES_FIELD_ID, DIC_LANGUAGES_FIELD_VERSION,DIC_LANGUAGES_FIELD_LANG,DIC_LANGUAGES_FIELD_IS_NEED_UPDATE, DIC_LANGUAGES_FIELD_IS_INSTALLED},
					sqlWhere, selectionArgs, null, null, null);
			if (dicListCursor == null)
				return DBError.DB_ERROR_FAILED;

			while(dicListCursor.moveToNext() == true) {
				DictionaryItem newItem = new DictionaryItem(context);
				
				newItem.id = dicListCursor.getInt(0);
				newItem.version = Integer.parseInt(dicListCursor.getString(1));
				newItem.lang = dicListCursor.getString(2);
				newItem.isNeedUpdate = (dicListCursor.getInt(3) == 1);
				newItem.isInstalled = (dicListCursor.getInt(4) == 1);
				
				error = loadDicItems(context, db, newItem.fileItems,
						newItem.id);
				if (error == DBError.DB_ERROR_NONE) {
					// Add one dictionary category
					dicList.add(newItem);
				}
			}

			// Close query
			dicListCursor.close();

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}

	public synchronized DBError loadDicInfo(Context context, SQLiteDatabase db,
			ArrayList<DictionaryItem> dicList) {
		return loadDicInfo(context, db, dicList, null, null);
	}

	/**
	 * Save dictionary item to "dic_item" table
	 */
	public synchronized DBError saveDicItem(long categoryId,
			DictionaryFileItem fileItem, boolean isNeedUpdate) {
		Assert.assertTrue(categoryId >= 0);
		Assert.assertTrue(fileItem != null);
		
		try {
			if (isNeedUpdate) {
				// Clear dic_cartegories, dic_items table
				if (mDB.delete(DIC_ITEM_TABLE_NAME, DIC_ITEM_FIELD_LANGUAGE_ID
						+ "=" + String.valueOf(categoryId) + " AND "
						+ DIC_ITEM_FIELD_NAME + "=" + fileItem.filename, null) == -1)
					return DBError.DB_ERROR_FAILED;
			}
			
			// Save dictionary item
			ContentValues values = new ContentValues();
			values.put(DIC_ITEM_FIELD_LANGUAGE_ID, categoryId);
			values.put(DIC_ITEM_FIELD_NAME, fileItem.filename);
			values.put(DIC_ITEM_FIELD_SIZE, (int)fileItem.size);
			if(mDB.insert(DIC_ITEM_TABLE_NAME, null, values) == -1) {
				Log.e(KeyboardApp.LOG_TAG, fileItem.toString());
				return DBError.DB_ERROR_FAILED; 
			}
			
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}
	
	/**
	 * Save dictionary items to "dic_items" table
	 */
	public synchronized DBError saveDicItems(long oldCategoryId, long categoryId,
			ArrayList<DictionaryFileItem> dicItemList) {
		Assert.assertTrue(dicItemList != null);
		
		DBError error;
		try {
			if(oldCategoryId != 0) {
			// Clear dic_cartegories, dic_items table
			if (mDB.delete(DIC_ITEM_TABLE_NAME, DIC_ITEM_FIELD_LANGUAGE_ID
						+ "=?", new String[] { String.valueOf(oldCategoryId) }) == -1)
				return DBError.DB_ERROR_FAILED;
			}
			
			DictionaryFileItem item;
			for (int i = 0 ; i < dicItemList.size() ; i++) {
				item = dicItemList.get(i);
				error = saveDicItem(categoryId, item, false);
				if (error == DBError.DB_ERROR_FAILED)
					continue;
			}
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}
	
	/**
	 * Save dictionary info
	 */
	public synchronized DBError saveDicItem(DictionaryItem dicItem,
			boolean isNeedDelete) {
		Assert.assertTrue(dicItem != null);
		
		DBError error;
		
		try {
			long oldId = 0;
			
			// Clear dic_cartegories, dic_items table
			if (isNeedDelete) {
				
				Cursor c = mDB.query(DIC_LANGUAGES_TABLE_NAME, new String[] { DIC_LANGUAGES_FIELD_ID }, DIC_LANGUAGES_FIELD_LANG
						+ "=?", new String[] { dicItem.lang }, null, null, null);
				
				int count = c.getCount();
				
				if(count > 0){
					c.moveToFirst();
					oldId = c.getLong(0);
					
				if (mDB.delete(DIC_LANGUAGES_TABLE_NAME, DIC_LANGUAGES_FIELD_LANG
						+ "=?", new String[] { dicItem.lang }) == -1)
					return DBError.DB_ERROR_FAILED;
			}
			}

			// Save category info
			ContentValues values = new ContentValues();
			values.put(DIC_LANGUAGES_FIELD_VERSION, dicItem.version);
			values.put(DIC_LANGUAGES_FIELD_LANG, dicItem.lang);
			values.put(DIC_LANGUAGES_FIELD_IS_NEED_UPDATE,
					dicItem.isNeedUpdate);
			values.put(DIC_LANGUAGES_FIELD_IS_INSTALLED,
					dicItem.isInstalled);			
			if ((dicItem.id = mDB.insert(DIC_LANGUAGES_TABLE_NAME, null, values)) == -1) {
				Log.e(KeyboardApp.LOG_TAG, dicItem.toString());
				return DBError.DB_ERROR_FAILED;
			}
				
			error = saveDicItems(oldId, dicItem.id, dicItem.fileItems);
			if (error == DBError.DB_ERROR_FAILED)
				return DBError.DB_ERROR_FAILED;
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}
	
	/**
	 * Save dictionary info to "dic_cartegories", "dic_items" table
	 */
	public synchronized DBError saveDicInfos(ArrayList<DictionaryItem> dicList) {
		Assert.assertTrue(dicList != null);
		
		try {
			// Clear dic_cartegories, dic_items table
			if (mDB.delete(DIC_LANGUAGES_TABLE_NAME, null, null) == -1)
				return DBError.DB_ERROR_FAILED;
			
			if (mDB.delete(DIC_ITEM_TABLE_NAME, null, null) == -1)
				return DBError.DB_ERROR_FAILED;			
			
			DictionaryItem item;
			for (int i = 0 ; i < dicList.size() ; i++) {
				item = dicList.get(i);
				// Save category info
				saveDicItem(item, false);
			}
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}
	
	/***********************************************************************************
	 * Profile function
	 **********************************************************************************/
	public ArrayList<LanguageProfile> getProfileItems() {
		ArrayList<LanguageProfile> profiles = new ArrayList<LanguageProfile>(); 
		try {
			Cursor mCursor = mDB.query(PROFILE_TABLE_NAME, new String[] {
					PROFILE_FIELD_LANG, PROFILE_FIELD_KEYBOARD }, null, null,
					null, null, PROFILE_FIELD_LANG);
			LanguageProfile item;

			if (mCursor == null)
				return profiles;

			if (mCursor.moveToFirst()){
				do{
					String lang = mCursor.getString(0);
					String keyboard = mCursor.getString(1);
					item = new LanguageProfile(lang, keyboard);
					profiles.add(item);
				} while(mCursor.moveToNext());
			}

			mCursor.close();
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return profiles;
		}

		return profiles;
	}

	/**
	 * Retrieve profile item from database
	 * 
	 * @param item
	 * @return
	 */
	public LanguageProfile getProfileItem(String lang) {
		LanguageProfile profile = null;
		try {
			Cursor mCursor = mDB.query(PROFILE_TABLE_NAME,
					new String[] { PROFILE_FIELD_KEYBOARD }, PROFILE_FIELD_LANG
							+ "=?", new String[] { lang }, null, null, null,
					null);

			if (mCursor == null)
				return null;

			boolean isExist = false;
			if (mCursor.moveToFirst()){
				String keyboard = mCursor.getString(0);
				profile = new LanguageProfile(lang, keyboard);
				isExist = true;
			}
			mCursor.close();

			if (!isExist)
				return null;

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return null;
		}
		
		return profile;
	}

	/**
	 * Add new profile item
	 * 
	 * @param newItem
	 * @return
	 */
	public DBError addProfileItem(String lang, String keyboard) {	
		Assert.assertTrue(lang != null);
		Assert.assertTrue(keyboard != null);

		try {
			// Check existing for the specified shortcut
			if (getProfileItem(lang) != null)
				return updateProfileItem(lang, keyboard);

			// Append new shortcut item to database 
			ContentValues values = new ContentValues();
			values.put(PROFILE_FIELD_LANG, lang);
			values.put(PROFILE_FIELD_KEYBOARD, keyboard);

			long result = mDB.insert(PROFILE_TABLE_NAME, null, values);
			if (result == -1)
				return DBError.DB_ERROR_FAILED;
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Update specified profile item
	 * 
	 * @param orgProfile
	 * @param newProfile
	 * @return
	 */
	public DBError updateProfileItem(String lang, String keyboard) {		
		Assert.assertTrue(lang != null);
		Assert.assertTrue(keyboard != null);

		try {
			// Check existing for the specified profile
			if (getProfileItem(lang) == null)
				return addProfileItem(lang, keyboard);

			// Append new profile item to database 
			ContentValues values = new ContentValues();
			values.put(PROFILE_FIELD_LANG, lang);
			values.put(PROFILE_FIELD_KEYBOARD, keyboard);

			long result = mDB.update(PROFILE_TABLE_NAME, values,
					PROFILE_FIELD_LANG + "=?", new String[] { lang });
			if (result == -1)
				return DBError.DB_ERROR_FAILED;
			else if (result == 0)
				return DBError.DB_ERROR_NOT_EXIST;

		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
			return DBError.DB_ERROR_FAILED;
		}

		return DBError.DB_ERROR_NONE;
	}

	/**
	 * Remove specified profile item
	 * 
	 * @param profile
	 * @return
	 */
	public void removeProfileItem(String lang) {
		try {
			mDB.delete(PROFILE_TABLE_NAME, PROFILE_FIELD_LANG + "=?",
					new String[] {lang});
		} catch (SQLiteException e) {
			Log.e(KeyboardApp.LOG_TAG, e.getMessage(), e);
		}
	}
}