@file:Suppress("DEPRECATION")
/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.text.InputType
import android.text.SpannableString
import android.text.method.MetaKeyKeyListener
import android.text.style.SuggestionSpan
import android.util.Log
import android.util.SparseIntArray
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.comet.keyboard.dictionary.DictionaryUtils
import com.comet.keyboard.dictionary.Suggestor
import com.comet.keyboard.dictionary.Suggestor.FinalSuggestions
import com.comet.keyboard.dictionary.updater.DictionaryDownloader
import com.comet.keyboard.dictionary.updater.DictionaryItem
import com.comet.keyboard.layouts.KeyboardLayout
import com.comet.keyboard.settings.AppRater
import com.comet.keyboard.settings.CandidateHeightSetting
import com.comet.keyboard.settings.KeyHeightSetting
import com.comet.keyboard.settings.KeyPaddingHeightSetting
import com.comet.keyboard.settings.KeyboardPaddingBottomSetting
import com.comet.keyboard.settings.LanguageProfileManager
import com.comet.keyboard.settings.LanguageSelector
import com.comet.keyboard.settings.OnResultListener
import com.comet.keyboard.settings.Settings
import com.comet.keyboard.settings.SoundVolumeSetting
import com.comet.keyboard.theme.KeyboardThemeManager
import com.comet.keyboard.util.DatabaseHelper
import com.comet.keyboard.util.OnPopupMenuItemClickListener
import com.comet.keyboard.util.PopupMenuView
import com.comet.keyboard.util.ProfileTracer
import com.comet.keyboard.util.Utils
import com.google.android.voiceime.VoiceRecognitionTrigger
import junit.framework.Assert
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * KeyboardService represents the IME service and is the "core" of the keyboard.
 * It implements KeyboardView.OnKeyboardActionsListener to handle all key events
 * except for long-presses, which are handled by KeyboardView.
 *
 * @author Barry Fruitman
 */
open class KeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    /**
     * Gets the keyboard view for this service.
     *
     * @return The keyboard view.
     */
    var keyboardView: KeyboardView? = null
        private set
    private var mKeyboardLayout: View? = null
    private var mCandidateView: CandidateView? = null
    private var mCompletions: Array<CompletionInfo>? = emptyArray()
    private var mSuggestions: FinalSuggestions? = null
    private var mSoundPool: SoundPool? = null
    private var mSoundsMap: SparseIntArray? = null
    private var mSoundVolume = 0f

    // Configuration
    protected var mLastKeyboardState: KeyboardState? = null
    private var mIsAlarmMessageFirstAppeared = false

    /**
     * is keyboard input view created
     *
     * @return
     */
    var isInputViewCreated = false
        private set
    private var mLastWord: String? = null
    private val WORD_REGEX = "[a-zA-Z0-9']*"
    private var mWordMatcher: Matcher? = null
    protected var mWindowVisible = false
    protected var mPrefsChanged = true
    protected var mKeyboardLayoutId: String? = null
    protected var mComposing: StringBuilder? = StringBuilder()
    protected var mPassword = StringBuilder()
    private var mLastPasswordId = -1

    // Keyboard states
    protected var mPredictionOn = false
    protected var mCompletionOn = false
    protected var bAutoSpace = false
    protected var mIsPassword = false
    protected var mShowPassword = false
    var mURL = false
    protected var mLearnWords = false
    protected var mAnyKeyState: String? = null
    protected var mAnyKeyLongState: String? = null
    protected var mReturnKeyLongState: String? = null
    protected var mMetaState: Long = 0
    protected var mLastDisplayWidth = 0
    protected var mLastShiftTime: Long = 0
    protected var mLastSpaceTime: Long = 0

    // The various keyboards that can be displayed
    protected var mArrowPadKeyboard: BaseKeyboard? = null
    protected var mNumPadKeyboard: BaseKeyboard? = null
    protected var mAlphaKeyboard: BaseKeyboard? = null
    protected var mAlphaNumKeyboard: BaseKeyboard? = null
    protected var mNumRowOn = false

    /**
     * Returns the list of punctuation that may be followed by a smart space.
     *
     * @return A list of punctuation.
     */
    private var smartSpacePreceders: String? = null
    var suggestor: Suggestor? = null
    private val mVoiceIsShifted = false
    private val mVoiceCapsLock = false

    // User prefs
    protected var mSmartSpaces = false
    protected var mDoubleSpace = false
    protected var mShowSuggestions = false
    @JvmField
    var mAutoSelect = false
    var isAutoCaps = false
        protected set
    protected var mVibrateLength = 0
    protected var mVibrateOnTypoCorrection = false
    protected var mSoundKey = false
    protected var mPredictNextWord = false
    protected var mShowPopupKey = false
    protected var mSwipeNumberRow = false
    var isSuperLabelEnabled = true
    protected var mBehaviorLocaleButton: String? = null
    protected var mInputContents = ""
    protected var mStartInputContents = ""

    // Ugh
    private var mGoogleMailHackCapsMode = false
    var isNeedUpgradeApp = false
    var isNeedUpdateDicts = false
    var language: String? = null
        private set
    var mTheme: String? = null
    protected var mDefaultCurrency: CharSequence? = null

    /*
	 * DEBUG METHODS
	 */ val callTrace = ArrayList<String>()
    private var mInPreviewMode = false
    private var mIsVibrated = false
    private var mFirstRepeatablePress = false
    private val DOUBLE_CLICK_TIME = 500 // Max # of milliseconds between

    // two clicks to count as
    // double-tap
    // Google Voice Typing. This is the default voice input in 4.0 and higher devices.
    var mVoiceRecognitionTrigger: VoiceRecognitionTrigger? = null
    private var mPendingRequest = false

    /**
     * Inputs text from voice input result(s)
     *
     * @param textIn
     */
    fun inputVoiceResult(textIn: String) {
        var text = textIn
        text = text.lowercase(Locale.getDefault()) + " "
        bAutoSpace = true
        if (mVoiceCapsLock) {
            text = text.uppercase(Locale.getDefault())
        } else if (mVoiceIsShifted) {
            text = DictionaryUtils.capFirstChar(text)
        }
        appendText(text)
        updateShiftKeyState()
    }

    /**
     * Main initialization of the input method component. Be sure to call to
     * super class.
     */
    override fun onCreate() {
        super.onCreate()
        callTrace("onCreate()")
        smartSpacePreceders = resources.getString(R.string.smart_space_preceders)
        // android.os.Debug.waitForDebugger();

        // Set the instance variable
        IME = this

        // Load preferences
        val sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, MODE_PRIVATE)
        mDefaultCurrency =
            sharedPrefs.getString("currency", getString(R.string.curr_symbol_default))
        mKeyboardLayoutId = KeyboardLayout.getCurrentLayout().id
        mDebug = sharedPrefs.getBoolean("debug_mode", false)
        Log.v(KeyboardApp.LOG_TAG, "debug mode = " + mDebug)

        // Create suggestor
        suggestor = Suggestor.getSuggestor()

        // Sound
        mSoundPool = SoundPool(4, SoundVolumeSetting.STREAM_TYPE, 100)
        mSoundsMap = loadSounds(this, mSoundPool!!)
        mSoundVolume = SoundVolumeSetting.getVolumePreference(this)
        isNeedUpgradeApp = KeyboardApp.getApp().updater.isNeedUpgrade
        isNeedUpdateDicts = KeyboardApp.getApp().updater.isNeedUpdate

        // Creates a regex pattern matcher for words
        createWordMatcher()
        mVoiceRecognitionTrigger = VoiceRecognitionTrigger(this)
    }

    /**
     * This is the point where you can do all of your UI initialization. It is
     * called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        callTrace("onInitializeInterface()")
        if (mLastKeyboardState == null) {
            mLastKeyboardState = KeyboardState()
        }
        if (mAlphaKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available space has changed.
            val displayWidth = maxWidth
            if (displayWidth == mLastDisplayWidth) return
            mLastDisplayWidth = displayWidth
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return KeyHeightSetting.isFullScreenMode(this)
    }

    /**
     * Sets the current keyboard by resource id.
     *
     * @param keyboardResID A resource id.
     * @return The Keyboard object (should be an instance of BaseKeyboard).
     */
    private fun setKeyboard(keyboardResID: Int): Keyboard? {
        when (keyboardResID) {
            mNumPadKeyboard!!.xmlResID -> {
                keyboardView!!.keyboard = mNumPadKeyboard
            }
            mArrowPadKeyboard!!.xmlResID -> {
                keyboardView!!.keyboard = mArrowPadKeyboard
            }
            mAlphaKeyboard!!.xmlResID -> {
                keyboardView!!.keyboard = mAlphaKeyboard
            }
            else -> {
                Assert.assertTrue(keyboardResID == mAlphaNumKeyboard!!.xmlResID)
                keyboardView!!.keyboard = mAlphaNumKeyboard
            }
        }
        return keyboardView!!.keyboard
    }

    /**
     * Update keyboard view
     */
    fun updateKeyboardView() {
        keyboardView!!.keyboard = keyboardView!!.keyboard
        keyboardView!!.invalidate()
        mCandidateView!!.updateView()
    }

    /**
     * Create the keyboards used in this view
     */
    private fun createKeyboards() {
        when (mKeyboardLayoutId) {
            getString(R.string.kb_id_qwerty_sp) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_es)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_es_num)
            }
            getString(R.string.kb_id_qwerty_intl) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_intl)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_intl_num)
            }
            getString(R.string.kb_id_azerty_fr) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.azerty_fr)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.azerty_fr_num)
            }
            getString(R.string.kb_id_qwerty_sl) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_sl)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_sl_num)
            }
            getString(R.string.kb_id_qwerty_sv) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_sv)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_sv_num)
            }
            getString(R.string.kb_id_azerty_be) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.azerty_be)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.azerty_be_num)
            }
            getString(R.string.kb_id_qwertz_de) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwertz_de)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwertz_de_num)
            }
            getString(R.string.kb_id_qwertz_sl) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwertz_sl)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwertz_sl_num)
            }
            getString(R.string.kb_id_t9) -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.t9)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.t9)
            }
            else -> {
                mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_en)
                mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_en_num)
            }
        }
        mNumPadKeyboard = constructMainKB(this, R.xml.num_pad)
        mArrowPadKeyboard = constructMainKB(this, R.xml.arrow_keypad)
        updateCurrencyKeys()
    }

    /**
     * Update key height
     */
    fun updateKeyHeight() {
        // Get current keyboard height
        val keyHeight = KeyHeightSetting.getKeyHeightPreference(this)
        val bottomGap = KeyboardPaddingBottomSetting
            .getKeyboardPaddingBottomPreference(this)
        if (mAlphaNumKeyboard != null) {
            mAlphaNumKeyboard!!.bottomGap = bottomGap
            mAlphaNumKeyboard!!.setKeyboardHeight(this, keyHeight)
        }
        if (mAlphaKeyboard != null) {
            mAlphaKeyboard!!.bottomGap = bottomGap
            mAlphaKeyboard!!.setKeyboardHeight(this, keyHeight)
        }
        if (mNumPadKeyboard != null) {
            mNumPadKeyboard!!.bottomGap = bottomGap
            mNumPadKeyboard!!.setKeyboardHeight(this, keyHeight)
        }
        if (mArrowPadKeyboard != null) {
            mArrowPadKeyboard!!.bottomGap = bottomGap
            mArrowPadKeyboard!!.setKeyboardHeight(this, keyHeight)
        }
        // set changed flag: hacked from KeyboardView.java
        keyboardView!!.keyboard = keyboardView!!.keyboard
        keyboardView!!.invalidate()
    }

    /**
     * Update keys padding height
     */
    fun updateKeyPaddingHeight() {
        // Get current keyboard height
        val keyPaddingHeight = KeyPaddingHeightSetting
            .getKeyPaddingHeightPreference(this)

        // Update all the keyboards.
        if (mAlphaKeyboard != null) {
            mAlphaKeyboard!!.setKeyboardPaddingHeight(this, keyPaddingHeight)
        }
        if (mAlphaNumKeyboard != null) {
            mAlphaNumKeyboard!!.setKeyboardPaddingHeight(this, keyPaddingHeight)
        }
        if (mAlphaNumKeyboard != null) {
            mAlphaNumKeyboard!!.setKeyboardPaddingHeight(this, keyPaddingHeight)
        }
        // set changed flag: hacked from KeyboardView.java
        keyboardView!!.keyboard = keyboardView!!.keyboard
    }

    /**
     * Update bottom padding of keyboard
     */
    fun updateKeyboardBottomPaddingHeight() {
        val left = keyboardView!!.paddingLeft
        val right = keyboardView!!.paddingRight
        val top = keyboardView!!.paddingTop
        val bottom = keyboardView!!.paddingBottom
        keyboardView!!.setPadding(left, right, top, bottom)
        updateKeyHeight()
        // set changed flag: hacked from KeyboardView.java
        keyboardView!!.keyboard = keyboardView!!.keyboard
    }

    /**
     * Constructs a new BaseKeyboard.
     *
     * @param context The context (this class).
     * @param resId   The keyboard resource id.
     * @return The BaseKeyboard object.
     */
    private fun constructMainKB(context: Context, resId: Int): BaseKeyboard {
        return BaseKeyboard(context, resId)
    }

    /**
     * Called by the framework when your view for creating input needs to be
     * generated. This will be called the first time your input method is
     * displayed, and every time it needs to be re-created such as due to a
     * configuration change.
     */
    override fun onCreateInputView(): View {
        callTrace("onCreateInputView()")

        // Create keyboards and layouts
        createKeyboards()
        createKeyboardLayout()
        isInputViewCreated = true
        return mKeyboardLayout!!
    }

    /**
     * Called by onCreateInputView() to create the layout views and assign the
     * keyboard view.
     *
     * @return
     */
    protected fun createKeyboardLayout(): View {
        // Inflate the keyboard layout
        mKeyboardLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as View

        // Find the keyboard view
        keyboardView = mKeyboardLayout!!.findViewById<View>(R.id.keyboard_view) as KeyboardView

        // Assign a key listener
        keyboardView!!.onKeyboardActionListener = this
        mCandidateView = mKeyboardLayout!!.findViewById<View>(R.id.candidate) as CandidateView

        // Assign the keyboard
        if (mLastKeyboardState!!.mChanged /* && lastConfiguration != null */) {
            // Restore status
            setKeyboard(mLastKeyboardState!!.keyboardResID)
            keyboardView!!.capsLock = mLastKeyboardState!!.mCapsLock
            keyboardView!!.setShifted(
                mLastKeyboardState!!.bShifted,
                mLastKeyboardState!!.mCapsLock
            )
            setCandidatesViewShown(mLastKeyboardState!!.isCandidateViewShown)
        } else {
            keyboardView!!.keyboard = mAlphaKeyboard
            setCandidatesViewShown(false)
        }
        return keyboardView!!
    }

    /**
     * Called when the keyboard size (may have) changed.
     */
    fun onKeyboardSizeChanged() {
        val w: Int
        val h: Int
        val oldW: Int
        val oldH: Int
        oldW = keyboardView!!.width
        w = oldW
        oldH = keyboardView!!.width
        h = oldH
        keyboardView!!.onSizeChanged(w, h, oldW, oldH)
    }

    // THIS METHOD IS A HACK BECAUSE THE LANGUAGE MENU ITEMS DO NOT SIGNAL
    // THE OnItemClickListener IN TranslatorView :(
    @Suppress("unused")
    fun onClickLanguageMenuItem(item: View?) {
        keyboardView!!.mTranslatorView?.onClickLanguageMenuItem(item as TextView?)
    }

    /**
     * Listener for the locale (i.e. language) menu.
     */
    private val mLocaleMenuListener =
        OnPopupMenuItemClickListener { position, title -> // Get the language code from the language name (e.g. "en" from
            // "English")
            val code = LanguageSelector.getCodeFromName(this@KeyboardService, title)
            // Update the language profile
            LanguageProfileManager.getProfileManager().setCurrentProfile(code)

            // Restart the input view. This updates the keyboard layout.
            onFinishInputView(true)
            onStartInputView(safeGetCurrentInputEditorInfo(), true)

            // Notify the user
            val toastMsg = String.format(
                this@KeyboardService.resources.getString(R.string.selected_locale_confirm_message),
                title
            )
            Toast.makeText(this@KeyboardService, toastMsg, Toast.LENGTH_SHORT).show()
        }

    /**
     * Callback for all the back buttons in the various popup windows (Any Key
     * menu, symbols menu, translator)
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickPopupBack(@Suppress("UNUSED_PARAMETER") v: View?) {
        dismissAllPopupWindows()
    }
    //    /**
    //     * Callback for the Comet logo.
    //     *
    //     * @param activity The activity that displayed the logo.
    //     */
    //    public static void onClickComet(final Activity activity) {
    //        final Uri uri = Uri.parse("http://m.cometapps.com/");
    //        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    //        activity.startActivity(intent);
    //    }
    /**
     * Callback for the share button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickShare(@Suppress("UNUSED_PARAMETER") v: View?) {
        dismissAllPopupWindows()
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(
            Intent.EXTRA_SUBJECT, String.format(
                getString(R.string.share_to_sns_title),
                getString(R.string.ime_name)
            )
        )
        shareIntent.putExtra(
            Intent.EXTRA_TEXT, String.format(
                getString(R.string.share_to_sns_description),
                getString(R.string.ime_name), KeyboardApp.getApp().appStoreUrl
            )
        )
        val chooserIntent = Intent.createChooser(shareIntent, "Share with")
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(chooserIntent)
    }

    /**
     * Callback for the voice key.
     */
    protected fun onVoiceKey() {
        openVoiceTyping()
    }

    /**
     * Callback for the voice button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickVoiceInput(@Suppress("UNUSED_PARAMETER") v: View?) {
        openVoiceTyping()
    }

    /**
     * Opens the voice input activity.
     */
    private fun openVoiceTyping() {
        if (mVoiceRecognitionTrigger!!.isInstalled) {
            mVoiceRecognitionTrigger!!.startVoiceRecognition()
        }
    }

    /**
     * Callback for the translator key.
     */
    protected fun onTranslatorKey() {
        openTranslator()
    }

    /**
     * Callback for the translator button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickTranslator(@Suppress("UNUSED_PARAMETER") v: View?) {
        openTranslator()
    }

    /**
     * Opens the translator window.
     */
    private fun openTranslator() {
        keyboardView!!.openTranslator()
    }

    /**
     * Callback for the settings key.
     */
    protected fun onSettingsKey() {
        launchSettings()
    }

    /**
     * Callback for the main menu key.
     */
    fun onMainMenuKey() {
        openMainMenu()
    }

    /**
     * Opens main menu
     */
    protected fun openMainMenu() {
        keyboardView!!.openMainMenu()
    }

    /**
     * Callback for the settings button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickSettings(@Suppress("UNUSED_PARAMETER") v: View?) {
        launchSettings()
    }

    /**
     * Launches the settings activity.
     */
    protected fun launchSettings() {
        dismissAllPopupWindows()

        // Launch settings
        val intent = Intent()
        intent.setAction(getString(R.string.settings_intent))
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Callback for the URL key.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickUrlKey(@Suppress("UNUSED_PARAMETER") v: View?) {
        openUrlMenu()
    }

    /**
     * Opens the URL menu.
     */
    private fun openUrlMenu() {
        keyboardView!!.openUrlKeyboard()
    }

    /**
     * Callback for the symbols main menu key.
     */
    protected fun onSymMenuKey() {
        openSymMenuMenu()
    }

    /**
     * Opens the symbols main menu.
     */
    protected fun openSymMenuMenu() {
        keyboardView!!.openSymMenu()
    }

    /**
     * Callback for the emoji key.
     */
    protected fun onEmojiKey() {
        writeDebug("KeyboardService.onEmojiKey()")
        openEmojiMenu()
    }

    /**
     * Opens the emoji menu.
     */
    protected fun openEmojiMenu() {
        writeDebug("KeyboardService.openEmojiMenu()")
        keyboardView!!.openEmojiKeyboard()
    }

    /**
     * Callback for the arrow keypad key.
     */
    protected fun onArrowKeypadKey() {
        arrowKeyboardMode()
    }

    /**
     * Callback for the arrow keypad button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickArrowKeypad(@Suppress("UNUSED_PARAMETER") v: View?) {
        arrowKeyboardMode()
    }

    /**
     * Callback for the language key.
     */
    private var mRotateToast: Toast? = null
    fun onLocaleKey() {
        dismissAllPopupWindows(true)

        // If we don't have any language profile, we should show error.
        val nextProfile = LanguageProfileManager.getProfileManager().nextProfile
        if (nextProfile == null) {
            val toastMsg = getString(R.string.selected_locale_no_profiles)
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            return
        }
        if (mBehaviorLocaleButton == getString(R.string.settings_locale_button_id_open_menu)) {
            // Open the locale menu.
            openLocaleMenu()
        } else {
            // Switch to the next language.
            Assert.assertTrue(mBehaviorLocaleButton == getString(R.string.settings_locale_button_id_next_locale))
            LanguageProfileManager.getProfileManager().setCurrentProfile(nextProfile.lang)

            // Restart the input view. This updates the keyboard layout.
            onFinishInputView(true)
            onStartInputView(safeGetCurrentInputEditorInfo(), true)

            // Notify the user
            val toastMsg = String.format(
                this@KeyboardService.resources.getString(R.string.selected_locale_confirm_message),
                LanguageSelector.getNameFromCode(this, nextProfile.lang)
            )

            // Cancel the last toast message. This is in case the user presses
            // the locale button rapidly.
            if (mRotateToast != null) {
                mRotateToast!!.cancel()
            }
            mRotateToast = Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).apply {
                show()
            }
        }
    }

    /**
     * Callback for the language button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickLocale(@Suppress("UNUSED_PARAMETER") v: View?) {
        onLocaleKey()
    }

    /**
     * Callback for the about button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickAbout(@Suppress("UNUSED_PARAMETER") v: View?) {
        // Launch the about page
        val intent = Intent(this, About::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Opens the language menu.
     */
    fun openLocaleMenu() {
        // Get a list of languages
        val profileList = LanguageProfileManager
            .getProfileManager().loadProfiles()
        val profileStrList = ArrayList<String>(
            profileList.size
        )
        // Get the current language index
        for (i in profileList.indices) {
            val strName = LanguageSelector.getNameFromCode(this, profileList[i].lang)
            profileStrList.add(strName)
        }

        // Create a menu
        val mPopupMenu = PopupMenuView(
            IME.mKeyboardLayout as ViewGroup?,
            profileStrList, R.string.any_key_action_locale,
            R.drawable.ic_launcher
        )
        mPopupMenu.setOnPopupMenuItemClickListener(mLocaleMenuListener)
        mPopupMenu.showContextMenu()
    }

    /**
     * Callback for the help button in the Any Key menu.
     *
     * @param v The button view.
     */
    @Suppress("unused")
    fun onClickHelp(@Suppress("UNUSED_PARAMETER") v: View?) {
        launchHelp()
    }

    /**
     * Launches the knowledge base website.
     */
    protected fun launchHelp() {
        dismissAllPopupWindows(true)

        // Launch the help & support PreferenceScreen
        val intent = Intent()
        intent.setAction(getString(R.string.settings_intent))
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Settings.LAUNCH_ACTIVITY_KEY, "help_support")
        startActivity(intent)
    }

    fun replaceText(text: CharSequence?) {
        val inputConnection = currentInputConnection
        inputConnection.performContextMenuAction(android.R.id.selectAll)
        commitText(text)
    }

    /**
     * Commits text to the edit view. Overrides any current composing text.
     *
     * @param text The text to commit.
     */
    protected fun commitText(text: CharSequence?) {
        setLastWord(text)
        mComposing!!.setLength(0)
        val inputConnection = currentInputConnection ?: return

        // FIRST INACTIVE INPUTCONNECTION
        inputConnection.commitText(text, 1)
    }

    /**
     * Append new text to the end of the edit view.
     *
     * @param text The text to append.
     */
    private fun appendText(text: String) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(text, 1)
    }

    /**
     * Hide or display the candidate view.
     *
     * @param show true to display, false to hide.
     */
    override fun setCandidatesViewShown(show: Boolean) {
        setCandidatesViewShown(show, true)
    }

    /**
     * Hide or display the candidate view.
     *
     * @param showIn        Set true to display or false to hide.
     * @param rememberState Flag to save state.
     */
    fun setCandidatesViewShown(showIn: Boolean, rememberState: Boolean) {

        //
        //
        var show = showIn
        show = true
        //
        //
        if (mCandidateView == null) {
            return
        }
        if (mShowSuggestions && show) {
            if (!mCandidateView!!.isShown) {
                mCandidateView!!.visibility = View.VISIBLE
                onKeyboardSizeChanged()
            }
        } else if (mCandidateView!!.isShown) {
            mCandidateView!!.visibility = View.GONE
            onKeyboardSizeChanged()
        }
        if (rememberState) {
            mLastKeyboardState!!.isCandidateViewShown = show
        }
    }

    /**
     * Apply a photo wallpaper to the background. Used by WallpaperPhoto for
     * preview purposes only.
     *
     * @param wallpaper Wallpaper drawable.
     * @param alpha     Alpha value for keys (not background).
     * @param fit       Set true to resize both dimensions to fit. This may (will)
     * change the aspect ratio. If false, it will be resized in only
     * one dimension, and cropped. This preserves the aspect ratio.
     */
    fun applyWallpaper(wallpaper: Drawable?, alpha: Int, fit: Boolean) {
        keyboardView!!.applyWallpaper(wallpaper, alpha, fit)
    }

    /**
     * Reloads the wallpaper. Called by wallpaper setting activity to restore
     * the original wallpaper if user cancels the preview.
     */
    fun reloadWallpaper() {
        keyboardView!!.reloadWallpaper()
    }

    /**
     * Reloads the current theme.
     */
    private fun reloadTheme() {
        KeyboardThemeManager.getThemeManager().reloadTheme()
        keyboardView!!.reloadTheme()
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (mInputContents != "") {
            // Parse input from previous session
            learnWords()
        }

        // Get starting contents of EditText
        mStartInputContents = inputContents
        mInputContents = mStartInputContents
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application. At this point we have been bound to
     * the client, and are now receiving all of the detailed information about
     * the target of our edits.
     */
    override fun onStartInputView(editorInfoIn: EditorInfo, restarting: Boolean) {
        var editorInfo = editorInfoIn
        super.onStartInputView(editorInfo, restarting)
        callTrace("onStartInputView()")

        // Rate app in market
        AppRater.promptUserToRate(this)

        // Show welcome screen (first time only)
        Welcome.showWelcome(this)

        // Escape this package to avoid losing focus
        if (editorInfo.packageName == "com.google.android.voicesearch") {
            return
        }

        // Load user preferences
        loadPrefs()

        // Retrieve current theme
        reloadTheme()

        // Reset our state. We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any
        // way.
        if (!mLastKeyboardState!!.mChanged) {
            mComposing!!.setLength(0)
            clearCandidates()

            // Reset composing region
            val inputConnection = currentInputConnection
            inputConnection?.finishComposingText()

            // Clear suggestions
            mLastKeyboardState!!.resetSuggestions()
        }
        updateCandidateFontSize()
        keyboardView!!.capsLock = false
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0
        }

        // Start with default values
        mPredictionOn = false
        mCompletionOn = false
        mIsPassword = false
        mLearnWords = false
        mCompletions = null
        mLastWord = null
        mURL = false

        // Various hacks for different apps that pass strange or illegal values
        // in EditorInfo
        editorInfo = editorInfoHack(editorInfo)
        var keyboard: BaseKeyboard?
        when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME ->                 // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                keyboard = mNumPadKeyboard

            EditorInfo.TYPE_CLASS_PHONE ->                 // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                keyboard = mNumPadKeyboard

            EditorInfo.TYPE_CLASS_TEXT -> {
                // This is general text editing. We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                keyboard = mAlphaKeyboard
                mPredictionOn = true
                mLearnWords = true

                // We now look for a few special variations of text that will modify our behavior.
                val variation = editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false
                    mLearnWords = false
                    mIsPassword = true
                    mSmartSpaces = false
                    if (editorInfo.fieldId != mLastPasswordId
                        || inputContents.length != mPassword.length
                    ) {
                        mPassword.setLength(0)
                    }
                    mLastPasswordId = editorInfo.fieldId
                }
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    // Predictions are not useful for e-mail addresses
                    mPredictionOn = false
                    mLearnWords = false
                    mSmartSpaces = false
                    mURL = true
                }
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    // Predictions are not useful for e-mail addresses or URIs.
                    mPredictionOn = false
                    mLearnWords = false
                    mSmartSpaces = false
                    mURL = true
                }
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for filters
                    mPredictionOn = false
                    mLearnWords = false
                    mURL = false
                }
                if (editorInfo.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                    // The app has it's own completions to display. If we're in fullscreen mode,
                    // display them instead of our predictions. If not, the app will display
                    // the completions and we can still display predictions.
                    if (isFullscreenMode.also { mCompletionOn = it }) mPredictionOn = false
                }
            }

            else -> {
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                keyboard = mAlphaKeyboard
                mPredictionOn = false
            }
        }

        // Restore number row if keyboard is alpha
        if (keyboard === mAlphaKeyboard && mNumRowOn) {
            openNumRow()
            keyboard = mAlphaNumKeyboard
        }

        // App settings override defaults for predictions
        if (mShowSuggestions == false) {
            mPredictionOn = false
        }
        if (!mPredictionOn) {
            // reset last configuration
            mLastKeyboardState!!.resetSuggestions()
        }

        // Update the keyboard for the current input state
        setCandidatesViewShown(mPredictionOn || mIsPassword)
        keyboard!!.setImeOptions(resources, editorInfo)
        keyboardView!!.setAnyKeyAction(mAnyKeyState!!, mAnyKeyLongState, mReturnKeyLongState)

        // Set preview stat
        keyboardView!!.isPreviewEnabled = mShowPopupKey

        // Update keyboard bottom padding
        updateKeyboardBottomPaddingHeight()
        if (mLastKeyboardState!!.mChanged || mLastKeyboardState!!.mNeedToUpdate) {
            // Restore saved state
            val handler = Handler()
            handler.post {
                keyboardView!!.restorePopupWindow(
                    mLastKeyboardState!!.popupLayoutID,
                    mLastKeyboardState!!.popupKeyboardID,
                    mLastKeyboardState!!.popupKeyboardParm
                )
            }
            if (mPredictionOn && !mCompletionOn) {
                mCandidateView!!.setSuggestions(mLastKeyboardState!!.suggestions)
            }
            keyboardView!!.capsLock = mLastKeyboardState!!.mCapsLock
            keyboardView!!.setShifted(
                mLastKeyboardState!!.bShifted,
                mLastKeyboardState!!.mCapsLock
            )
            mLastKeyboardState!!.mNeedToUpdate = false
        } else {
            if (keyboardView!!.keyboard !== keyboard) {
                keyboardView!!.keyboard = keyboard
            }
            updateShiftKeyState(editorInfo, isMultiLine) // Use auto-caps in
            // multi-line fields
            // even if the app
            // didn't request
            // it);
            clearCandidates()
        }

        // Clear the suggestion bar
        mCandidateView!!.clearDisplayMessage()
        if (mIsPassword) {
            // Initialize the password value
            updatePassword(false)
        }
        if (!inPreviewMode()) {
            if (!KeyboardApp.getApp().updater.isDictionaryExist(
                    this,
                    language
                ) || isNeedUpdateDicts
            ) {
                // No dictionary installed. Display a prompt in the candidate view
                showSuggestionDictionaryUpdate()
            } else if (isNeedUpgradeApp) {
                showSuggestionAppUpdate()
            }
        }
        if (mVoiceRecognitionTrigger != null) {
            mVoiceRecognitionTrigger!!.onStartInputView()
        }
    }

    fun showSuggestionDictionaryUpdate() {
        if (mCandidateView != null) {
            mCandidateView!!.setDisplayMessage(
                resources
                    .getString(R.string.dic_updated_alarm_message),
                CandidateView.MessageType.MESSAGE_ALARM,
                object : View.OnClickListener {
                    override fun onClick(v: View) {
                        if (!KeyboardApp.getApp().updater.isDictionaryExist(
                                this@KeyboardService,
                                language
                            )
                        ) launchDictionariesUpdate(
                            arrayOf<String?>(
                                language
                            )
                        ) else launchDictionariesUpdate()
                    }
                }
            )
            mIsAlarmMessageFirstAppeared = true
        }
    }

    fun showSuggestionDictionaryUpdateOnUi() {
        if (mCandidateView != null) {
            mCandidateView!!.post { showSuggestionDictionaryUpdate() }
        }
    }

    fun showSuggestionAppUpdate() {
        if (mCandidateView != null) {
            mCandidateView!!.setDisplayMessage(
                resources
                    .getString(R.string.dic_updated_app_is_needed_upgrade),
                CandidateView.MessageType.MESSAGE_STATIC
            ) {
                val intent = Intent(
                    Intent.ACTION_VIEW, Uri
                        .parse("market://details?id=" + KeyboardApp.packageName)
                )
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            mIsAlarmMessageFirstAppeared = true
        }
    }

    /**
     * Runs showSuggestionAppUpdate() on UI thread
     */
    fun showSuggestionAppUpdateOnUi() {
        if (mCandidateView != null) mCandidateView!!.post { showSuggestionAppUpdate() }
    }

    /**
     * Callback for check password prompt. Toggles the password reveal.
     */
    private val mOnClickPassword = View.OnClickListener { updatePassword(!mShowPassword) }

    /**
     * This is called when the user is done editing a field. We can use this to
     * reset our state.
     */
    override fun onFinishInput() {
        super.onFinishInput()
        callTrace("KeyboardService.onFinishInput()")

        // Parse the inputted text to learn look-ahead word prediction.
        learnWords()

        // Clear current composing text and candidates.
        mComposing!!.setLength(0)
        clearCandidates()

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false, false)
        if (mLastKeyboardState!!.mChanged) {
            mLastKeyboardState!!.mChanged = false
            mLastKeyboardState!!.mNeedToUpdate = true
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        callTrace("onFinishInputView()")
        mInPreviewMode = false

        // hide all context menu
        PopupMenuView.closeAllMenu()
    }

    /**
     * Load user settings from shared prefs and re-initialize keyboard if
     * necessary.
     */
    protected fun loadPrefs() {
        val sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, MODE_PRIVATE)
        if (mKeyboardLayoutId != KeyboardLayout.getCurrentLayout().id) {
            // Keyboard layout changed
            mKeyboardLayoutId = KeyboardLayout.getCurrentLayout().id
            createKeyboards()
            keyboardView!!.keyboard = mAlphaKeyboard
        }
        suggestor!!.loadPreferences()
        mDefaultCurrency =
            sharedPrefs.getString("currency", getString(R.string.curr_symbol_default))
        isAutoCaps = sharedPrefs.getBoolean("auto_caps", true)
        mSmartSpaces = sharedPrefs.getBoolean("smart_spaces", true)
        mDoubleSpace = sharedPrefs.getBoolean("double_space", false)
        mBehaviorLocaleButton = sharedPrefs.getString(
            "behavior_locale_button",
            getString(R.string.settings_locale_button_id_next_locale)
        )
        mShowSuggestions = sharedPrefs.getBoolean("show_suggestions", true)
        mAutoSelect = sharedPrefs.getBoolean("auto_select", true)
        mVibrateLength =
            sharedPrefs.getString("vibrate", getString(R.string.settings_vibrate_default))!!
                .toInt()
        mVibrateOnTypoCorrection = sharedPrefs.getBoolean("vibrate_on_typo_correction", false)
        mSoundKey = sharedPrefs.getBoolean("sound", false)
        mPredictNextWord = sharedPrefs.getBoolean("nextword", true)
        mShowPopupKey = sharedPrefs.getBoolean("preview_popup", true)
        mAnyKeyState =
            sharedPrefs.getString("any_key_action", getString(R.string.any_key_action_id_default))
        mAnyKeyLongState = sharedPrefs.getString(
            "any_key_long_action",
            getString(R.string.any_key_long_action_id_default)
        )
        mReturnKeyLongState = sharedPrefs.getString(
            "return_key_long_action",
            getString(R.string.return_key_long_action_id_default)
        )
        mSwipeNumberRow = sharedPrefs.getBoolean("swipe_num_row", true)

//		mSuggestionsAlwaysShow = sharedPrefs.getBoolean("suggestions_always_show", true);
        if (mDebug) {
            writeDebug("loadPrefs(): mDefaultCurrency=$mDefaultCurrency")
            writeDebug("loadPrefs(): mAutoCaps=" + isAutoCaps)
            writeDebug("loadPrefs(): mSmartSpaces=$mSmartSpaces")
            writeDebug("loadPrefs(): mDoubleSpace=$mDoubleSpace")
            writeDebug("loadPrefs(): mBehaviorLocaleButton=$mBehaviorLocaleButton")
            writeDebug("loadPrefs(): mShowSuggestions=$mShowSuggestions")
            writeDebug("loadPrefs(): mAutoSelect=$mAutoSelect")
            writeDebug("loadPrefs(): mVibrateLength=$mVibrateLength")
            writeDebug("loadPrefs(): mVibrateOnTypoCorrection=$mVibrateOnTypoCorrection")
            writeDebug("loadPrefs(): mSoundKey=$mSoundKey")
            writeDebug("loadPrefs(): mPredictNextWord=$mPredictNextWord")
            writeDebug("loadPrefs(): mShowPopupKey=$mShowPopupKey")
            writeDebug("loadPrefs(): mAnyKeyState=$mAnyKeyState")
            writeDebug("loadPrefs(): mAnyKeyLongState=$mAnyKeyLongState")
            writeDebug("loadPrefs(): mReturnKeyLongState=$mReturnKeyLongState")
            writeDebug("loadPrefs(): mSwipeNumberRow=$mSwipeNumberRow")
        }
        language = LanguageSelector.getLanguagePreference(this)
        mTheme = sharedPrefs.getString("theme", getString(R.string.default_theme_id))

        // enable/diable long press symbols
        isSuperLabelEnabled = sharedPrefs.getBoolean("long_press_symbols", true)
        updateCurrencyKeys()
        mPrefsChanged = false
    }

    fun isDrawSuperLabel(key: Keyboard.Key): Boolean {
        val primaryCode = key.codes[0]
        return !(!isSuperLabelEnabled
            && !(isFnKeyPressed(primaryCode) || primaryCode == BaseKeyboard.KEYCODE_MODE || key.label != null && key.label == "." && key.edgeFlags == BaseKeyboard.EDGE_BOTTOM))
    }

    /**
     * Called by Settings when prefs change. Causes a reload on next
     * onStartInput().
     */
    fun onPrefsChanged() {
        mPrefsChanged = true
    }

    /**
     * Parse input text and save bi-grams (word pairs) to learn look-ahead word
     * prediction.
     */
    private fun learnWords() {
        if (!mLearnWords) return

        // Don't learn "Other" language
        if (language == getString(R.string.lang_code_other)) return

        // Remove original text from final text, if necessary
        if (mStartInputContents != "") {
            mInputContents =
                if (mInputContents.contains(mStartInputContents)) mInputContents.replace(
                    mStartInputContents,
                    ""
                ) else ""
        }
        mStartInputContents = ""
        suggestor!!.learnSuggestions(mInputContents)
        mStartInputContents = ""
        mInputContents = ""
    }

    /**
     * Remembers a word so it is immediately available for suggestions.
     *
     * @param word The word to remember
     */
    protected fun rememberWord(word: String?) {
        if (suggestor!!.languageDictionary.remember(word)) {
            Toast.makeText(
                applicationContext,
                resources.getString(R.string.msg_word_remembered, word),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onSuggestionMenuItemClick(word: String?) {
        val inputConnection = currentInputConnection
        inputConnection?.finishComposingText()
        mComposing!!.setLength(0)
        clearCandidates()
        rememberWord(word)
    }

    private val inputContents: String
        /**
         * Gets the current contents of the edit view, up to MAX_INPUT_LENGTH chars
         * before/after cursor.
         *
         * @return The contents of the edit view.
         */
        get() {
            val inputConnection = currentInputConnection
            val inputText = StringBuilder("")
            if (inputConnection != null) {
                val before = getTextBeforeCursor(inputConnection, MAX_INPUT_LENGTH)
                val after = getTextAfterCursor(inputConnection, MAX_INPUT_LENGTH)
                inputText.append(before).append(after)
            }
            return inputText.toString()
        }

    fun showMessage(message: String?, listener: View.OnClickListener?) {
        // Using View.post() lets this method be called from a non-UI thread.
        if (mCandidateView == null) {
            return
        }
        mCandidateView!!.post {
            mCandidateView!!.setDisplayMessage(
                message,
                CandidateView.MessageType.MESSAGE_STATIC,
                listener
            )
        }
        mComposing!!.setLength(0)
    }

    fun clearMessage() {
        // Using View.post() lets this method be called from a non-UI thread.
        if (mCandidateView == null) {
            return
        }
        mCandidateView!!.post { mCandidateView!!.clearDisplayMessage() }
    }

    /**
     * Displays a sample suggestion. This is used only by the suggestion height
     * setting preview.
     */
    fun showSampleSuggestion() {
        // set demo candidate string
        showMessage("Suggestion-Height", null)
        updateCandidateFontSize()
    }

    /**
     * Update the suggestion font size to user setting.
     */
    fun updateCandidateFontSize() {
        if (mCandidateView == null) {
            return
        }

        // Get current candidate font size
        val fontSize = CandidateHeightSetting.getFontSizePreference(this)

        // set candidate view size
        mCandidateView!!.setFontHeight(fontSize)
        mCandidateView!!.requestLayout()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        composingStartIn: Int,
        composingEndIn: Int
    ) {
        var composingStart = composingStartIn
        var composingEnd = composingEndIn
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            composingStart, composingEnd
        )
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()
        if (composingEnd > composingStart
            && (newSelEnd < composingStart || newSelStart > composingEnd)
        ) {
            // Selection has moved outside composing. Stop composing.
            inputConnection.finishComposingText()
            mComposing!!.setLength(0)
            composingStart = -1
            composingEnd = -1
            bAutoSpace = false
        }
        if (composingStart == composingEnd) {
            mComposing = StringBuilder()
        }
        updateComposing(inputConnection)
        inputConnection.endBatchEdit()

        // Update input contents
        mInputContents = inputContents
        if (mInputContents.length == 0) {
            mComposing!!.setLength(0)
        }
    }

    private fun updateComposing(inputConnection: InputConnection?) {
        if (!mPredictionOn || mComposing != null && mComposing!!.length > 0) {
            // Predictions unnecessary, or already in progress
            return
        }
        if (inputConnection == null) {
            return
        }
        val before: CharSequence = StringBuilder(getWordBeforeCursor(inputConnection))
        val after: CharSequence = StringBuilder(getWordAfterCursor(inputConnection))
        if (before.length > 0) {
            val cursor = getCursorLocation(inputConnection)
            if (safeSetComposingRegion(
                    inputConnection,
                    cursor - before.length, cursor + after.length
                )
            ) // Only in 2.3.3+ (see below)
                mComposing = StringBuilder(before).append(after)
        }
        updateCandidates()
    }

    private fun safeSetComposingRegion(
        inputConnection: InputConnection,
        start: Int,
        end: Int
    ): Boolean {
        if (isGoogleMailBody) {
            return false
        }
        if (mInputConnection_setComposingRegion != null) {
            try {
                return mInputConnection_setComposingRegion!!.invoke(
                    inputConnection,
                    start,
                    end
                ) as Boolean
            } catch (ite: InvocationTargetException) {
                // Unexpected exception; wrap and re-throw
                throw RuntimeException(ite)
            } catch (ie: IllegalAccessException) {
                // Do nothing?
            }
        }
        return false
    }
    /**
     * This tells us about completions that the editor has determined based on
     * the current text in it. We want to use this in fullscreen mode to show
     * the completions ourself, since the editor can not be seen in that
     * situation.
     */
    /*
	 * RESTORE THIS METHOD RESTORE THIS METHOD RESTORE THIS METHOD RESTORE THIS
	 * METHOD RESTORE THIS METHOD
	 *
	 * @Override public void onDisplayCompletions(CompletionInfo[] completions)
	 * { if (mCompletionOn) { mCompletions = completions; if (completions ==
	 * null) { clearCandidates(); return; }
	 *
	 * List<String> stringList = new ArrayList<String>(); for (int i=0;
	 * i<(completions != null ? completions.length : 0); i++) { CompletionInfo
	 * ci = completions[i]; if (ci != null) { CharSequence text = ci.getText();
	 * if(text != null) stringList.add(text.toString()); } }
	 * setSuggestions(stringList, true); } }
	 */
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection. It is only needed when using the PROCESS_HARD_KEYS
     * option.
     */
    private fun translateKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event)
        var c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState))
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState)
        val ic = currentInputConnection
        if (c == 0 || ic == null) {
            return false
        }
        if (c and KeyCharacterMap.COMBINING_ACCENT != 0) {
            c = c and KeyCharacterMap.COMBINING_ACCENT_MASK
        }
        if (mComposing!!.length > 0) {
            val accent = mComposing!![mComposing!!.length - 1]
            val composed = KeyEvent.getDeadChar(accent.code, c)
            if (composed != 0) {
                c = composed
                mComposing!!.setLength(mComposing!!.length - 1)
            }
        }
        onKey(c, null)
        return true
    }

    /**
     * Use this to monitor key events being delivered to the application. We get
     * first crack at them, and can either resume them or let them continue to
     * the app.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!mWindowVisible) {
            return false
        }
        bAutoSpace = false
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Ignore this event if the translator popup is open (onKeyUp
                // will close it)
                val translatorView = keyboardView!!.mTranslatorView
                if (translatorView != null) {
                    if (translatorView.findViewById<View>(R.id.text_clipboard)
                            .visibility != View.GONE
                    ) {
                        return true
                    }
                    if (translatorView.findViewById<View>(R.id.menu_languages)
                            .visibility != View.GONE
                    ) {
                        return true
                    }
                }

                // Ignore this event if popup windows are showing. It will be
                // handled in onKeyUp().
                if (keyboardView!!.isPopupShowing) {
                    return true
                }

                // Ignore this event in certain activities. They will handle it.
                if (inPreviewMode()) {
                    return false
                }
            }

            KeyEvent.KEYCODE_DEL ->                 // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing!!.length > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null)
                    return true
                }

            KeyEvent.KEYCODE_ENTER ->                 // Let the underlying text editor always handle these.
                return false

            else ->                 // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true
                    }
                }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Use this to monitor key events being delivered to the application. We get
     * first crack at them, and can either resume them or let them continue to
     * the app.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!mWindowVisible) {
            // Don't do anything if the keyboard is not visible.
            return false
        }

        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(
                    mMetaState,
                    keyCode, event
                )
            }
        }
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                val translatorView = keyboardView!!.mTranslatorView
                if (translatorView != null) {
                    // Close translator items (clipboard translation popup, or
                    // language menu)
                    val textClipboard = keyboardView!!.mTranslatorView?.findViewById<View>(R.id.text_clipboard) as TextView
                    if (textClipboard.visibility != View.GONE) {
                        // Close the clipboard translation
                        textClipboard.visibility = View.GONE
                        return true
                    }
                    val menuLanguages = keyboardView!!.mTranslatorView?.findViewById<View>(R.id.menu_languages) as ListView
                    if (menuLanguages.visibility != View.GONE) {
                        // Close a language menu
                        menuLanguages.visibility = View.GONE
                        return true
                    }
                }
                if (keyboardView!!.isPopupShowing) {
                    // Close popup keyboard
                    dismissAllPopupWindows()
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onWindowHidden() {
        // Composing text is automatically committed, so clear mComposing and
        // candidates view
        mComposing!!.setLength(0)
        clearCandidates()
        dismissAllPopupWindows()

        // Keep track of window visibility.
        mWindowVisible = false
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Keep track of window visibility.
        mWindowVisible = true
    }

    /**
     * Commit the current composition. Do not replace with a suggestion.
     */
    fun commitTyped(): CharSequence? {
        setLastWord(mComposing.toString())
        val inputConnection = currentInputConnection
        if (inputConnection != null && mComposing!!.length > 0) {
            inputConnection.commitText(mComposing, 1)
            mComposing!!.setLength(0)
            clearCandidates()
        }
        return mComposing
    }

    /**
     * Update the shift key state based on the app preference, or force all-caps
     * if caps==true.
     *
     * @param ei     An EditorInfo provided by the app.
     * @param capsIn Set true to force caps. If false, use EditorInfo.
     */
    private fun updateShiftKeyState(
        ei: EditorInfo? = safeGetCurrentInputEditorInfo(),
        capsIn: Boolean = false
    ) {
        var caps = capsIn
        if (ei != null) {
            if (ei.inputType != EditorInfo.TYPE_NULL && isAutoCaps) {
                val inputConnection = currentInputConnection ?: return
                if (caps == false) {
                    caps = if (isGoogleMailBody(ei)) {
                        mGoogleMailHackCapsMode
                    } else {
                        val capsMode = inputConnection.getCursorCapsMode(ei.inputType)
                        capsMode > 0
                    }
                }
            }
            keyboardView!!.setShifted(caps && isAutoCaps, keyboardView!!.capsLock)
        }
    }

    /**
     * Returns the current EditorInfo, processed by editorInfoHack().
     *
     * @return The current EditorInfo from the app (hacked).
     */
    private fun safeGetCurrentInputEditorInfo(): EditorInfo {
        var editorInfo = currentInputEditorInfo
        // Update the EditorInfo with hack overrides.
        editorInfo = editorInfoHack(editorInfo)
        return editorInfo
    }

    /**
     * This method hacks the EditorInfo provided by the app. Why? Because Google
     * Mail body field is F***ED.
     *
     * @param editorInfo The EditorInfo to hack.
     * @return The hacked EditorInfo.
     */
    private fun editorInfoHack(editorInfo: EditorInfo): EditorInfo {
        // Android Email uses an unknown input class
        if (isGoogleMailBody(editorInfo)) {
            if (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS == 0xf) {
                // Android Email uses an unknown input class. Change it to
                // standard text.
                editorInfo.inputType = (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS.inv()
                    or EditorInfo.TYPE_CLASS_TEXT)
            }
            if (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION == 0xff) {
                // Android Email uses unknown IME options. Change it to none
                editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE
            }
        }
        return editorInfo
    }

    private fun googleMailSetCapsModeHack(code: Int) {
        mGoogleMailHackCapsMode = if (code == '.'.code || code == '?'.code || code == '!'.code) {
            true
        } else {
            false
        }
    }

    private fun isGoogleMailBody(editorInfo: EditorInfo): Boolean {
        return if (editorInfo.packageName == "com.android.email" && editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE > 0) {
            // Android Email body doesn't handle auto-caps correctly
            true
        } else false
    }

    private val isGoogleMailBody: Boolean
        get() = isGoogleMailBody(currentInputEditorInfo)

    /**
     * Helper to determine if a given character code belongs in a word. In all
     * languages, letters and numbers are word characters. Depending on the
     * language, certain punctuation may be a word character. (e.g.
     * English:apostrophe, French:hyphen).
     *
     * @param code The character code to check.
     * @return true if code is a word character.
     */
    private fun isWordCharacter(code: Int): Boolean {
        if (language == "fr") {
            // FRENCH HACK
            if (Character.isLetter(code) || Character.isDigit(code) || code == '-'.code) {
                return true
            }
        } else if (Character.isLetter(code) || Character.isDigit(code) || code == '\''.code) {
            run { return true }
        }
        return false
    }

    /**
     * This method determines if a character code should be followed by a smart
     * space.
     *
     * @param code The code to check.
     * @return true if code should be followed by a smart space.
     */
    fun isSmartSpacePreceder(code: Int): Boolean {
        return smartSpacePreceders!!.contains(code.toChar().toString())
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private fun keyDownUp(keyEventCode: Int) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }
    /**
     * Helper to send a character to the editor as raw key events.
     */
    /*
	 * private void sendKey(int keyCode) { switch (keyCode) { case '\n':
	 * keyDownUp(KeyEvent.KEYCODE_ENTER); break; default: if (keyCode >= '0' &&
	 * keyCode <= '9') { keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0); } else {
	 * InputConnection inputConnection = getCurrentInputConnection();
	 * if(inputConnection == null) return;
	 * inputConnection.commitText(String.valueOf((char) keyCode), 1); } break; }
	 * }
	 */
    /**
     * Detects any key button via primary code
     *
     * @param primaryCode
     * @return
     */
    fun isFnKeyPressed(primaryCode: Int): Boolean {
        when (primaryCode) {
            BaseKeyboard.KEYCODE_SETTINGS, BaseKeyboard.KEYCODE_MODE_CHANGE, BaseKeyboard.KEYCODE_SYM_MENU, BaseKeyboard.KEYCODE_EMOJI, BaseKeyboard.KEYCODE_ARROWS, BaseKeyboard.KEYCODE_VOICE, BaseKeyboard.KEYCODE_TRANSLATE, BaseKeyboard.KEYCODE_LOCALE -> return true
        }
        return false
    }

    /**
     * This method receives and handles all keystrokes.
     *
     * @param primaryCode The primary keystroke code.
     * @param keyCodes    All possible codes for this keystroke.
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        writeDebug("KeyboardService.onKey(primaryCode=$primaryCode)")
        if (IME.inPreviewMode()) {
            // Keyboard is in preview mode
            if (primaryCode == BaseKeyboard.KEYCODE_ACTION) {
                // Hide the keyboard
                onCancelKey()
            }
            return
        }
        dismissAllPopupWindows()

        // Send keystroke audio and vibrate feedback. Only sends one feedback
        // for repeatable keys.
        mIsVibrated = false
        if (!isRepeatable(primaryCode) || mFirstRepeatablePress) {
            // Send vibrate and/or sound feedback
            keyFeedback(primaryCode)

            // Only vibrate once for repeatable keys
            mFirstRepeatablePress = false
        }

        // Ugh. Hack for Android
        googleMailSetCapsModeHack(primaryCode)
        if (mIsAlarmMessageFirstAppeared) {
            clearCandidates()
            mIsAlarmMessageFirstAppeared = false
        }

        // If the user pressed certain punctuation (e.g. period) after an
        // auto-space, delete the space before inserting the punc.
        if (bAutoSpace && mSmartSpaces && isSmartSpacePreceder(primaryCode)) {
            // Delete the auto-space
            val inputConnection = currentInputConnection
            if (inputConnection != null) {
                val before = getTextBeforeCursor(inputConnection, 1)
                if (before.length == 1 && before[0] == ' ') inputConnection.deleteSurroundingText(
                    1,
                    0
                )
            }
        }
        bAutoSpace = false

        // Reset timer for double-tap space
        if (primaryCode != ' '.code) mLastSpaceTime = 0
        if (isWordSeparator(primaryCode)) {
            // Handle word separator
            onWordSeparator(primaryCode)
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            // Handle delete key
            onDelete()
        } else if (primaryCode == BaseKeyboard.KEYCODE_DEL_FORWARD) {
            // Handle delete forward key
            onDeleteForward()
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            // Handle shift key
            onShiftKey()
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            // Handle cancel key
            onCancelKey()
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            // Handle keyboard mode key
            if (keyboardView!!.mPopupKeyboardWindow!!.isShowing) {
                // Close sym quick menu
                keyboardView!!.mPopupKeyboardWindow!!.dismiss()
            } else {
                toggleKeyboardMode()
            }
        } else if (primaryCode == BaseKeyboard.KEYCODE_ACTION) {
            // Handle action key
            when (keyboardView!!.keyboard!!.getEditorAction(false)) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_UNSPECIFIED, EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND -> performEditorAction()
                else -> {
                    // User pressed return.
                    onEnter()
                    val editorInfo = safeGetCurrentInputEditorInfo()
                    updateShiftKeyState(editorInfo, isMultiLine)
                }
            }
        } else if (primaryCode == BaseKeyboard.KEYCODE_ARROW_UP || primaryCode == BaseKeyboard.KEYCODE_ARROW_LEFT || primaryCode == BaseKeyboard.KEYCODE_ARROW_RIGHT || primaryCode == BaseKeyboard.KEYCODE_ARROW_DOWN || primaryCode == BaseKeyboard.KEYCODE_ARROW_HOME || primaryCode == BaseKeyboard.KEYCODE_ARROW_END || primaryCode == BaseKeyboard.KEYCODE_ARROW_BACK || primaryCode == BaseKeyboard.KEYCODE_ARROW_NEXT || primaryCode == BaseKeyboard.KEYCODE_CUT || primaryCode == BaseKeyboard.KEYCODE_COPY || primaryCode == BaseKeyboard.KEYCODE_PASTE || primaryCode == BaseKeyboard.KEYCODE_SELECT || primaryCode == BaseKeyboard.KEYCODE_SELECT_ALL) {
            // Handle cursor keys
            onArrowKey(primaryCode)
        } else if (primaryCode > 0) {
            // Handle character key
            onCharacter(primaryCode)
        } else {
            onFnKey(primaryCode)
        }
    }

    /**
     * Handle any key
     *
     * @param primaryCode
     * @return
     */
    private fun onFnKey(primaryCode: Int): Boolean {
        when (primaryCode) {
            BaseKeyboard.KEYCODE_EMOJI -> {
                // Handle emoji key
                onEmojiKey()
                return true
            }

            BaseKeyboard.KEYCODE_ARROWS -> {
                // Handle arrow keypad key
                onArrowKeypadKey()
                return true
            }

            BaseKeyboard.KEYCODE_VOICE -> {
                onVoiceKey()
                return true
            }

            BaseKeyboard.KEYCODE_TRANSLATE -> {
                // Handle translator key
                onTranslatorKey()
                return true
            }

            BaseKeyboard.KEYCODE_LOCALE -> {
                // Handle locale key
                onLocaleKey()
                return true
            }

            BaseKeyboard.KEYCODE_SYM_MENU -> {
                onSymMenuKey()
                return true
            }

            BaseKeyboard.KEYCODE_SETTINGS -> {
                // Handle settings key
                onSettingsKey()
                return true
            }
        }
        return false
    }

    /**
     * Called by the framework to insert text into the edit view.
     */
    override fun onText(text: CharSequence?) {
        writeDebug("KeyboardService.onText(text=$text)")
        setLastWord(text.toString())
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()
        if (mComposing!!.length > 0) {
            commitTyped()
        }
        inputConnection.commitText(text, 0)
        inputConnection.endBatchEdit()
        updateShiftKeyState()

        // Close popup keyboard if open
        dismissAllPopupWindows()
    }

    /**
     * Toggles the password reveal
     *
     * @param reveal Set true to reveal the password, false to hide it.
     */
    protected fun updatePassword(reveal: Boolean) {
        mShowPassword = reveal
        var message = getString(R.string.candidate_check_password)
        if (mIsPassword && mShowPassword) {
            message = mPassword.toString()
        }
        mCandidateView!!.setDisplayMessage(
            message,
            CandidateView.MessageType.MESSAGE_STATIC, mOnClickPassword
        )
    }

    /**
     * Gets a list of suggestions based on composing, to display to the user.
     *
     * @param composing The composing to match. Equal to the composition.
     */
    protected fun updateCandidates(composing: CharSequence?) {
        callTrace("updateCandidates($composing)")
        if (mIsPassword) {
            updatePassword(mShowPassword)
            return
        }
        if (mCompletionOn || !mPredictionOn) {
            return
        }

        // Find suggestions asynchronously. Suggestor will call
        // returnCandidates() when done.
        suggestor!!.findSuggestionsAsync(composing.toString()) { suggestions ->
            returnCandidates(
                suggestions
            )
        }
        mPendingRequest = true
    }

    fun updateCandidates() {
        updateCandidates(mComposing)
    }

    /**
     * Called asynchronously by the Suggestor to update the CandidateView
     *
     * @param suggestions The list of suggestions to display.
     */
    private fun returnCandidates(suggestions: FinalSuggestions) {
        mPendingRequest = false
        mSuggestions = suggestions
        if (mDebug) {
            val list = suggestions.suggestionsList
            val debug = StringBuilder("returnCandidates(): suggestions={")
            for (i in list.indices) debug.append(list[i]).append(",")
            writeDebug(debug.append("}"))
        }

        // Save the suggestions
        setSuggestions(mSuggestions, true)
    }

    /**
     * Update the list of available candidates from the current composing text.
     * This will need to be filled in by however you are determining candidates.
     */
    fun clearCandidates() {
        if (!mCompletionOn) {
            setLastWord("")
            setSuggestions(null, false)
        }
    }

    /**
     * Saves the suggestions and updates the CandidateView.
     *
     * @param suggestions The suggestions.
     * @param completions true if these are completions provided by the app, not the suggestor.
     */
    fun setSuggestions(suggestions: FinalSuggestions?, completions: Boolean) {
        // Make sure CandidateView is visible.
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true)
        } else if (isExtractViewShown) {
            setCandidatesViewShown(true)
        }

        // Update CandidateView
        if (mCandidateView != null) mCandidateView!!.setSuggestions(suggestions)

        // Save suggestions to keyboard state
        mLastKeyboardState!!.saveSuggestions(suggestions, completions)
    }

    /**
     *
     * Called by onKey() when the user presses delete (i.e. backspace) key.
     * Deletes the character in behind of the cursor.
     */
    protected fun onDelete() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText != null && selectedText.length > 0) {
            // There is selected text. Delete it and return.
            val nCharsToDelete = selectedText.length
            val cursorLocation = getCursorLocation(inputConnection) + nCharsToDelete
            inputConnection.setSelection(cursorLocation, cursorLocation)
            inputConnection.finishComposingText()
            inputConnection.deleteSurroundingText(nCharsToDelete, 0)
            mComposing!!.setLength(0)
        } else {
            // Check if user is inline editing a word.
            val before = StringBuilder()
            val after = StringBuilder()
            if (isEditingWord(inputConnection, before, after)) {
                // User is inline editing a word. Stop composing and backspace.
                val cursor = getCursorLocation(inputConnection)
                inputConnection.finishComposingText()
                inputConnection.setSelection(cursor, cursor)
                inputConnection.deleteSurroundingText(1, 0)
                mComposing!!.setLength(0)
                if (before.length > 0) {
                    before.setLength(before.length - 1)
                }
            } else {
                // Process a standard backspace.
                val length = mComposing!!.length
                if (length > 1) {
                    // Delete the last character in the composition
                    mComposing!!.delete(length - 1, length)
                    inputConnection.setComposingText(mComposing, 1)
                } else if (length > 0) {
                    // Delete the only character in the composition
                    mComposing!!.setLength(0)
                    inputConnection.commitText("", 0)
                } else if (mIsPassword) {
                    if (mPassword.length > 1) {
                        mPassword.delete(
                            mPassword.length - 1,
                            mPassword.length
                        )
                    } else if (mPassword.length == 1) {
                        mPassword.setLength(0)
                    }
                    keyDownUp(KeyEvent.KEYCODE_DEL)
                } else {
                    // Just delete the preceding character
                    keyDownUp(KeyEvent.KEYCODE_DEL)
                }
            }
        }
        inputConnection.endBatchEdit()

        // Update keyboard state
        clearCandidates()
        updateShiftKeyState()
        updateCandidates()
    }

    /**
     * Called by onKey() when user presses delete forward key. Deletes the
     * character in front of the cursor.
     */
    protected fun onDeleteForward() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()
        var nCharsToDelete = 1
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText != null && selectedText.length > 0) {
            // There is selected text. Delete it and return.
            nCharsToDelete = selectedText.length
            val cursorLocation = getCursorLocation(inputConnection)
            inputConnection.setSelection(cursorLocation, cursorLocation)
        }

        // Stop composing
        inputConnection.finishComposingText()
        inputConnection.deleteSurroundingText(0, nCharsToDelete)
        mComposing!!.setLength(0)
        inputConnection.endBatchEdit()

        // Update keyboard state
        clearCandidates()
        updateShiftKeyState()
        updateCandidates()
    }

    /**
     * Called by onKey() when the user presses the shift key. Updates the
     * keyboard shift state.
     */
    private fun onShiftKey() {
        // This key only affects the alpha keyboard
        if (isAlphaKeyboard) {
            checkToggleCapsLock()
            val bShifted = keyboardView!!.isShifted
            keyboardView!!.setShifted(!bShifted, keyboardView!!.capsLock)
        }
    }

    /**
     * Called by onKey() when the user presses a character key.
     *
     * @param codeIn The character code.
     */
    @SuppressLint("NewApi")
    protected fun onCharacter(codeIn: Int) {
        var code = codeIn
        if (isInputViewShown) {
            if (keyboardView!!.isShifted) {
                if (code.toChar() == 'ß') {
                    // Special case for upper-case ß
                    onText("SS")
                    return
                }
                // Shift is selected. Capitalize the letter.
                code = Character.toUpperCase(code)
            }
        }
        val inputConnection = currentInputConnection ?: return
        updateComposing(inputConnection)
        inputConnection.beginBatchEdit()

        // Delete selected text
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText != null && selectedText.length > 0) {
            // There is selected text. Delete it and return.
            val cursorLocation = getCursorLocation(inputConnection)
            inputConnection.setSelection(cursorLocation, cursorLocation)
            inputConnection.finishComposingText()
            inputConnection.deleteSurroundingText(0, selectedText.length)
            mComposing!!.setLength(0)
        }
        if ((!isWordCharacter(code) || code == '\''.code) && mPredictionOn && mComposing!!.length == 0) {
            // Do not start composing with a non-letter. Just commit.
            inputConnection.commitText(Character.toString(code.toChar()), 1)
        } else if (isWordCharacter(code) && mPredictionOn) {
            val before = StringBuilder()
            val after = StringBuilder()
            if (isEditingWord(inputConnection, before, after)) {
                // User is inline editing a word. Stop composing and insert text manually.
                val cursor = getCursorLocation(inputConnection)
                inputConnection.finishComposingText()
                inputConnection.setSelection(cursor, cursor)
                inputConnection.commitText("" + code.toChar(), 1)
                mComposing!!.setLength(0)
                before.append(code.toChar())
            } else {
                // Append to composing
                mComposing!!.append(code.toChar())
                inputConnection.setComposingText(mComposing, 1)
            }
            if (mDebug) {
                writeDebug("onCharacter(code=$code): mComposing=$mComposing")
            }
            updateCandidates()
        } else {
            // Prediction is off. Just append the character.
            val commit = code.toChar().toString() + ""
            inputConnection.commitText(commit, 1)
            if (mIsPassword) {
                // Append to password reveal.
                mPassword.append(commit)
                updatePassword(mShowPassword)
            }
        }
        inputConnection.endBatchEdit()
        updateShiftKeyState()
    }

    /**
     * "Inline editing" is when the user edits the middle of a composing word,
     * and the keyboard handles it by stopping composing (and therefore predictions).
     *
     * @param inputConnection The current InputConnection.
     * @param before          A StringBuilder to store the part of the word in front of the cursor.
     * @param after           A StringBuilder to store the part of the word after the cursor.
     * @return Returns true if the user is inline editing a word.
     */
    private fun isEditingWord(
        inputConnection: InputConnection,
        before: StringBuilder,
        after: StringBuilder
    ): Boolean {
        val result: Boolean
        before.append(getWordBeforeCursor(inputConnection))
        after.append(getWordAfterCursor(inputConnection))
        val composing = StringBuilder(before).append(after).toString()
        result = if (composing == mComposing.toString() && after.length > 0) {
            true
        } else {
            false
        }
        return result
    }

    /**
     * Called by onKey() when the user presses the enter key.
     */
    protected fun onEnter() {
        commitComposing()
        onText("\r\n")
    }

    /**
     * Returns the word in front of the cursor
     *
     * @param inputConnection The current InputConnection
     * @return A word, or empty string if there is no word directly in front of the cursor
     */
    private fun getWordBeforeCursor(inputConnection: InputConnection?): StringBuilder {
        if (inputConnection == null) {
            return StringBuilder("")
        }
        val wordBeforeCursor = StringBuilder()
        val before = getTextBeforeCursor(inputConnection, MAX_WORD_LENGTH)
        if (before.length == 0) {
            return wordBeforeCursor
        }
        var iStartOfWord: Int
        iStartOfWord = before.length - 1
        while (iStartOfWord >= 0 && isWordCharacter(before[iStartOfWord].code)) {
            iStartOfWord--
        }
        if (++iStartOfWord < before.length) {
            wordBeforeCursor.append(before.subSequence(iStartOfWord, before.length))
        }
        if (wordBeforeCursor.length == 1 && wordBeforeCursor[0] == '\'') {
            // Don't start a word with '
            wordBeforeCursor.setLength(0)
        }
        return wordBeforeCursor
    }

    /**
     * Delete the word, and any trailing punctuation/whitespace, in front of the
     * cursor.
     */
    private fun deleteWordBeforeCursor() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()
        var letterBeforeCursor = getTextBeforeCursor(inputConnection, 1)
        if (letterBeforeCursor.length == 1
            && !isWordCharacter(letterBeforeCursor[0].code)
        ) {
            // First delete whitespace/punc before cursor
            while (getTextBeforeCursor(inputConnection, 1).also {
                    letterBeforeCursor = it
                }.length == 1
                && !isWordCharacter(letterBeforeCursor[0].code)
            ) {
                inputConnection.deleteSurroundingText(1, 0)
            }
        }
        // Commit composing first
        if (mComposing!!.length > 0) {
            commitTyped()
        }
        // Now, delete the word (if any)
        while (getTextBeforeCursor(inputConnection, 1).also { letterBeforeCursor = it }
                .length == 1 && isWordCharacter(letterBeforeCursor[0].code)) {
            inputConnection.deleteSurroundingText(1, 0)
        }
        inputConnection.endBatchEdit()
    }

    /**
     * Returns the word following the cursor
     *
     * @param inputConnection A valid InputConnection
     * @return A word, or empty string if there is no word immediately following the cursor
     */
    private fun getWordAfterCursor(inputConnection: InputConnection?): StringBuilder {
        if (inputConnection == null) {
            return StringBuilder("")
        }

        // We are starting an append to a word. Make that word the new composing text.
        val word = StringBuilder()
        val after = getTextAfterCursor(inputConnection, MAX_WORD_LENGTH)
        if (after.length == 0) {
            return word
        }
        var iEndOfWord: Int
        iEndOfWord = 0
        while (iEndOfWord < after.length && isWordCharacter(after[iEndOfWord].code)) {
            iEndOfWord++
        }
        if (--iEndOfWord < after.length) word.append(after.subSequence(0, iEndOfWord + 1))
        return word
    }

    /**
     * Safe wrapper for InputConnection.getTextBeforeCursor() that returns ""
     * instead of null.
     *
     * @param inputConnection An InputConnection. It is checked for null value.
     * @param n               The number of characters to return. The result may be shorter.
     * @return The text before the cursor, up to n characters, or "" if there is none.
     */
    private fun getTextBeforeCursor(
        inputConnection: InputConnection?,
        n: Int
    ): CharSequence {
        return if (inputConnection == null) {
            ""
        } else inputConnection.getTextBeforeCursor(n, 0) ?: return ""
    }

    /**
     * Safe wrapper for InputConnection.getTextAfterCursor() that returns ""
     * instead of null.
     *
     * @param inputConnection An InputConnection. It is checked for null value.
     * @param n               The number of characters to return. The result may be shorter.
     * @return The text after the cursor, up to n characters, or "" if there is none.
     */
    private fun getTextAfterCursor(
        inputConnection: InputConnection?,
        n: Int
    ): CharSequence {
        return if (inputConnection == null) "" else inputConnection.getTextAfterCursor(n, 0)
            ?: return ""
    }

    /**
     * Returns an index to the cursor location in the edit text. (Why is there
     * no framework method for this???)
     *
     * @param inputConnection The current InputConnection.
     * @return An index to the cursor location.
     */
    private fun getCursorLocation(inputConnection: InputConnection?): Int {
        if (inputConnection == null) {
            return 0
        }
        var textBeforeCursor = getTextBeforeCursor(inputConnection, 99)
        if (textBeforeCursor.length >= 99) {
            textBeforeCursor = getTextBeforeCursor(inputConnection, 999)
            if (textBeforeCursor.length >= 999) {
                textBeforeCursor = getTextBeforeCursor(inputConnection, 9999)
                if (textBeforeCursor.length >= 9999) {
                    textBeforeCursor = getTextBeforeCursor(inputConnection, 99999)
                }
            }
        }
        return textBeforeCursor.length
    }

    /**
     * Move the cursor back one word. Skip any trailing punctuation/whitespace too.
     *
     * @param inputConnection The current InputConnection.
     */
    private fun cursorBackWord(inputConnection: InputConnection) {
        inputConnection.beginBatchEdit()
        var nChars = 0
        var letterBeforeCursor: CharSequence
        // First skip whitespace/punc before cursor
        while (getTextBeforeCursor(inputConnection, nChars + 1).also {
                letterBeforeCursor = it
            }.length == nChars + 1
            && !isWordCharacter(letterBeforeCursor[0].code)
        ) {
            nChars++
        }

        // Now, skip the word (if any)
        while (getTextBeforeCursor(inputConnection, nChars + 1).also {
                letterBeforeCursor = it
            }.length == nChars + 1
            && isWordCharacter(letterBeforeCursor[0].code)
        ) {
            nChars++
        }
        val cursor = getCursorLocation(inputConnection)
        inputConnection.setSelection(cursor - nChars, cursor - nChars)
        inputConnection.endBatchEdit()
    }

    /**
     * Move the cursor forward one word. Skip any preceding punctuation/whitespace too.
     *
     * @param inputConnection The current InputConnection.
     */
    private fun cursorNextWord(inputConnection: InputConnection) {
        inputConnection.beginBatchEdit()
        var nChars = 0
        // First, skip the word
        var letterAfterCursor: CharSequence
        while (getTextAfterCursor(inputConnection, nChars + 1).also {
                letterAfterCursor = it
            }.length == nChars + 1
            && isWordCharacter(letterAfterCursor[nChars].code)
        ) {
            nChars++
        }

        // Now skip any trailing whitespace or punctuation
        while (getTextAfterCursor(inputConnection, nChars + 1).also {
                letterAfterCursor = it
            }.length == nChars + 1
            && !isWordCharacter(letterAfterCursor[nChars].code)
        ) {
            nChars++
        }
        val cursor = getCursorLocation(inputConnection)
        inputConnection.setSelection(cursor + nChars, cursor + nChars)
        inputConnection.endBatchEdit()
    }

    /**
     * Returns up to two words in front of the composing, less leading or trailing
     * whitespace and punctuation. For example, if the user is typing
     * "nice to meet", word1="nice", word2="to" and composing="meet".
     *
     * @param word1 A StringBuilder that is filled with the word before word2.
     * @param word2 A StringBuilder that is filled with the word before the composing/cursor.
     * @return The number of words returned. May be < 2.
     */
    fun getTwoWordsBeforeComposing(word1: StringBuilder, word2: StringBuilder): Int {
        val inputConnection = currentInputConnection ?: return 0

        // Clear the StringBuilders, just in case.
        word1.setLength(0)
        word2.setLength(0)
        var iEndOfWord: Int
        var iStartOfWord: Int
        var iStartOfComposing: Int


        // Get the text before the cursor, up to 50 chars.
        val textBeforeCursor = getTextBeforeCursor(inputConnection, MAX_WORD_LENGTH)
        if (textBeforeCursor.length == 0) {
            return 0
        }


        // Skip the composing (if any)
        iStartOfComposing = textBeforeCursor.length - 1
        while (iStartOfComposing >= 0) {
            if (!isWordCharacter(textBeforeCursor[iStartOfComposing].code)) {
                break
            }
            iStartOfComposing--
        }
        if (iStartOfComposing < 0) {
            return 0
        }

        /*
		 * Get the word in front of the composing/cursor (i.e. word2)
		 */

        // Skip trailing whitespace & punctuation.
        iEndOfWord = iStartOfComposing
        while (iEndOfWord >= 0) {
            if (isWordCharacter(textBeforeCursor[iEndOfWord].code)) {
                break
            } else if (isSentenceSeparator(textBeforeCursor[iEndOfWord].code)) {
                return 0
            }
            iEndOfWord--
        }
        if (iEndOfWord < 0) {
            return 0
        }

        // Scan word2
        iStartOfWord = iEndOfWord
        while (iStartOfWord >= 0) {
            if (!isWordCharacter(textBeforeCursor[iStartOfWord].code)) {
                break
            }
            iStartOfWord--
        }
        if (iStartOfWord + 1 < 0) {
            return 0
        }

        // Write word2 to StringBuilder
        word2.append(textBeforeCursor.subSequence(iStartOfWord + 1, iEndOfWord + 1))

        /*
		 * Get the word in front of word2 (i.e. word1)
		 */

        // Skip trailing whitespace & punctuation.
        iEndOfWord = iStartOfWord
        while (iEndOfWord >= 0) {
            if (isWordCharacter(textBeforeCursor[iEndOfWord].code)) {
                break
            } else if (isSentenceSeparator(textBeforeCursor[iEndOfWord].code)) {
                return 1
            }
            iEndOfWord--
        }
        if (iEndOfWord < 0) {
            return 1
        }

        // Scan word1
        iStartOfWord = iEndOfWord
        while (iStartOfWord >= 0) {
            if (!isWordCharacter(textBeforeCursor[iStartOfWord].code)) {
                break
            }
            iStartOfWord--
        }
        if (iStartOfWord + 1 < 0) {
            return 1
        }

        // Write word1 to StringBuilder
        word1.append(textBeforeCursor.subSequence(iStartOfWord + 1, iEndOfWord + 1))
        return 2
    }

    /**
     * Called by onKey() when the user presses a word separator key, which is
     * any non-word key. Commits the current composition, replacing it with the
     * default suggestion if necessary, then commits the character.
     *
     * @param code The character code.
     */
    @SuppressLint("NewApi")
    protected fun onWordSeparator(code: Int) {
        if (mPendingRequest) {
            Handler().postDelayed({ onWordSeparator(code) }, 5)
            return
        }

        // Commit whatever is being typed
        val inputConnection = currentInputConnection ?: return

        // Start a batch edit to commit the suggestion (if any) and this whitespace character.
        inputConnection.beginBatchEdit()

        // Delete selected text
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText != null && selectedText.length > 0) {
            // There is selected text. Delete it and return.
            val cursorLocation = getCursorLocation(inputConnection)
            inputConnection.setSelection(cursorLocation, cursorLocation)
            inputConnection.finishComposingText()
            inputConnection.deleteSurroundingText(0, selectedText.length)
            mComposing!!.setLength(0)
        }
        val before = StringBuilder()
        val after = StringBuilder()
        if (isEditingWord(inputConnection, before, after)) {
            // User is inline editing a word. Stop composing and insert text manually.
            val cursor = getCursorLocation(inputConnection)
            inputConnection.finishComposingText()
            inputConnection.setSelection(cursor, cursor)
            inputConnection.commitText("" + code.toChar(), 1)
            inputConnection.finishComposingText()
            mComposing!!.setLength(0)
            before.append(code.toChar())
            inputConnection.endBatchEdit()
            return
        }

        /*
		 * First, commit the current composition
		 */
        var committed: CharSequence? = ""
        if (mPredictionOn) {
            if (mComposing!!.length > 0) {
                committed = if (mAutoSelect && (isSmartSpacePreceder(code) || code == ' '.code)) {
                    // Replace the composing with the default suggestion.
                    pickDefaultCandidate()
                } else {
                    commitTyped()
                }
            }
        }

        // Set auto-space flag if necessary
        if (code == ' '.code && committed!!.length > 0) {
            bAutoSpace = true
        }

        // Handle password
        if (mIsPassword) {
            mPassword.append(code.toChar())
            updatePassword(mShowPassword)
        }
        if (code != ' '.code) {
            // Don't start a new bi-gram
            mLastWord = null
        }

        /*
		 * Now, commit the character typed
		 */
        var commit = StringBuilder()
        if (code == ' '.code && checkDoubleSpace()) {
            commit.append(". ")
        } else {
            commit = StringBuilder().append(code.toChar())
            commit = appendSmartSpace(inputConnection, commit, code.toChar())
        }
        // Special case: Replace ". Com" with ".com"
        if (mSmartSpaces) {
            val dotcom = getTextBeforeCursor(inputConnection, 5)
            if (dotcom.length == 5 && (dotcom == ". com" || dotcom == ". Com")) {
                inputConnection.deleteSurroundingText(5, 0)
                commit = StringBuilder(".com").append(commit)
            }
        }

        // Commit!
        inputConnection.commitText(commit, 1)
        inputConnection.endBatchEdit()

        // Update keyboard state
        updateShiftKeyState()
        updateCandidates()
    }

    /**
     * Commits the composition, replacing it with the default suggestion if
     * necessary.
     *
     * @return The committed text.
     */
    private fun commitComposing(): CharSequence? {
        var committed: CharSequence? = ""
        if (mPredictionOn) {
            // Commit the composition
            if (mComposing!!.length > 0) {
                committed = if (mAutoSelect) {
                    pickDefaultCandidate()
                } else {
                    commitTyped()
                }
            }
        }

        // Update keyboard state
        updateShiftKeyState()
        updateCandidates()
        return committed
    }

    /**
     * Called by onKey() when the user presses a cursor key.
     *
     * @param code The cursor key code.
     */
    private fun onArrowKey(code: Int) {
        val inputConnection = currentInputConnection ?: return
        var keyEvent: KeyEvent? = null
        when (code) {
            BaseKeyboard.KEYCODE_ARROW_UP -> keyEvent = KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_UP
            )

            BaseKeyboard.KEYCODE_ARROW_LEFT -> keyEvent = KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT
            )

            BaseKeyboard.KEYCODE_ARROW_RIGHT -> keyEvent = KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT
            )

            BaseKeyboard.KEYCODE_ARROW_DOWN -> keyEvent = KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN
            )

            BaseKeyboard.KEYCODE_ARROW_HOME -> inputConnection.setSelection(0, 0)
            BaseKeyboard.KEYCODE_ARROW_END -> {
                val inputContent = inputContents
                inputConnection.setSelection(
                    inputContent.length,
                    inputContent.length
                )
            }

            BaseKeyboard.KEYCODE_ARROW_BACK -> cursorBackWord(inputConnection)
            BaseKeyboard.KEYCODE_ARROW_NEXT -> cursorNextWord(inputConnection)
            BaseKeyboard.KEYCODE_CUT -> {
                inputConnection.performContextMenuAction(android.R.id.cut)
                mComposing!!.setLength(0)
                updateCandidates()
            }

            BaseKeyboard.KEYCODE_COPY -> inputConnection.performContextMenuAction(android.R.id.copy)
            BaseKeyboard.KEYCODE_PASTE -> inputConnection.performContextMenuAction(android.R.id.paste)
            BaseKeyboard.KEYCODE_SELECT -> {
                // Select the current word.
                val before: CharSequence = getWordBeforeCursor(inputConnection)
                val after: CharSequence = getWordAfterCursor(inputConnection)
                val cursorLocation: Int
                if (before.isNotEmpty() || after.isNotEmpty()) {
                    cursorLocation = getCursorLocation(inputConnection)
                    inputConnection.setSelection(
                        cursorLocation - before.length,
                        cursorLocation + after.length
                    )
                } else inputConnection
                    .performContextMenuAction(android.R.id.startSelectingText)
            }

            BaseKeyboard.KEYCODE_SELECT_ALL ->                 // Select all text.
                inputConnection.performContextMenuAction(android.R.id.selectAll)
        }
        if (keyEvent != null) inputConnection.sendKeyEvent(keyEvent)
    }

    /**
     * Called by onKey() when the user presses the cancel key.
     */
    protected fun onCancelKey() {
        // Commit composition and hide keyboard.
        commitTyped()
        requestHideSelf(0)
        keyboardView!!.closing()
    }

    /**
     * Perform the default app action (e.g. send).
     */
    fun performEditorAction() {
        val ei = safeGetCurrentInputEditorInfo()

        // Commit the composition first. This will perform a completion or typo-correction if necessary
        commitComposing()

        // Get the action code and send it to the app.
        val editorAction = ei.imeOptions and EditorInfo.IME_MASK_ACTION
        val inputConnection = currentInputConnection
        inputConnection?.performEditorAction(editorAction)
    }

    /**
     * Toggle between alpha and number pad keyboards
     */
    protected fun toggleKeyboardMode() {
        if (isAlphaKeyboard) {
            keyboardView!!.keyboard = mNumPadKeyboard
            keyboardView!!.setShifted(false, false)
        } else {
            if (mNumRowOn) {
                keyboardView!!.keyboard = mAlphaNumKeyboard
            } else {
                keyboardView!!.keyboard = mAlphaKeyboard
            }
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        val currKeyboard = keyboardView!!.keyboard
        currKeyboard!!.setImeOptions(resources, safeGetCurrentInputEditorInfo())
        getTextForImeAction(0)
    }

    /**
     * Change keyboard mode to arrow keys
     */
    protected fun arrowKeyboardMode() {
        dismissAllPopupWindows()
        keyboardView!!.keyboard = mArrowPadKeyboard
    }

    /**
     * Methods to open/close the number row
     */
    protected fun openNumRow() {
        // This only applies to the alpha keyboard
        if (keyboardView!!.keyboard === mAlphaKeyboard) {
            // Display the alternate keyboard and update its state
            mAlphaNumKeyboard!!.setImeOptions(resources, safeGetCurrentInputEditorInfo())
            keyboardView!!.keyboard = mAlphaNumKeyboard
            keyboardView!!.setShifted(mAlphaKeyboard!!.isShifted, keyboardView!!.capsLock)
            mNumRowOn = true
        }
    }

    protected fun closeNumRow() {
        // This only applies to the alpha keyboard
        if (keyboardView!!.keyboard === mAlphaNumKeyboard) {
            // Display the alternate keyboard and update its state
            mAlphaKeyboard!!.setImeOptions(resources, safeGetCurrentInputEditorInfo())
            keyboardView!!.keyboard = mAlphaKeyboard
            keyboardView!!.setShifted(mAlphaNumKeyboard!!.isShifted, keyboardView!!.capsLock)
            mNumRowOn = false
        }
    }

    fun dismissAllPopupWindows() {
        dismissAllPopupWindows(false)
    }

    private fun dismissAllPopupWindows(rememberLastWindow: Boolean) {
        keyboardView!!.dismissAllPopupWindows(rememberLastWindow)
    }

    /**
     * Toggle the number row in the alpha keyboard.
     */
    protected fun toggleNumRow() {
        if (keyboardView!!.keyboard === mAlphaKeyboard) {
            openNumRow()
        } else if (keyboardView!!.keyboard === mAlphaNumKeyboard) {
            closeNumRow()
        }
    }
    /*
	 * Methods to handle customized swipe action
	 */
    /**
     * Called by the framework swipe callbacks (see below).
     *
     * @param action The action to perform.
     */
    private fun swipe(action: String?) {
        Assert.assertTrue(action != null && action != "")
        when (action) {
            getString(R.string.any_key_action_id_voice_input) -> {
                openVoiceTyping()
            }
            getString(R.string.any_key_action_id_arrow_keypad) -> {
                arrowKeyboardMode()
            }
            getString(R.string.any_key_action_id_translator) -> {
                openTranslator()
            }
            getString(R.string.any_key_action_id_locale) -> {
                onLocaleKey()
            }
            getString(R.string.swipe_action_id_delete_word) -> {
                deleteWordBeforeCursor()
            }
            getString(R.string.swipe_action_id_num_row) -> {
                toggleNumRow()
            }
            getString(R.string.swipe_action_id_toggle_mode) -> {
                toggleKeyboardMode()
            }
        }
    }

    override fun swipeDown() {
        if (mSwipeNumberRow) {
            // Perform the default swipe down action (close num row or keyboard)
            if (keyboardView!!.keyboard === mAlphaNumKeyboard) {
                closeNumRow()
            } else if (keyboardView!!.keyboard === mAlphaKeyboard) {
                onCancelKey()
            }
        } else {
            // Perform custom user action
            val action: String
            val sharedPrefs = getSharedPreferences(
                Settings.SETTINGS_FILE, MODE_PRIVATE
            )
            action = sharedPrefs.getString(
                "swipe_down_action",
                getString(R.string.any_key_action_id_arrow_keypad)
            ) ?: getString(R.string.any_key_action_id_arrow_keypad)
            swipe(action)
        }
    }

    override fun swipeUp() {
        if (mSwipeNumberRow) {
            // Perform the default swipe up action (open num row)
            if (keyboardView!!.keyboard === mAlphaKeyboard) {
                openNumRow()
            }
        } else {
            // Perform custom user action
            val sharedPrefs = getSharedPreferences(
                Settings.SETTINGS_FILE, MODE_PRIVATE
            )
            val action = sharedPrefs.getString(
                "swipe_up_action",
                getString(R.string.any_key_action_id_arrow_keypad)
            )
            swipe(action)
        }
    }

    override fun swipeRight() {
        // Perform custom user action
        val sharedPrefs = getSharedPreferences(
            Settings.SETTINGS_FILE, MODE_PRIVATE
        )
        val action = sharedPrefs.getString(
            "swipe_right_action",
            getString(R.string.any_key_action_id_arrow_keypad)
        )
        swipe(action)
    }

    override fun swipeLeft() {
        // Perform custom user action
        val sharedPrefs = getSharedPreferences(
            Settings.SETTINGS_FILE, MODE_PRIVATE
        )
        val action = sharedPrefs.getString(
            "swipe_left_action",
            getString(R.string.any_key_action_id_arrow_keypad)
        )
        swipe(action)
    }

    protected val isAlphaKeyboard: Boolean
        /**
         * Check if we are using the alpha keyboard
         *
         * @return Returns true if keyboard is alpha, false otherwise
         */
        get() = keyboardView!!.keyboard === mAlphaKeyboard || keyboardView!!.keyboard === mAlphaNumKeyboard

    /**
     * Checks if the user double-tapped the caps key
     */
    private fun checkToggleCapsLock() {
        // Switch from caps to caps-lock on double-tap
        val now = System.currentTimeMillis()
        if (now - mLastShiftTime < DOUBLE_CLICK_TIME) {
            keyboardView!!.capsLock = true
            mLastShiftTime = 0
        } else {
            keyboardView!!.capsLock = false
            mLastShiftTime = now
        }
    }

    /**
     * Checks if the user double-tapped the space key
     */
    private fun checkDoubleSpace(): Boolean {
        if (mDoubleSpace == false) {
            return false
        }

        // Insert a period-space on double-tap space bar
        val now = System.currentTimeMillis()
        if (now - mLastSpaceTime < DOUBLE_CLICK_TIME) {
            // Delete the extra space preceding
            val inputConnection = currentInputConnection
            inputConnection?.deleteSurroundingText(1, 0)
            mLastSpaceTime = 0
            return true
        }
        mLastSpaceTime = now
        return false
    }

    fun isWordSeparator(code: Int): Boolean {
        return code > 0 && !isWordCharacter(code)
    }

    fun isSentenceSeparator(code: Int): Boolean {
        return code == '\n'.code || isSmartSpacePreceder(code)
    }

    /**
     * Called by CandidateView if the user tapped a suggestion
     *
     * @param index The index of the suggestion.
     */
    fun touchCandidate(index: Int) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.beginBatchEdit()

        // Complete the selection
        pickSuggestionManually(index)

        // Remember word if it's the first suggestion but not the default.
        if (index == 0 && mSuggestions!!.defaultIndex != 0) {
            val suggestion = mSuggestions!![index]
            rememberWord(suggestion.word)
        }
        if (mSmartSpaces) {
            // Append a space after completion
            val after = getTextAfterCursor(inputConnection, 1)
            if (after.length == 0 || after[0] != ' ') inputConnection.commitText(
                " ",
                1
            ) else  // There is already a word separator following the word.
            // Just move the cursor ahead.
                inputConnection.commitText("", 2)
            bAutoSpace = true
        }
        inputConnection.endBatchEdit()
        updateShiftKeyState()
        updateCandidates()
    }

    /**
     * Inserts the default suggestion.
     *
     * @return The word inserted.
     */
    @SuppressLint("NewApi")
    protected fun pickDefaultCandidate(): CharSequence? {
        if (mSuggestions == null) {
            return mComposing
        }
        val orgWord = mComposing.toString()
        var committedWord: CharSequence?
        committedWord = if (orgWord != mSuggestions!!.composing) {
            orgWord
        } else {
            // Insert the default suggestion
            val defaultSuggestion = mSuggestions!![mSuggestions!!.defaultIndex]
            if (defaultSuggestion == null) {
                orgWord
            } else {
                defaultSuggestion.word
            }
        }

        // If committed word is different from the composing, offer more suggestions in menu.
        val isDifferent =
            !DictionaryUtils.removePunc(committedWord.toString()).lowercase(Locale.getDefault())
                .startsWith(
                    orgWord.lowercase(
                        Locale.getDefault()
                    )
                )
        if (committedWord != null && mComposing != null && isDifferent) {
            // Wrap a SpannableString in a SuggestionSpan containing the original word
            val suggestions = mSuggestions!!.words
            suggestions.add(0, orgWord)
            val words = arrayOfNulls<String>(
                mSuggestions!!.size() + 1
            )
            val span = SuggestionSpan(
                this,
                Locale.getDefault(),
                suggestions.toArray(words),
                SuggestionSpan.FLAG_EASY_CORRECT,
                SuggestionMenuReceiver::class.java
            )
            val suggestion = SpannableString(committedWord)
            suggestion.setSpan(span, 0, committedWord.length, 0)
            committedWord = suggestion

            // If committed word is different from composing, not including punctuation, this is a typo
            val isTypo = !committedWord.toString().lowercase(Locale.getDefault()).startsWith(
                orgWord.lowercase(
                    Locale.getDefault()
                )
            )
            // Long vibrate for typo correction
            if (mVibrateOnTypoCorrection && isTypo) {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VIBRATE_LENGTH_ON_TYPO_CORRECTION.toLong())
                mIsVibrated = true
            }
        }
        commitText(committedWord)
        return committedWord
    }

    /**
     * Insert a particular suggestion.
     *
     * @param index The index of the suggestion to insert.
     * @return The word inserted.
     */
    private fun pickSuggestionManually(index: Int): CharSequence? {
        val committedStr: String?
        if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions!!.size) {
            // Use app completion
            val ci = mCompletions!![index]
            committedStr = ci.text.toString()
            val inputConnection = currentInputConnection
            inputConnection?.commitCompletion(ci)
            if (mCandidateView != null) {
                mCandidateView!!.clear()
            }
            updateShiftKeyState()
        } else if (mPredictionOn && mSuggestions != null && index >= 0 && mSuggestions!!.size() > 0 && index < mSuggestions!!.size()) {
            // Use dictionary suggestion
            val prediction = mSuggestions!![index].word
            committedStr = prediction
            setLastWord(prediction)

            // Commit prediction
            val inputConnection = currentInputConnection
            inputConnection?.commitText(prediction, 1)

            // Reset candidates
            mComposing!!.setLength(0)
        } else if (mComposing!!.length > 0) {
            committedStr = mComposing.toString()
            commitTyped()
        } else {
            committedStr = null
        }
        return committedStr
    }

    /**
     * Returns true if we should append a smart space after we append newChar to
     * composing.
     *
     * @param inputConnection The current InputConnection.
     * @param newChar         The character to check.
     * @return true if a space should be appended after newChar.
     */
    private fun shouldAppendSmartSpace(inputConnection: InputConnection?, newChar: Char): Boolean {
        if (!mSmartSpaces) {
            return false
        }
        if (inputConnection == null) {
            return false
        }
        val textBeforeCursor = getTextBeforeCursor(inputConnection, 3)
        if (textBeforeCursor.length < 1) {
            return false
        }
        val endChar = textBeforeCursor[textBeforeCursor.length - 1]
        if ((newChar == '.' || newChar == ':') && Character.isDigit(endChar)) {
            // User is typing a floating point number, IP address or time
            return false
        }

        // Don't append after www.
        if (textBeforeCursor.length == 3 && textBeforeCursor.toString()
                .lowercase(Locale.getDefault()) == "www" && newChar == '.'
        ) {
            return false
        }
        return if (isSmartSpacePreceder(newChar.code) && isWordCharacter(endChar.code)) {
            true
        } else false
    }

    /**
     * Append a smart space to the composition, if necessary.
     *
     * @param inputConnection The current InputConnection.
     * @param composition     The composition.
     * @param newChar         The character following the composition.
     * @return composing + optional smart space.
     */
    private fun appendSmartSpace(
        inputConnection: InputConnection,
        composition: StringBuilder, newChar: Char
    ): StringBuilder {
        if (!shouldAppendSmartSpace(inputConnection, newChar)) return composition
        composition.append(" ")
        bAutoSpace = true
        return composition
    }

    /**
     * Creates a regex pattern matcher for words and saves it in mWordMatcher.
     */
    private fun createWordMatcher() {
        val wordPattern = Pattern.compile(WORD_REGEX)
        mWordMatcher = wordPattern.matcher("")
    }

    /**
     * Check if a word is a "word". That is, if it is composed entirely of word character.
     *
     * @param word The word to check.
     * @return true if it is a word.
     */
    private fun isWord(word: String): Boolean {
        return mWordMatcher!!.reset(word.lowercase(Locale.getDefault())).matches()
    }

    /**
     * Saves the last word typed. This is used for redo, and look-ahead word prediction.
     *
     * @param word The word to save.
     */
    fun setLastWord(word: CharSequence?) {
        if (!mPredictionOn) {
            return
        }
        val sWord = word.toString()
        mLastWord = if (isWord(sWord)) {
            sWord.lowercase(Locale.getDefault())
        } else {
            null
        }
    }

    /**
     * Returns true if the keyboard is in preview mode. In preview mode,
     * all input is ignored except for the action key, which closes
     * the keyboard.
     *
     * @return true if the keyboard is in preview mode.
     */
    fun inPreviewMode(): Boolean {
        return mInPreviewMode
    }

    fun setPreviewMode(enabled: Boolean) {
        mInPreviewMode = enabled
    }

    private val isMultiLine: Boolean
        /**
         * Checks if this is multi-line input.
         *
         * @return True if it is a multi-line input.
         */
        get() {
            val editorInfo = safeGetCurrentInputEditorInfo()
            return editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE > 0
        }

    /**
     * Called by the framework when a key is pressed. Similar to onKey(), but
     * only called once for repeatable keys.
     */
    override fun onPress(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") code: Int) {
        // Keep track of multi keypresses to avoid vibration on multipress
        if (isRepeatable(code)) {
            mFirstRepeatablePress = true
        }
    }

    override fun onRelease(primaryCode: Int) {
        // Do nothing.
    }

    /**
     * Checks if this is a repeatable key.
     *
     * @param code The key code to check.
     * @return true if it is a repeatable key.
     */
    private fun isRepeatable(code: Int): Boolean {
        // TODO: Can we use android:isRepeatable instead?
        return if (code == Keyboard.KEYCODE_DELETE || code == 32 || code == BaseKeyboard.KEYCODE_ARROW_BACK || code == BaseKeyboard.KEYCODE_ARROW_NEXT || code == BaseKeyboard.KEYCODE_ARROW_UP || code == BaseKeyboard.KEYCODE_ARROW_DOWN || code == BaseKeyboard.KEYCODE_ARROW_LEFT || code == BaseKeyboard.KEYCODE_ARROW_RIGHT || code == BaseKeyboard.KEYCODE_DEL_FORWARD
        ) {
            true
        } else false
    }

    val candidateHeight: Int
        /**
         * Gets the height of the CandidateView.
         *
         * @return The height of the CandidateView, in (device?) pixels.
         */
        get() {
            var height = 0
            if (mCandidateView != null && mCandidateView!!.isShown) {
                height = mCandidateView!!.height
            }
            return height
        }

    /**
     * Updates the currency keys on all keyboards with the user's currency
     * symbol.
     */
    protected fun updateCurrencyKeys() {
        updateCurrencyKeys(mAlphaKeyboard)
        updateCurrencyKeys(mAlphaNumKeyboard)
        updateCurrencyKeys(mNumPadKeyboard)
    }

    /**
     * Updates the currency keys on a keyboard with the user's currency symbol.
     *
     * @param keyboard The keyboard to update.
     */
    protected fun updateCurrencyKeys(keyboard: Keyboard?) {
        val key = (keyboard as BaseKeyboard?)!!.mCurrencyKey ?: return
        if (keyboard === mNumPadKeyboard) {
            // This is the numpad keyboard. Update currency key label and popup
            // menu
            key.label = mDefaultCurrency
            key.codes[0] = mDefaultCurrency!![0].code
            key.popupCharacters =
                getString(R.string.currency_keys).replace(mDefaultCurrency.toString(), "")
            BaseKeyboard.checkForNulls("updateCurrencyKeys()", key)
        } else {
            // This is a regular keyboard. Update currency key long-press symbol
            key.popupCharacters = mDefaultCurrency
        }
    }

    fun launchDictionariesUpdate() {
        val t = ArrayList<DictionaryItem>()
        DatabaseHelper.safeGetDatabaseHelper(this).getDictionariesForUpdate(this, t)
        val list = arrayOfNulls<String>(t.size)
        for (i in t.indices) {
            val item = t[i]
            list[i] = item.lang
        }
        launchDictionariesUpdate(list)
    }

    /**
     * Launches all dictionaries updates
     */
    fun launchDictionariesUpdate(list: Array<String?>?) {
        val intent = Intent(this, DictionaryDownloader::class.java)
        val bundle = Bundle()
        bundle.putStringArray(DictionaryDownloader.LANG_LIST, list)
        intent.putExtra(Settings.BUNDLE_KEY, bundle)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        DictionaryDownloader.setOnResultListener(object : OnResultListener {
            override fun onSuccess() {
                KeyboardApp.getApp().updater.markAsReadAll()
                KeyboardApp.getApp().updater.saveDicUpdatedTime(Utils.getTimeMilis())
                KeyboardApp.getApp().removeNotificationTry()
                clearCandidates()
                suggestor!!.reloadLanguageDictionary()
            }

            override fun onFail() {
                writeDebug("failed")
            }
        })
        startActivity(intent)
    }

    /**
     * Send keystroke audio and haptic feedback.
     *
     * @param code The code of the keystroke.
     */
    fun keyFeedback(code: Int) {
        if (mVibrateLength > 0) {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(mVibrateLength.toLong())
        }
        if (mSoundVolume > 0) {
            if (code == 32) {
                // Spacebar
                mSoundPool!!.play(
                    mSoundsMap!![SOUND_CLICK], mSoundVolume,
                    mSoundVolume, 1, 0, 1f
                )
            } else {
                mSoundPool!!.play(
                    mSoundsMap!![SOUND_CLICK], mSoundVolume,
                    mSoundVolume, 1, 0, 1f
                )
            }
        }
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        callTrace("onConfigurationChanged()")
        if (mLastKeyboardState != null) {
            mLastKeyboardState!!.saveKeyboardState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callTrace("onDestroy()")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        callTrace("onLowMemory()")
    }

    override fun toString(): String {
        return toString("KeyboardService")
    }

    fun setSoundVolume(`val`: Float) {
        mSoundVolume = `val`
    }

    private fun callTrace(call: String) {
        if (callTrace.size > 250) {
            callTrace.clear()
            callTrace.add("...truncated")
        }
        callTrace.add(call)
        writeDebug("KeyboardService.$call")
    }

    fun writeInputDebug() {
        writeDebug("====================================================================================================================================================")
        writeDebug(mInputContents)
        writeDebug("---------------------------------------------------------")
    }

    fun toString(name: String): String {
        val buffer = StringBuilder()
        buffer.append(javaClass.name)
        val subName = name + "\t"
        Utils.appendLine(buffer, name, "{")

        // Add important fields
        Utils.appendLine(buffer, subName, "mKeyboardType = $mKeyboardLayoutId")
        Utils.appendLine(buffer, subName, "mKeyboardLayout = $mKeyboardLayout")
        Utils.appendLine(buffer, subName, "mKeyboardView" + " = " + keyboardView)
        if (keyboardView != null) Utils.appendLine(
            buffer,
            subName,
            "mKeyboardView.getKeyboard()" + " = " + keyboardView!!.keyboard
        )
        Utils.appendLine(buffer, subName, "mCandidateView = $mCandidateView")
        Utils.appendLine(buffer, subName, "mCompletions = $mCompletions")
        Utils.appendLine(buffer, subName, "mPredictions = $mSuggestions")
        Utils.appendLine(
            buffer,
            subName,
            "mIsAlarmMessageFirstAppeared = $mIsAlarmMessageFirstAppeared"
        )
        Utils.appendLine(buffer, subName, "mLastWord = $mLastWord")
        Utils.appendLine(buffer, subName, "mWordMatcher = $mWordMatcher")
        Utils.appendLine(buffer, subName, "mWindowVisible = $mWindowVisible")
        Utils.appendLine(buffer, subName, "mPrefsChanged = $mPrefsChanged")
        Utils.appendLine(buffer, subName, "mComposing = $mComposing")
        Utils.appendLine(buffer, subName, "mLastDisplayWidth = $mLastDisplayWidth")
        Utils.appendLine(buffer, subName, "mLastShiftTime = $mLastShiftTime")
        Utils.appendLine(buffer, subName, "mLastSpaceTime = $mLastSpaceTime")
        Utils.appendLine(buffer, subName, "mMetaState = $mMetaState")
        Utils.appendLine(buffer, subName, "bAutoSpace = $bAutoSpace")
        Utils.appendLine(buffer, subName, "mIsPassword = $mIsPassword")
        Utils.appendLine(buffer, subName, "mURL = $mURL")
        Utils.appendLine(buffer, subName, "mNumRowOn = $mNumRowOn")
        // Utils.appendLine(buffer, subComposing, "mWordSeparators" + " = " +
        // mWordSeparators);
        Utils.appendLine(buffer, subName, "mSmartSpacePreceders" + " = " + smartSpacePreceders)
        Utils.appendLine(buffer, subName, "suggestor" + " = " + suggestor)
        Utils.appendLine(buffer, subName, "mVoiceIsShifted = $mVoiceIsShifted")
        Utils.appendLine(buffer, subName, "mVoiceCapsLock = $mVoiceCapsLock")
        Utils.appendLine(buffer, subName, "mLang" + " = " + language)
        Utils.appendLine(buffer, subName, "mTheme = $mTheme")
        Utils.appendLine(buffer, subName, "mDefaultCurrency = $mDefaultCurrency")

        // Add KeyboardView
        if (keyboardView != null) {
            Utils.appendLine(buffer, subName, "mKeyboardView = " + keyboardView!!.toString(subName))
        }
        if (mAlphaKeyboard != null) {
            Utils.appendLine(buffer, subName, "mAlphaKeyboard = ")
            Utils.append(
                buffer, subName,
                (mAlphaKeyboard as BaseKeyboard).toString(subName)
            )
        }
        if (mNumPadKeyboard != null) {
            Utils.appendLine(buffer, subName, "mNumPadKeyboard = ")
            Utils.appendLine(buffer, subName, (mNumPadKeyboard as BaseKeyboard).toString(subName))
        }
        if (mAlphaNumKeyboard != null) {
            Utils.appendLine(buffer, subName, "mAlphaNumKeyboard = ")
            Utils.appendLine(buffer, subName, (mAlphaNumKeyboard as BaseKeyboard).toString(subName))
        }

        // Add all native fields
        Utils.appendLine(buffer, subName, "")
        Utils.appendLine(
            buffer, subName,
            Utils.getClassString(this, subName)
        )
        Utils.appendLine(buffer, name, "}")
        return buffer.toString()
    }

    /**
     * KeyboardState stores the current keyboard state in case it needs to be
     * re-initialized.
     *
     * @author Barry Fruitman
     */
    inner class KeyboardState {
        // Keyboard
        var keyboardResID = -1
        var bShifted = false
        var mCapsLock = false

        // Popup window
        var popupLayoutID = -1
        var popupKeyboardID = -1
        var popupKeyboardParm: Any? = null

        // Candidate View
        var isCandidateViewShown = false
        protected var orgWord: String? = null
        var suggestions: FinalSuggestions? = null
        protected var completions = false
        var mChanged = false
        var mNeedToUpdate = false

        // TODO: Add portrait and landscape num row state
        init {
//            if (keyboardView == null) {
//                return
//            }
            saveKeyboardState()
        }

        fun saveKeyboardState() {
            if (keyboardView == null) {
                return
            }

            // Keyboard State
            if (keyboardView != null) {
                keyboardResID = keyboardView!!.keyboard!!.xmlResID
                bShifted = keyboardView!!.keyboard!!.isShifted
            }
            mCapsLock = keyboardView!!.capsLock

            // / Popup Keyboard Status
            if (keyboardView != null) {
                popupLayoutID = keyboardView!!.currPopupLayoutID
                popupKeyboardID = keyboardView!!.currPopupKeyboardID
                if (keyboardView!!.currPopupLayoutID == R.id.main_menu) popupKeyboardParm =
                    keyboardView!!.keyboard!!.anyKey
            }
            mChanged = true
        }

        // Reset suggestions
        fun resetSuggestions() {
            suggestions = null
            completions = false
            orgWord = null
        }

        // Save current suggestions
        fun saveSuggestions(
            suggestions: FinalSuggestions?,
            completions: Boolean
        ) {
            this.suggestions = suggestions
            this.completions = completions
        }
    }

    // End of class
    class DebugTracer : ProfileTracer() {
        override fun write(message: String) {
            writeDebug(message)
        }
    }

    companion object {
        // THIS IS TERRIBLE
        lateinit var IME: KeyboardService

        private const val MAX_INPUT_LENGTH = 499
        private const val MAX_WORD_LENGTH = 32
        private const val VIBRATE_LENGTH_ON_TYPO_CORRECTION = 100

        // click
        const val SOUND_CLICK = 0
        private const val PROCESS_HARD_KEYS = false

        // DEBUG
        @JvmField
        var mDebug = false
        @JvmStatic
        fun loadSounds(ctx: Context?, soundPool: SoundPool): SparseIntArray {
            val soundMap = SparseIntArray()
            soundMap.put(SOUND_CLICK, soundPool.load(ctx, R.raw.key_click_default, 1))
            return soundMap
        }

        // Backwards-compatible implementation of
        // InputConnection.setComposingRegion()
        private var mInputConnection_setComposingRegion: Method? = null

        init {
            initCompatibility()
        }

        private fun initCompatibility() {
            try {
                mInputConnection_setComposingRegion = InputConnection::class.java
                    .getMethod(
                        "setComposingRegion", *arrayOf<Class<*>?>(
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                    )
                /* success, this is a newer device */
            } catch (nsme: NoSuchMethodException) {
                /* failure, must be older device */
            }
        }

        @JvmStatic
        @SuppressLint("SimpleDateFormat")
        fun writeDebug(message: CharSequence) {
            if (mDebug) {
                Log.v(KeyboardApp.LOG_TAG, message.toString())
                try {
                    // Prepare message
                    val now = SimpleDateFormat("dd-MMM-yyyy kk:mm:ss.SSS").format(Date())
                    val entry = "$now: $message\n"

                    // Open log file
                    val logWriter = OutputStreamWriter(
                        IME.openFileOutput("debug.log", MODE_APPEND)
                    )

                    // Write message to log file and close
                    logWriter.write(entry, 0, entry.length)
                    logWriter.flush()
                    logWriter.close()
                } catch (e: IOException) {
                    Log.e(KeyboardApp.LOG_TAG, e.message!!)
                }
            }
        }
    }
}
