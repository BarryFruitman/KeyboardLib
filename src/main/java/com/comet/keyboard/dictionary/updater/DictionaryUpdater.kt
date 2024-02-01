/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard.dictionary.updater

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Handler
import android.util.Log
import com.comet.keyboard.KeyboardApp
import com.comet.keyboard.KeyboardService
import com.comet.keyboard.R
import com.comet.data.api.DictionaryItem
import com.comet.data.api.DictionaryService
import com.comet.keyboard.settings.Settings
import com.comet.data.db.DatabaseHelper
import com.comet.keyboard.util.Utils
import junit.framework.Assert
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class DictionaryUpdater(private val mContext: Context) {
    private val service = DictionaryService.create()
    private val mRes: Resources

    // Current dictionary list
    private var mDicList: List<DictionaryItem>
    private var mDicUpdatedListener: OnDictionaryUpdatedListener? = null

    // Is marked as unread for the updating info
    private var mIsNeedUpdate = false
    private val lock = ReentrantLock()

    init {
        mRes = mContext.resources
        if (mUIHandler == null) {
            mUIHandler = Handler()
        }
        mDicList = mutableListOf()

        // load dictionary list from database
        refreshDictionaryListFromDb()
    }

    fun refreshDictionaryListFromDb() {
//        try {
//            lock.lock()
//            DatabaseHelper
//                .safeGetDatabaseHelper(mContext)
//                .loadDicInfo(mContext, mDicList)
//        } finally {
//            lock.unlock()
//        }
    }

    fun stopUpdate() {
        // TODO: Implement this or remove it.
    }

    /**
     * Load dictionary list from Internet
     */
    @Synchronized
    fun loadDictionaryList() {
        Log.v(KeyboardApp.LOG_TAG, "started loading dictionary list")
        try {
            val call = service.getDictionaries()
            val response = call.execute()
            val index = response.body() ?: return

            Log.e("RETROFIT_DEBUG", "$index")

            // Save into database
            if (index.dictionaries.isNotEmpty()) {
                lock.lock()
                try {
                    // Notify dictionary info changed
                    mDicList = index.dictionaries
//                    if (markAndNotifyUpdatedState(mDicList, newDicList)) {
//                        // Save new diction list into database
//                        DatabaseHelper.safeGetDatabaseHelper(mContext).saveDicInfos(mDicList)
//                        saveDicUpdatedTime(Utils.getTimeMilis())
//                    }
                } finally {
                    lock.unlock()
                }
            }
            if (index.dictionaries.isNotEmpty()) {
                Log.v(KeyboardApp.LOG_TAG, "saving check dictionary time ")
                // Set update time into preference value
                saveDicCheckTime(Utils.getTimeMilis())
            }
        } catch (e: Exception) {
            Log.e(KeyboardApp.LOG_TAG, e.message, e)
        }
        Log.v(KeyboardApp.LOG_TAG, "finished loading dictionary list")
    }

    val isNeedCheckingUpdate: Boolean
        /**
         * Returns if checking update is required
         */
        get() {
            val now = Utils.getTimeMilis()
            val afterUpdate = dicUpdatedTime + CHECK_AFTER_UPDATE < now
            val afterLastCheck = dicCheckTime + CHECK_LAST < now
            val isNeedAppUpgradeOrDictUpdate = isNeedUpgrade || isNeedUpdate
            Log.v(
                KeyboardApp.LOG_TAG,
                "isNeedCheckingUpdate(): afterUpdate $afterUpdate; afterLastCheck $afterLastCheck; isNeedAppUpgradeOrDictUpdate $isNeedAppUpgradeOrDictUpdate"
            )
            return !isNeedAppUpgradeOrDictUpdate && afterUpdate && afterLastCheck
        }

    /**
     * Notify diction list changed
     */
    private fun markAndNotifyUpdatedState(
        newDicList: ArrayList<DictionaryItem>?,
        oldDicList: ArrayList<DictionaryItem>?
    ): Boolean {
        Assert.assertTrue(newDicList != null)
        Assert.assertTrue(oldDicList != null)
        mIsNeedUpdate = false
        if (mDicUpdatedListener != null) {
            val needUpdateList = ArrayList<DictionaryItem>()
            // Compare 2 dictionary info
            for (i in newDicList!!.indices) {
                val newItem = newDicList[i]
                val oldItem = getDictionaryItemPrim(oldDicList, newItem.language)
                if (oldItem == null || newItem.version > oldItem.version) {
                    Log.v(KeyboardApp.LOG_TAG, "new dictionary available " + newItem.language)
                    newItem.isNeedUpdate = true
                    if (oldItem != null) {
                        newItem.isInstalled = oldItem.isInstalled
                    }
                    needUpdateList.add(newItem)
                }
            }
            if (needUpdateList.size > 0) {
                mIsNeedUpdate = true

                // Send updated event
                mDicUpdatedListener!!.onDictionaryUpdated(needUpdateList)
                for (i in needUpdateList.indices) {
                    mDicUpdatedListener!!.onDictionaryItemUpdated(needUpdateList[i])
                }
            } else {
                Log.v(
                    KeyboardApp.LOG_TAG,
                    "There is no need to update dictionaries"
                )
            }
        }
        return mIsNeedUpdate
    }

    private val dicUpdatedTime: Long
        // Load updated time
        get() {
            // Set update time into preference value
            val preference = mContext.getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            )
            return preference.getLong(mRes.getString(R.string.dic_updated_time), 0)
        }

    // Save current wallpaper drawable id
    fun saveDicUpdatedTime(updatedTime: Long) {
        val preferenceEditor = mContext
            .getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            )
            .edit()
        preferenceEditor.putLong(mRes.getString(R.string.dic_updated_time), updatedTime)
        preferenceEditor.apply()
    }

    // Save current wallpaper drawable id
    private fun saveDicCheckTime(updatedTime: Long) {
        val preferenceEditor = mContext
            .getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            )
            .edit()
        preferenceEditor.putLong(mRes.getString(R.string.dic_checked_time), updatedTime)
        preferenceEditor.apply()
    }

    val dicCheckTime: Long
        // Load checked time
        get() {
            // Set update time into preference value
            val preference = mContext
                .getSharedPreferences(
                    Settings.SETTINGS_FILE,
                    Context.MODE_PRIVATE
                )
            return preference.getLong(mRes.getString(R.string.dic_checked_time), 0)
        }

    /**
     * Retrieve dictionary item by dictionary name
     */
    fun getDictionaryItem(dicName: String): DictionaryItem? {
        val item: DictionaryItem?
        lock.lock()
        item = try {
            getDictionaryItemPrim(mDicList, dicName)
        } finally {
            lock.unlock()
        }
        return item
    }

    private fun getDictionaryItemPrim(
        list: List<DictionaryItem>?,
        dicName: String
    ): DictionaryItem? {
        var item: DictionaryItem
        lock.lock()
        try {
            for (i in list!!.indices) {
                item = list[i]
                if (item.language == dicName) {
                    return item
                }
            }
        } finally {
            lock.unlock()
        }
        return null
    }

    /**
     * Set event listener
     */
    fun setOnDictionaryUpdatedListener(listener: OnDictionaryUpdatedListener?) {
        mDicUpdatedListener = listener
    }

    fun markAsReadAll() {
        mIsNeedUpdate = false
        var item: DictionaryItem
        for (i in mDicList.indices) {
            item = mDicList[i]
            item.isNeedUpdate = false
        }
//        DatabaseHelper.safeGetDatabaseHelper(mContext).saveDicInfos(mDicList)
    }

    /**
     * Specified dictionary exist or not
     */
    fun isDictionaryExist(context: Context, langID: String): Boolean {
        return langID == context.resources.getString(R.string.lang_code_other) || isDictionaryExist(
            context,
            getDictionaryItem(langID)
        )
    }

    val installedDictionaryNames: List<String>
        /**
         * Retrieve dictionary version info
         */
        get() {
            val installed: MutableList<String> = ArrayList()
            try {
                if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    for (i in mDicList.indices) {
                        val item = mDicList[i]
                        if (!item.isNeedUpdate) {
                            val lang = Settings.getNameFromValue(
                                mContext,
                                "language",
                                item.language
                            )
                            installed.add(
                                String.format(
                                    Locale.getDefault(),
                                    "%s (v%d)", lang, item.version
                                )
                            )
                        }
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                lock.unlock()
            }
            return installed
        }
    val isNeedUpdate: Boolean
        /**
         * It returns true if there is a new dictionary available, otherwise false
         */
        get() = updatedStatus

    fun checkNeedUpdate(context: Context?): Boolean {
        // TODO: Move this to a method in DatabaseHelper
        val c = DatabaseHelper
            .safeGetDatabaseHelper(context).mDB.query(
                DatabaseHelper.DIC_LANGUAGES_TABLE_NAME,
                null,
                DatabaseHelper.DIC_LANGUAGES_FIELD_IS_NEED_UPDATE
                    + " = 1 AND "
                    + DatabaseHelper.DIC_LANGUAGES_FIELD_IS_INSTALLED
                    + " = 1",
                null,
                null,
                null,
                null
            )
        val count = c.count
        c.close()
        Log.v(KeyboardApp.LOG_TAG, "checkNeedUpdate() " + (count > 0))
        updatedStatus = count > 0
        KeyboardService.IME.isNeedUpdateDicts = count > 0
        return count > 0
    }

    val isNeedUpgrade: Boolean
        /**
         * Checks application upgrading required or not  by dictionary minimum application version code
         */
        get() {
            val preference = mContext.getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            )
            val requiredVersionCode =
                preference.getInt(mContext.getString(R.string.dic_app_version_code), 0)
            try {
                val pkg = mContext.packageName
                val mVersionNumber = mContext.packageManager.getPackageInfo(pkg, 0).versionCode
                return requiredVersionCode > mVersionNumber
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(KeyboardApp.LOG_TAG, "error", e)
            }
            return false
        }

//    /**
//     * Dictionary has minimum application version code to up-to-date
//     */
//    private fun setNeedUpgrade(version: Int) {
//        val preferenceEditor = mContext.getSharedPreferences(
//            Settings.SETTINGS_FILE,
//            Context.MODE_PRIVATE
//        )
//            .edit()
//        preferenceEditor.putInt(
//            mContext.getString(R.string.dic_app_version_code),
//            version
//        )
//        preferenceEditor.apply()
//
//        // Notify service
//        if (KeyboardService.IME != null) {
//            val isNeedUpgrade = isNeedUpgrade
//            KeyboardService.IME.isNeedUpgradeApp = isNeedUpgrade
//            if (isNeedUpgrade) {
//                KeyboardService.IME.showSuggestionAppUpdateOnUi()
//            }
//        }
//    }

    var updatedStatus: Boolean
        get() {
            // Get preferences
            val preference = mContext.getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            )

            // Check language
            val currLang = preference.getString(
                "language",
                mContext.resources.getString(R.string.lang_code_default)
            )
            return currLang != mContext.resources.getString(R.string.lang_code_other) && preference.getBoolean(
                mContext.resources.getString(R.string.prefs_is_dic_updated),
                false
            )
        }
        set(status) {
            val preferenceEditor = mContext.getSharedPreferences(
                Settings.SETTINGS_FILE,
                Context.MODE_PRIVATE
            ).edit()
            preferenceEditor.putBoolean(
                mContext.resources.getString(R.string.prefs_is_dic_updated),
                status
            )
            preferenceEditor.apply()
        }

    companion object {
        private const val CHECK_LAST = 24 * 60 * 60 * 1000L // check new updates every 1 day
        private const val CHECK_AFTER_UPDATE =
            7 * 24 * 60 * 60 * 1000L // after updating don't check  again for 7 days.

        // Period of updating time
        private var mUIHandler: Handler? = null

        @JvmStatic
        fun isDictionaryExist(
            context: Context?,
            item: DictionaryItem?
        ): Boolean {
            if (item == null) {
                return false
            }

            // Check dictionary existing
            val folder = "databases"
            val file = File(
                Utils.getInternalFilePath(
                    context,
                    folder + "/" + item.filename
                )
            )
            if (!file.exists()) {
                Log.v(KeyboardApp.LOG_TAG, "!file.exists() " + file.absolutePath)
                return false
            }
            if (file.length() < item.size) {
                // File can be bigger but not smaller.
                Log.v(KeyboardApp.LOG_TAG, "file.length() < fileItem.size")
                return false
            }
            return true
        }
    }
}