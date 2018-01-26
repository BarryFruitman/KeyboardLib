/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.MetaKeyKeyListener;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.comet.keyboard.Suggestor.Suggestion;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.dictionary.DictionaryUtils;
//import com.comet.keyboard.dictionary.UserDB;
import com.comet.keyboard.dictionary.updater.DictionaryDownloader;
import com.comet.keyboard.dictionary.updater.DictionaryItem;
import com.comet.keyboard.install.Installer;
import com.comet.keyboard.install.LanguageSelector;
import com.comet.keyboard.install.Installer.InstallStep;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.models.KeyHeight;
import com.comet.keyboard.settings.AppRater;
import com.comet.keyboard.settings.CandidateHeightSetting;
import com.comet.keyboard.settings.KeyHeightSetting;
import com.comet.keyboard.settings.KeyPaddingHeightSetting;
import com.comet.keyboard.settings.KeyboardPaddingBottomSetting;
import com.comet.keyboard.settings.LanguageProfile;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.OnResultListener;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.settings.SoundVolumeSetting;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.OnPopupMenuItemClickListener;
import com.comet.keyboard.util.PopupMenuView;
import com.comet.keyboard.util.ProfileTracer;
import com.comet.keyboard.util.Utils;
import com.google.android.voiceime.VoiceRecognitionTrigger;

/**
 * KeyboardService represents the IME service and is the "core" of the keyboard.
 * It implements KeyboardView.OnKeyboardActionsListener to handle all key events
 * except for long-presses, which are handled by KeyboardView.
 *
 * @author Barry Fruitman
 */
public class KeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    static private KeyboardService mIME;
    static private final int MAX_INPUT_LENGTH = 499;
    static private final int MAX_WORD_LENGTH = 32;
    static private final int VIBRATE_LENGTH_ON_TYPO_CORRECTION = 100;

    // click
    static public final int SOUND_CLICK = 0;

    static private final boolean PROCESS_HARD_KEYS = false;

    protected KeyboardView mKeyboardView = null;
    protected View mKeyboardLayout = null;
    protected CandidateView mCandidateView;
    protected CompletionInfo mCompletions[];
    private Suggestions mSuggestions;

    private SoundPool mSoundPool;
    private SparseIntArray mSoundsMap;
    private float mSoundVolume;

    // Configuration
    protected KeyboardState mLastKeyboardState = null;

    private boolean mIsAlarmMessageFirstAppeared = false;

    private boolean mInputViewCreated = false;

    protected final int NO_DEFAULT_CANDIDATE = -1;

    private String mLastWord = null;
    private final String WORD_REGEX = "[a-zA-Z0-9']*";
    private Matcher mWordMatcher;

    protected boolean mWindowVisible = false;
    protected boolean mPrefsChanged = true;
    protected String mKeyboardLayoutId;

    protected StringBuilder mComposing = new StringBuilder();
    protected StringBuilder mPassword = new StringBuilder();
    private int mLastPasswordId = -1;
    protected StringBuilder mLastPassword = new StringBuilder();

    // Keyboard states
    protected boolean mPredictionOn;
    protected boolean mCompletionOn;
    protected boolean bAutoSpace = false;
    protected boolean mIsPassword = false;
    protected boolean mShowPassword = false;
    protected boolean mURL = false;
    protected boolean mLearnWords = false;
    protected String mAnyKeyState;
    protected String mAnyKeyLongState;
    protected String mReturnKeyLongState;
    protected long mMetaState;

    protected int mLastDisplayWidth;
    protected long mLastShiftTime;
    protected long mLastSpaceTime;

    // The various keyboards that can be displayed
    protected BaseKeyboard mArrowPadKeyboard;
    protected BaseKeyboard mNumPadKeyboard;
    protected BaseKeyboard mAlphaKeyboard;
    protected BaseKeyboard mAlphaNumKeyboard;
    protected boolean mNumRowOn = false;

    protected String mSmartSpacePreceders;
    public Suggestor mSuggestor;

    private boolean mVoiceIsShifted = false;
    private boolean mVoiceCapsLock = false;

    // User prefs
    protected boolean mSmartSpaces;
    protected boolean mDoubleSpace;
    protected boolean mShowSuggestions;
    protected boolean mAutoSelect;
    protected boolean mAutoCaps;
    protected int mVibrateLength;
    protected boolean mVibrateOnTypoCorrection;
    protected boolean mSoundKey;
    protected boolean mPredictNextWord;
    protected boolean mShowPopupKey;
    protected boolean mSwipeNumberRow;
    private boolean mSuperLabelEnabled = true;
    protected String mBehaviorLocaleButton;

    protected String mInputContents = "";
    protected String mStartInputContents = "";

    // Ugh
    private boolean mGoogleMailHackCapsMode = false;
    private boolean mNeedUpgradeApp = false;
    private boolean mNeedUpdateDicts = false;

    private String mLanguage;
    public String mTheme;
    protected CharSequence mDefaultCurrency;

    private ArrayList<String> mCallTrace = new ArrayList<String>();

    private boolean mInPreviewMode = false;
    private boolean mIsVibrated = false;
    private boolean mFirstRepeatablePress = false;
    private final int DOUBLE_CLICK_TIME = 500; // Max # of milliseconds between
    // two clicks to count as
    // double-tap

    // Purchase
    private static final int TRANS_PURCHASE_INCREASE_UNIT = 200;

    // If true, uses Dragon in pre-4.0 devices. Otherwise uses Google voice API.
    private boolean mUseDragon = false;

    // Google Voice Typing. This is the default voice input in 4.0 and higher devices.
    VoiceRecognitionTrigger mVoiceRecognitionTrigger;


    // DEBUG
    public static boolean mDebug = false;
    private boolean mPendingRequest = false;
//	public static DebugTracer mProfileTracer = new DebugTracer();

    public static KeyboardService getIME() {
        return mIME;
    }

    public Handler getVoiceHandler() {
        return mVoiceHandler;
    }

    public Suggestor getSuggestor() {
        return mSuggestor;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public boolean isPredictNextWord() {
        return mPredictNextWord;
    }

    /**
     * Message handler for voice input
     */
    protected Handler mVoiceHandler = new Handler() {
        @Override
        public void handleMessage(Message result) {
            @SuppressWarnings("unchecked")
            ArrayList<String> sentences = (ArrayList<String>) result.obj;

            // Check closest match option
            boolean isCheckedClosestMatch = false;
            SharedPreferences sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
            isCheckedClosestMatch = sharedPrefs.getBoolean("speech_input_closest_match", true);

            String mVoiceText = "";
            if (sentences != null && sentences.size() > 0) {
                if (isCheckedClosestMatch) {
                    mVoiceText = sentences.get(0);

                    inputVoiceResult(mVoiceText);
                } else {
                    // Show select menu
                    PopupMenuView mPopupMenu = new PopupMenuView(
                            (ViewGroup) KeyboardService.mIME.mKeyboardLayout,
                            sentences, R.string.popup_menu_closest_match_title,
                            R.drawable.ic_launcher);
                    mPopupMenu.setOnPopupMenuItemClickListener(mWordMenuListener);
                    mPopupMenu.showContextMenu();
                }
            }
        }
    };

    /**
     * Listener for events from the voice results menu
     */
    private OnPopupMenuItemClickListener mWordMenuListener = new OnPopupMenuItemClickListener() {
        @Override
        public void onPopupMenuItemClicked(int position, String title) {
            inputVoiceResult(title);
        }
    };

    /**
     * Inputs text from voice input result(s)
     *
     * @param text
     */
    public void inputVoiceResult(String text) {
        text = text.toLowerCase() + " ";
        bAutoSpace = true;
        if (mVoiceCapsLock)
            text = text.toUpperCase();
        else if (mVoiceIsShifted)
            text = DictionaryUtils.capFirstChar(text);

        appendText(text);

        updateShiftKeyState();
    }

    /**
     * Main initialization of the input method component. Be sure to call to
     * super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        callTrace("onCreate()");

        mSmartSpacePreceders = getResources().getString(R.string.smart_space_preceders);
        // android.os.Debug.waitForDebugger();

        // Set the instance variable
        mIME = this;

        // Load preferences
        SharedPreferences sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
        mDefaultCurrency = sharedPrefs.getString("currency", getString(R.string.curr_symbol_default));
        mKeyboardLayoutId = KeyboardLayout.getCurrentLayout().getId();
        mDebug = sharedPrefs.getBoolean("debug_mode", false);

        Log.v(KeyboardApp.LOG_TAG, "debug mode = " + mDebug);

        // Create suggestor
        mSuggestor = Suggestor.getSuggestor();

        // Sound
        mSoundPool = new SoundPool(4, SoundVolumeSetting.STREAM_TYPE, 100);
        mSoundsMap = loadSounds(this, mSoundPool);

        mSoundVolume = SoundVolumeSetting.getVolumePreference(this);

        mNeedUpgradeApp = KeyboardApp.getApp().getUpdater().isNeedUpgrade();
        mNeedUpdateDicts = KeyboardApp.getApp().getUpdater().isNeedUpdate();

        // Creates a regex pattern matcher for words
        createWordMatcher();

        mVoiceRecognitionTrigger = new VoiceRecognitionTrigger(this);
    }


    public static SparseIntArray loadSounds(Context ctx, SoundPool soundPool) {
        SparseIntArray soundMap = new SparseIntArray();
        soundMap.put(SOUND_CLICK, soundPool.load(ctx, R.raw.key_click_default, 1));
        return soundMap;
    }


    /**
     * This is the point where you can do all of your UI initialization. It is
     * called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        callTrace("onInitializeInterface()");

        if (mLastKeyboardState == null)
            mLastKeyboardState = new KeyboardState();

        if (mAlphaKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth)
                return;
            mLastDisplayWidth = displayWidth;
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return KeyHeightSetting.isFullScreenMode(this);
    }

    /**
     * Gets the resource id of the current keyboard.
     *
     * @return A resource id.
     */
    public int getKeyboardID() {
        int keyboardResID;

        BaseKeyboard keyboard = mKeyboardView.getKeyboard();
        keyboardResID = keyboard.getXMLResID();

        return keyboardResID;
    }

    /**
     * Sets the current keyboard by resource id.
     *
     * @param keyboardResID A resource id.
     * @return The Keyboard object (should be an instance of BaseKeyboard).
     */
    public Keyboard setKeyboard(int keyboardResID) {
        if (keyboardResID == mNumPadKeyboard.getXMLResID()) {
            mKeyboardView.setKeyboard(mNumPadKeyboard);
        } else if (keyboardResID == mArrowPadKeyboard.getXMLResID()) {
            mKeyboardView.setKeyboard(mArrowPadKeyboard);
        } else if (keyboardResID == mAlphaKeyboard.getXMLResID()) {
            mKeyboardView.setKeyboard(mAlphaKeyboard);
        } else {
            Assert.assertTrue(keyboardResID == mAlphaNumKeyboard.getXMLResID());
            mKeyboardView.setKeyboard(mAlphaNumKeyboard);
        }

        return mKeyboardView.getKeyboard();
    }

    /**
     * Create the keyboards used in this view
     */
    protected void createKeyboards() {
        if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwerty_sp))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_es);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_es_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwerty_intl))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_intl);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_intl_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_azerty_fr))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.azerty_fr);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.azerty_fr_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwerty_sl))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_sl);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_sl_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwerty_sv))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_sv);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_sv_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_azerty_be))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.azerty_be);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.azerty_be_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwertz_de))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwertz_de);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwertz_de_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_qwertz_sl))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwertz_sl);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwertz_sl_num);
        } else if (mKeyboardLayoutId.equals(getString(R.string.kb_id_t9))) {
            mAlphaKeyboard = constructMainKB(this, R.xml.t9);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.t9);
        } else {
            mAlphaKeyboard = constructMainKB(this, R.xml.qwerty_en);
            mAlphaNumKeyboard = constructMainKB(this, R.xml.qwerty_en_num);
        }
        mNumPadKeyboard = constructMainKB(this, R.xml.num_pad);
        mArrowPadKeyboard = constructMainKB(this, R.xml.arrow_keypad);

        updateCurrencyKeys();
    }

    /**
     * Update keyboard view
     */
    public void updateKeyboardView() {
        if (mKeyboardView == null)
            return;

        mKeyboardView.setKeyboard(mKeyboardView.getKeyboard());
        mKeyboardView.invalidate();
        mCandidateView.updateView();
    }

    /**
     * Update key height
     */
    public void updateKeyHeight() {
        // Get current keyboard height
        KeyHeight keyHeight = KeyHeightSetting.getKeyHeightPreference(this);
        int[] bottomGap = KeyboardPaddingBottomSetting
                .getKeyboardPaddingBottomPreference(this);

        if (mAlphaNumKeyboard != null) {
            mAlphaNumKeyboard.setBottomGap(bottomGap);
            mAlphaNumKeyboard.setKeyboardHeight(this, keyHeight);
        }

        if (mAlphaKeyboard != null) {
            mAlphaKeyboard.setBottomGap(bottomGap);
            mAlphaKeyboard.setKeyboardHeight(this, keyHeight);
        }
        if (mNumPadKeyboard != null) {
            mNumPadKeyboard.setBottomGap(bottomGap);
            mNumPadKeyboard.setKeyboardHeight(this, keyHeight);
        }
        if (mArrowPadKeyboard != null) {
            mArrowPadKeyboard.setBottomGap(bottomGap);
            mArrowPadKeyboard.setKeyboardHeight(this, keyHeight);
        }

        if (mKeyboardView != null) {
            // set changed flag: hacked from KeyboardView.java
            mKeyboardView.setKeyboard(mKeyboardView.getKeyboard());
            mKeyboardView.invalidate();
        }
    }

    /**
     * Update keys padding height
     */
    public void updateKeyPaddingHeight() {
        // Get current keyboard height
        int keyPaddingHeight = KeyPaddingHeightSetting
                .getKeyPaddingHeightPreference(this);

        // Update all the keyboards.
        if (mAlphaKeyboard != null)
            mAlphaKeyboard.setKeyboardPaddingHeight(this, keyPaddingHeight);
        if (mAlphaNumKeyboard != null)
            mAlphaNumKeyboard.setKeyboardPaddingHeight(this, keyPaddingHeight);
        if (mAlphaNumKeyboard != null)
            mAlphaNumKeyboard.setKeyboardPaddingHeight(this, keyPaddingHeight);

        if (mKeyboardView != null) {
            // set changed flag: hacked from KeyboardView.java
            mKeyboardView.setKeyboard(mKeyboardView.getKeyboard());
        }
    }

    /**
     * Update bottom padding of keyboard
     */
    public void updateKeyboardBottomPaddingHeight() {
        int left, right, bottom, top;

        if (getKeyboardView() == null)
            return;

        left = getKeyboardView().getPaddingLeft();
        right = getKeyboardView().getPaddingRight();
        top = getKeyboardView().getPaddingTop();
        bottom = getKeyboardView().getPaddingBottom();

        getKeyboardView().setPadding(left, right, top, bottom);
        updateKeyHeight();

        if (mKeyboardView != null) {
            // set changed flag: hacked from KeyboardView.java
            mKeyboardView.setKeyboard(mKeyboardView.getKeyboard());
        }
    }

    /**
     * Constructs a new BaseKeyboard.
     *
     * @param context The context (this class).
     * @param resId   The keyboard resource id.
     * @return The BaseKeyboard object.
     */
    protected BaseKeyboard constructMainKB(Context context, int resId) {
        return new BaseKeyboard(context, resId);
    }

    /**
     * Called by the framework when your view for creating input needs to be
     * generated. This will be called the first time your input method is
     * displayed, and every time it needs to be re-created such as due to a
     * configuration change.
     */
    @Override
    public View onCreateInputView() {
        callTrace("onCreateInputView()");

        // Create keyboards and layouts
        createKeyboards();
        createKeyboardLayout();

        // HACK. Used to catch and report a mystery exception.
        ViewParent parent = mKeyboardLayout.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            reportError_192(new NullPointerException().fillInStackTrace());

            ViewGroup group = (ViewGroup) parent;
            group.removeView(mKeyboardLayout);
        }

        mInputViewCreated = true;

        return mKeyboardLayout;
    }

    /**
     * Temporary method used to report a mystery exception. (Is it still
     * necessary?)
     *
     * @param e The exception.
     */
    public void reportError_192(Throwable e) {

        ErrorReport errorReport = new ErrorReport(this, e, "192");

        try {
            errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
            errorReport.putRunningProcesses();
            errorReport.putInstalledPackages();
            errorReport.putMainObjects();
        } catch (Exception e2) {
            errorReport.putParam("meta_error", e2.toString());
        }

        errorReport.post();
    }

    /**
     * Called by onCreateInputView() to create the layout views and assign the
     * keyboard view.
     *
     * @return
     */
    protected View createKeyboardLayout() {
        // Return the keyboard view if it is already inflated
        // if(mKeyboardView != null)
        // return mKeyboardView;

        // Inflate the keyboard layout
        mKeyboardLayout = (View) getLayoutInflater().inflate(R.layout.keyboard_layout, null);

        // Find the keyboard view
        mKeyboardView = (KeyboardView) mKeyboardLayout.findViewById(R.id.keyboard_view);

        // Assign a key listener
        mKeyboardView.setOnKeyboardActionListener(this);

        mCandidateView = (CandidateView) mKeyboardLayout.findViewById(R.id.candidate);

        // Assign the keyboard
        if (mLastKeyboardState.mChanged/* && lastConfiguration != null */) {
            // Restore status
            setKeyboard(mLastKeyboardState.keyboardResID);
            mKeyboardView.setCapsLock(mLastKeyboardState.mCapsLock);
            mKeyboardView.setShifted(mLastKeyboardState.bShifted,
                    mLastKeyboardState.mCapsLock);
            setCandidatesViewShown(mLastKeyboardState.isCandidateViewShown);
        } else {
            mKeyboardView.setKeyboard(mAlphaKeyboard);
            setCandidatesViewShown(false);
        }

        return mKeyboardView;
    }

    /**
     * Gets the keyboard view for this service.
     *
     * @return The keyboard view.
     */
    public KeyboardView getKeyboardView() {
        return (KeyboardView) mKeyboardView;
    }


    /**
     * Called when the keyboard size (may have) changed.
     */
    public void onKeyboardSizeChanged() {
        int w, h, oldW, oldH;

        w = oldW = getKeyboardView().getWidth();
        h = oldH = getKeyboardView().getWidth();

        getKeyboardView().onSizeChanged(w, h, oldW, oldH);
    }

    // THIS METHOD IS A HACK BECAUSE THE LANGUAGE MENU ITEMS DO NOT SIGNAL
    // THE OnItemClickListener IN TranslatorView :(
    public void onClickLanguageMenuItem(View item) {
        getKeyboardView().mTranslatorView
                .onClickLanguageMenuItem((TextView) item);
    }

    /**
     * Listener for the locale (i.e. language) menu.
     */
    private OnPopupMenuItemClickListener mLocaleMenuListener = new OnPopupMenuItemClickListener() {
        @Override
        public void onPopupMenuItemClicked(int position, String title) {
            // Get the language code from the language name (e.g. "en" from
            // "English")
            String code = LanguageSelector.getCodeFromName(KeyboardService.this, title);
            // Update the language profile
            LanguageProfileManager.getProfileManager().setCurrentProfile(code);

            // Restart the input view. This updates the keyboard layout.
            onFinishInputView(true);
            onStartInputView(safeGetCurrentInputEditorInfo(), true);

            // Notify the user
            String toastMsg = String.format(
                    KeyboardService.this.getResources().getString(R.string.selected_locale_confirm_message), title);
            Toast.makeText(KeyboardService.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Callback for all the back buttons in the various popup windows (Any Key
     * menu, symbols menu, translator)
     *
     * @param v The button view.
     */
    public void onClickPopupBack(View v) {
        dismissAllPopupWindows();
    }

    /**
     * Callback for the Comet logo.
     *
     * @param activity The activity that displayed the logo.
     */
    public static void onClickComet(Activity activity) {
        Uri uri = Uri.parse("http://m.cometapps.com/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }

    /**
     * Callback for the share button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickShare(View v) {
        dismissAllPopupWindows();

        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, String
                .format(getString(R.string.share_to_sns_title),
                        getString(R.string.ime_name)));
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, String.format(
                getString(R.string.share_to_sns_description),
                getString(R.string.ime_name), KeyboardApp.getApp().getAppStoreUrl()));

        Intent chooserIntent = Intent.createChooser(shareIntent, "Share with");
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooserIntent);
    }

    /**
     * Callback for the voice key.
     */
    protected void onVoiceKey() {
        openVoiceTyping();
    }

    /**
     * Callback for the voice button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickVoiceInput(View v) {
        openVoiceTyping();
    }

    /**
     * Opens the voice input activity.
     */
    private void openVoiceTyping() {
        if (mVoiceRecognitionTrigger.isInstalled())
            mVoiceRecognitionTrigger.startVoiceRecognition();
    }


    /**
     * Kills all activities. This is to prevent other activities from covering
     * the settings activity when the user opens settings. (Is there a better
     * way to do this???)
     */
    protected void killActivities() {
        Intent killIntent = new Intent("killMyActivity");
        killIntent.setType("text/plain");
        sendBroadcast(killIntent);
    }

    /**
     * Callback for the translator key.
     */
    protected void onTranslatorKey() {
        openTranslator();
    }

    /**
     * Callback for the translator button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickTranslator(View v) {
        openTranslator();
    }

    /**
     * Opens the translator window.
     */
    private void openTranslator() {
        mKeyboardView.openTranslator();
    }

    /**
     * Callback for the settings key.
     */
    protected void onSettingsKey() {
        launchSettings();
    }

    /**
     * Callback for the main menu key.
     */
    protected void onMainMenuKey() {
        openMainMenu();
    }


    /**
     * Opens main menu
     */
    protected void openMainMenu() {
        if (mKeyboardView == null)
            return;

        mKeyboardView.openMainMenu();
    }

    /**
     * Callback for the settings button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickSettings(View v) {
        launchSettings();
    }

    /**
     * Launches the settings activity.
     */
    protected void launchSettings() {
        dismissAllPopupWindows();

        // Launch settings
        Intent intent = new Intent();
        intent.setAction(getString(R.string.settings_intent));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Callback for the URL key.
     *
     * @param v The button view.
     */
    public void onClickUrlKey(View v) {
        openUrlMenu();
    }

    /**
     * Opens the URL menu.
     */
    private void openUrlMenu() {
        getKeyboardView().openUrlKeyboard();
    }

    /**
     * Callback for the symbols main menu key.
     */
    protected void onSymMenuKey() {
        openSymMenuMenu();
    }

    /**
     * Opens the symbols main menu.
     */
    protected void openSymMenuMenu() {
        mKeyboardView.openSymMenu();
    }

    /**
     * Callback for the emoji key.
     */
    protected void onEmojiKey() {
        writeDebug("KeyboardService.onEmojiKey()");
        openEmojiMenu();
    }

    /**
     * Opens the emoji menu.
     */
    protected void openEmojiMenu() {
        writeDebug("KeyboardService.openEmojiMenu()");
        mKeyboardView.openEmojiKeyboard();
    }

    /**
     * Callback for the arrow keypad key.
     */
    protected void onArrowKeypadKey() {
        arrowKeyboardMode();
    }

    /**
     * Callback for the arrow keypad button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickArrowKeypad(View v) {
        arrowKeyboardMode();
    }

    /**
     * Callback for the language key.
     */
    private Toast mRotateToast = null;

    public void onLocaleKey() {
        dismissAllPopupWindows(true);

        // If we don't have any language profile, we should show error.
        LanguageProfile nextProfile = LanguageProfileManager.getProfileManager().getNextProfile();
        if (nextProfile == null) {
            String toastMsg = getString(R.string.selected_locale_no_profiles);
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mBehaviorLocaleButton.equals(getString(R.string.settings_locale_button_id_open_menu))) {
            // Open the locale menu.
            openLocaleMenu();
        } else {
            // Switch to the next language.
            Assert.assertTrue(mBehaviorLocaleButton.equals(getString(R.string.settings_locale_button_id_next_locale)));

            LanguageProfileManager.getProfileManager().setCurrentProfile(nextProfile.getLang());

            // Restart the input view. This updates the keyboard layout.
            onFinishInputView(true);
            onStartInputView(safeGetCurrentInputEditorInfo(), true);

            // Notify the user
            String toastMsg = String.format(
                    KeyboardService.this.getResources().getString(R.string.selected_locale_confirm_message),
                    LanguageSelector.getNameFromCode(this, nextProfile.getLang()));

            // Cancel the last toast message. This is in case the user presses
            // the locale button rapidly.
            if (mRotateToast != null)
                mRotateToast.cancel();
            mRotateToast = Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT);
            mRotateToast.show();
        }
    }

    /**
     * Callback for the language button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickLocale(View v) {
        onLocaleKey();
    }


    /**
     * Callback for the about button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickAbout(View v) {
        // Launch the about page
        Intent intent = new Intent(this, About.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    /**
     * Opens the language menu.
     */
    public void openLocaleMenu() {
        // Get a list of languages
        ArrayList<LanguageProfile> profileList = LanguageProfileManager
                .getProfileManager().loadProfiles();
        ArrayList<String> profileStrList = new ArrayList<String>(
                profileList.size());
        // Get the current language index
        for (int i = 0; i < profileList.size(); i++) {
            String strName = LanguageSelector.getNameFromCode(this, profileList.get(i).getLang());
            profileStrList.add(strName);
        }

        // Create a menu
        PopupMenuView mPopupMenu = new PopupMenuView(
                (ViewGroup) KeyboardService.mIME.mKeyboardLayout,
                profileStrList, R.string.any_key_action_locale,
                R.drawable.ic_launcher);
        mPopupMenu.setOnPopupMenuItemClickListener(mLocaleMenuListener);
        mPopupMenu.showContextMenu();
    }

    /**
     * Callback for the help button in the Any Key menu.
     *
     * @param v The button view.
     */
    public void onClickHelp(View v) {
        launchHelp();
    }

    /**
     * Launches the knowledge base website.
     */
    protected void launchHelp() {
        dismissAllPopupWindows(true);

        // Launch the help & support PreferenceScreen
        Intent intent = new Intent();
        intent.setAction(getString(R.string.settings_intent));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Settings.LAUNCH_ACTIVITY_KEY, "help_support");
        startActivity(intent);
    }


    protected void replaceText(final CharSequence text) {
        final InputConnection inputConnection = getCurrentInputConnection();
        inputConnection.performContextMenuAction(android.R.id.selectAll);
        commitText(text);
    }


    /**
     * Commits text to the edit view. Overrides any current composing text.
     *
     * @param text The text to commit.
     */
    protected void commitText(CharSequence text) {
        setLastWord(text);
        mComposing.setLength(0);

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        // FIRST INACTIVE INPUTCONNECTION
        inputConnection.commitText(text, 1);
    }

    /**
     * Append new text to the end of the edit view.
     *
     * @param text The text to append.
     */
    private void appendText(String text) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.commitText(text, 1);
    }

    /**
     * Hide or display the candidate view.
     *
     * @param show true to display, false to hide.
     */
    public void setCandidatesViewShown(boolean show) {
        setCandidatesViewShown(show, true);
    }

    /**
     * Hide or display the candidate view.
     *
     * @param show          Set true to display or false to hide.
     * @param rememberState Flag to save state.
     */
    public void setCandidatesViewShown(boolean show, boolean rememberState) {

        //
        //
        show = true;
        //
        //

        if (mCandidateView == null)
            return;

        if (mShowSuggestions && show) {
            if (!mCandidateView.isShown()) {
                mCandidateView.setVisibility(View.VISIBLE);
                onKeyboardSizeChanged();
            }
        } else if (mCandidateView.isShown()) {
            mCandidateView.setVisibility(View.GONE);
            onKeyboardSizeChanged();
        }

        if (rememberState)
            mLastKeyboardState.isCandidateViewShown = show;
    }

    /**
     * Apply a photo wallpaper to the background. Used by WallpaperPhoto for
     * preview purposes only.
     *
     * @param wallpaper Wallpaper drawable.
     * @param alpha     Alpha value for keys (not background).
     * @param fit       Set true to resize both dimensions to fit. This may (will)
     *                  change the aspect ratio. If false, it will be resized in only
     *                  one dimension, and cropped. This preserves the aspect ratio.
     */
    public void applyWallpaper(Drawable wallpaper, int alpha, boolean fit) {
        if (mKeyboardView != null)
            mKeyboardView.applyWallpaper(wallpaper, alpha, fit);
    }

    /**
     * Reloads the wallpaper. Called by wallpaper setting activity to restore
     * the original wallpaper if user cancels the preview.
     */
    public void reloadWallpaper() {
        if (mKeyboardView != null)
            mKeyboardView.reloadWallpaper();
    }

    /**
     * Reloads the current theme.
     */
    private void reloadTheme() {
        KeyboardThemeManager.getThemeManager().reloadTheme();
        if (getKeyboardView() != null)
            getKeyboardView().reloadTheme();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        if (!mInputContents.equals(""))
            // Parse input from previous session
            learnWords();

        // Get starting contents of EditText
        mInputContents = mStartInputContents = getInputContents();
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application. At this point we have been bound to
     * the client, and are now receiving all of the detailed information about
     * the target of our edits.
     */
    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        callTrace("onStartInputView()");

        // Rate app in market
        AppRater.promptUserToRate(this);

        // Show welcome screen (first time only)
        Welcome.showWelcome(this);

        // Escape this package to avoid losing focus
        if (editorInfo.packageName.equals("com.google.android.voicesearch"))
            return;

        // Load user preferences
        loadPrefs();

        // Retrieve current theme
        reloadTheme();

        // Reset our state. We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any
        // way.
        if (!mLastKeyboardState.mChanged) {
            mComposing.setLength(0);
            clearCandidates();

            // Reset composing region
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null)
                inputConnection.finishComposingText();

            // Clear suggestions
            mLastKeyboardState.resetSuggestions();
        }

        updateCandidateFontSize();

        getKeyboardView().setCapsLock(false);

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        // Start with default values
        mPredictionOn = false;
        mCompletionOn = false;
        mIsPassword = false;
        mLearnWords = false;
        mCompletions = null;
        mLastWord = null;
        mURL = false;

        // Various hacks for different apps that pass strange or illegal values
        // in EditorInfo
        editorInfo = editorInfoHack(editorInfo);

        BaseKeyboard keyboard = mAlphaKeyboard;
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (editorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                keyboard = mNumPadKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                keyboard = mNumPadKeyboard;
                break;

            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing. We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                keyboard = mAlphaKeyboard;
                mPredictionOn = true;
                mLearnWords = true;

                // We now look for a few special variations of text that will modify our behavior.
                int variation = editorInfo.inputType
                        & EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                    mLearnWords = false;
                    mIsPassword = true;
                    mSmartSpaces = false;
                    if (editorInfo.fieldId != mLastPasswordId
                            || getInputContents().length() != mPassword.length())
                        mPassword.setLength(0);
                    mLastPasswordId = editorInfo.fieldId;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)) {
                    // Predictions are not useful for e-mail addresses
                    mPredictionOn = false;
                    mLearnWords = false;
                    mSmartSpaces = false;
                    mURL = true;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    // Predictions are not useful for e-mail addresses or URIs.
                    mPredictionOn = false;
                    mLearnWords = false;
                    mSmartSpaces = false;
                    mURL = true;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for filters
                    mPredictionOn = false;
                    mLearnWords = false;
                    mURL = false;
                }


                if ((editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // The app has it's own completions to display. If we're in fullscreen mode,
                    // display them instead of our predictions. If not, the app will display
                    // the completions and we can still display predictions.
                    if (mCompletionOn = isFullscreenMode())
                        mPredictionOn = false;
                }

//			if ((editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
//				// Application has specifically requested no suggestions
//				mPredictionOn = false;
//			}
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                keyboard = mAlphaKeyboard;
                mPredictionOn = false;
        }

        // Restore number row if keyboard is alpha
        if (keyboard == mAlphaKeyboard && mNumRowOn) {
            openNumRow();
            keyboard = mAlphaNumKeyboard;
        }

        // App settings override defaults for predictions
        if (mShowSuggestions == false) {
            mPredictionOn = false;
        }
        if (!mPredictionOn) {
            // reset last configuration
            mLastKeyboardState.resetSuggestions();
        }

        // Update the keyboard for the current input state
        setCandidatesViewShown(mPredictionOn || mIsPassword);
        keyboard.setImeOptions(getResources(), editorInfo);
        getKeyboardView().setAnyKeyAction(mAnyKeyState, mAnyKeyLongState, mReturnKeyLongState);

        // Set preview stat
        getKeyboardView().setPreviewEnabled(mShowPopupKey);

        // Update keyboard bottom padding
        updateKeyboardBottomPaddingHeight();

        if (mLastKeyboardState.mChanged || mLastKeyboardState.mNeedToUpdate) {
            // Restore saved state
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // ???
                    mKeyboardView.restorePopupWindow(
                            mLastKeyboardState.popupLayoutID,
                            mLastKeyboardState.popupKeyboardID,
                            mLastKeyboardState.popupKeyboardParm);
                }
            });
            // mKeyboardView.showPopupKeyboard(lastConfiguration.popupLayoutID,
            // lastConfiguration.popupKeyboardID,
            // lastConfiguration.popupKeyboardParm);
            // candidate string
            if (mPredictionOn && !mCompletionOn) {
                mCandidateView.setSuggestions(mLastKeyboardState.suggestions,
                        mLastKeyboardState.completions);
            }

            getKeyboardView().setCapsLock(mLastKeyboardState.mCapsLock);
            mKeyboardView.setShifted(mLastKeyboardState.bShifted,
                    mLastKeyboardState.mCapsLock);
            mLastKeyboardState.mNeedToUpdate = false;
        } else {
            if (mKeyboardView.getKeyboard() != keyboard)
                mKeyboardView.setKeyboard(keyboard);
            updateShiftKeyState(editorInfo, isMultiLine()); // Use auto-caps in
            // multi-line fields
            // even if the app
            // didn't request
            // it);

            clearCandidates();
        }

        // Clear the suggestion bar
        mCandidateView.clearDisplayMessage();

        if (mIsPassword) {
            // Initialize the password value
            updatePassword(false);
        }

        if (!inPreviewMode()) {
//			Log.v(KeyboardApp.LOG_TAG, "dictionary not exist " + !KeyboardApp.getKeyboardApp().getUpdater().isDictionaryExist(this,
//					mLanguage) + " lang " + mLanguage +  " ; dictionary needs updates "  + isNeedUpdateDicts());

            if (!KeyboardApp.getApp().getUpdater().isDictionaryExist(this, mLanguage) || isNeedUpdateDicts()) {
                // No dictionary installed. Display a prompt in the candidate view
                showSuggestionDictionaryUpdate();
            } else if (isNeedUpgradeApp()) {
                showSuggestionAppUpdate();
            }
        }


        if (mVoiceRecognitionTrigger != null)
            mVoiceRecognitionTrigger.onStartInputView();
    }

    public void showSuggestionDictionaryUpdate() {
        if (mCandidateView != null) {

            mCandidateView.setDisplayMessage(
                    getResources()
                            .getString(R.string.dic_updated_alarm_message),
                    CandidateView.MessageType.MESSAGE_ALARM,
                    new OnClickListener() {
                        public void onClick(View v) {
                            if (!KeyboardApp.getApp().getUpdater().isDictionaryExist(KeyboardService.this, mLanguage))
                                launchDictionariesUpdate(new String[]{mLanguage});
                            else
                                launchDictionariesUpdate();
                        }
                    }
            );

            mIsAlarmMessageFirstAppeared = true;

        }
    }

    public void showSuggestionDictionaryUpdateOnUi() {
        if (mCandidateView != null)
            mCandidateView.post(new Runnable() {
                public void run() {
                    showSuggestionDictionaryUpdate();
                }
            });
    }

    public void showSuggestionAppUpdate() {
        if (mCandidateView != null) {
            mCandidateView.setDisplayMessage(
                    getResources()
                            .getString(R.string.dic_updated_app_is_needed_upgrade),
                    CandidateView.MessageType.MESSAGE_STATIC,
                    new OnClickListener() {
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse("market://details?id=" + KeyboardApp.packageName));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });

            mIsAlarmMessageFirstAppeared = true;
        }
    }

    /**
     * Runs showSuggestionAppUpdate() on UI thread
     */
    public void showSuggestionAppUpdateOnUi() {
        if (mCandidateView != null)
            mCandidateView.post(new Runnable() {
                public void run() {
                    showSuggestionAppUpdate();
                }
            });
    }

    /**
     * Callback for check password prompt. Toggles the password reveal.
     */
    private View.OnClickListener mOnClickPassword = new OnClickListener() {
        public void onClick(View v) {
            updatePassword(!mShowPassword);
        }
    };

    /**
     * This is called when the user is done editing a field. We can use this to
     * reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();
        callTrace("KeyboardService.onFinishInput()");

        // Parse the inputted text to learn look-ahead word prediction.
        learnWords();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        clearCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false, false);

        if (mLastKeyboardState.mChanged) {
            mLastKeyboardState.mChanged = false;
            mLastKeyboardState.mNeedToUpdate = true;
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        callTrace("onFinishInputView()");

        mInPreviewMode = false;

        // hide all context menu
        PopupMenuView.closeAllMenu();
    }

    /**
     * Load user settings from shared prefs and re-initialize keyboard if
     * necessary.
     */
    protected void loadPrefs() {
        SharedPreferences sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
        if (!mKeyboardLayoutId.equals(KeyboardLayout.getCurrentLayout().getId())) {
            // Keyboard layout changed
            mKeyboardLayoutId = KeyboardLayout.getCurrentLayout().getId();
            createKeyboards();
            mKeyboardView.setKeyboard(mAlphaKeyboard);
        }
        mSuggestor.loadPreferences();
        mDefaultCurrency = sharedPrefs.getString("currency", getString(R.string.curr_symbol_default));
        mAutoCaps = sharedPrefs.getBoolean("auto_caps", true);
        mSmartSpaces = sharedPrefs.getBoolean("smart_spaces", true);
        mDoubleSpace = sharedPrefs.getBoolean("double_space", false);
        mBehaviorLocaleButton = sharedPrefs.getString("behavior_locale_button", getString(R.string.settings_locale_button_id_next_locale));
        mShowSuggestions = sharedPrefs.getBoolean("show_suggestions", true);
        mAutoSelect = sharedPrefs.getBoolean("auto_select", true);
        mVibrateLength = Integer.parseInt(sharedPrefs.getString("vibrate", getString(R.string.settings_vibrate_default)));
        mVibrateOnTypoCorrection = sharedPrefs.getBoolean("vibrate_on_typo_correction", false);
        mSoundKey = sharedPrefs.getBoolean("sound", false);
        mPredictNextWord = sharedPrefs.getBoolean("nextword", true);
        mShowPopupKey = sharedPrefs.getBoolean("preview_popup", true);
        mAnyKeyState = sharedPrefs.getString("any_key_action", getString(R.string.any_key_action_id_default));
        mAnyKeyLongState = sharedPrefs.getString("any_key_long_action", getString(R.string.any_key_long_action_id_default));
        mReturnKeyLongState = sharedPrefs.getString("return_key_long_action", getString(R.string.return_key_long_action_id_default));
        mSwipeNumberRow = sharedPrefs.getBoolean("swipe_num_row", true);

//		mSuggestionsAlwaysShow = sharedPrefs.getBoolean("suggestions_always_show", true);

        if (mDebug) {
            writeDebug("loadPrefs(): mDefaultCurrency=" + mDefaultCurrency);
            writeDebug("loadPrefs(): mAutoCaps=" + mAutoCaps);
            writeDebug("loadPrefs(): mSmartSpaces=" + mSmartSpaces);
            writeDebug("loadPrefs(): mDoubleSpace=" + mDoubleSpace);
            writeDebug("loadPrefs(): mBehaviorLocaleButton=" + mBehaviorLocaleButton);
            writeDebug("loadPrefs(): mShowSuggestions=" + mShowSuggestions);
            writeDebug("loadPrefs(): mAutoSelect=" + mAutoSelect);
            writeDebug("loadPrefs(): mVibrateLength=" + mVibrateLength);
            writeDebug("loadPrefs(): mVibrateOnTypoCorrection=" + mVibrateOnTypoCorrection);
            writeDebug("loadPrefs(): mSoundKey=" + mSoundKey);
            writeDebug("loadPrefs(): mPredictNextWord=" + mPredictNextWord);
            writeDebug("loadPrefs(): mShowPopupKey=" + mShowPopupKey);
            writeDebug("loadPrefs(): mAnyKeyState=" + mAnyKeyState);
            writeDebug("loadPrefs(): mAnyKeyLongState=" + mAnyKeyLongState);
            writeDebug("loadPrefs(): mReturnKeyLongState=" + mReturnKeyLongState);
            writeDebug("loadPrefs(): mSwipeNumberRow=" + mSwipeNumberRow);
        }

        mLanguage = LanguageSelector.getLanguagePreference(this);
        mTheme = sharedPrefs.getString("theme", getString(R.string.default_theme_id));

        // enable/diable long press symbols
        setSuperLabelEnabled(sharedPrefs.getBoolean("long_press_symbols", true));

        updateCurrencyKeys();

        mPrefsChanged = false;
    }

    public boolean isDrawSuperLabel(Key key) {
        int primaryCode = key.codes[0];
        if (!isSuperLabelEnabled()
                && !(isFnKeyPressed(primaryCode)
                || primaryCode == BaseKeyboard.KEYCODE_MODE
                || primaryCode == BaseKeyboard.KEYCODE_MODE_CHANGE
                || (key.label != null && key.label.equals(".") && key.edgeFlags == BaseKeyboard.EDGE_BOTTOM))) {
            return false;
        }

        return true;
    }

    /**
     * Called by Settings when prefs change. Causes a reload on next
     * onStartInput().
     */
    public void onPrefsChanged() {
        mPrefsChanged = true;
    }

    /**
     * Parse input text and save bi-grams (word pairs) to learn look-ahead word
     * prediction.
     */
    private void learnWords() {

        if (!mLearnWords)
            return;

        // Don't learn "Other" language
        if (mLanguage.equals(getString(R.string.lang_code_other)))
            return;

        // Remove original text from final text, if necessary
        if (!mStartInputContents.equals("")) {
            if (mInputContents.contains(mStartInputContents))
                mInputContents = mInputContents.replace(mStartInputContents, "");
            else
                mInputContents = "";
        }
        mStartInputContents = "";

        mSuggestor.learnSuggestions(mInputContents);

        mStartInputContents = "";
        mInputContents = "";
    }


    /**
     * Remembers a word so it is immediately available for suggestions.
     *
     * @param word The word to remember
     */
    protected void rememberWord(String word) {
        if (mSuggestor.getLanguageDictionary().remember(word))
            Toast.makeText(
                    getApplicationContext(),
                    getResources().getString(R.string.msg_word_remembered, word),
                    Toast.LENGTH_SHORT).show();
    }


    protected void onSuggestionMenuItemClick(String _orig, String word) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null)
            inputConnection.finishComposingText();
        mComposing.setLength(0);
        clearCandidates();
        rememberWord(word);
    }


    /**
     * Gets the current contents of the edit view, up to MAX_INPUT_LENGTH chars
     * before/after cursor.
     *
     * @return The contents of the edit view.
     */
    protected String getInputContents() {
        InputConnection inputConnection = getCurrentInputConnection();
        StringBuilder inputText = new StringBuilder("");
        if (inputConnection != null) {
            CharSequence before = getTextBeforeCursor(inputConnection, MAX_INPUT_LENGTH);
            CharSequence after = getTextAfterCursor(inputConnection, MAX_INPUT_LENGTH);
            inputText.append(before).append(after);
        }

        return inputText.toString();
    }

    public void showMessage(final String message, final OnClickListener listener) {
        // Using View.post() lets this method be called from a non-UI thread.
        if (mCandidateView == null)
            return;

        mCandidateView.post(new Runnable() {
            @Override
            public void run() {
                mCandidateView.setDisplayMessage(message, CandidateView.MessageType.MESSAGE_STATIC, listener);
            }
        });
        mComposing.setLength(0);
    }

    public void clearMessage() {
        // Using View.post() lets this method be called from a non-UI thread.
        if (mCandidateView == null)
            return;

        mCandidateView.post(new Runnable() {
            @Override
            public void run() {
                mCandidateView.clearDisplayMessage();
            }
        });
    }

    /**
     * Displays a sample suggestion. This is used only by the suggestion height
     * setting preview.
     */
    public void showSampleSuggestion() {
        // set demo candidate string
        showMessage("Suggestion-Height", null);
        updateCandidateFontSize();
    }

    /**
     * Update the suggestion font size to user setting.
     */
    public void updateCandidateFontSize() {
        if (mCandidateView == null)
            return;

        // Get current candidate font size
        int fontSize = CandidateHeightSetting.getFontSizePreference(this);

        // set candidate view size
        mCandidateView.setFontHeight(fontSize);

        mCandidateView.requestLayout();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int composingStart, int composingEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                composingStart, composingEnd);

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.beginBatchEdit();

        if (composingEnd > composingStart
                && (newSelEnd < composingStart || newSelStart > composingEnd)) {
            // Selection has moved outside composing. Stop composing.
            inputConnection.finishComposingText();
            mComposing.setLength(0);
            composingStart = -1;
            composingEnd = -1;
            bAutoSpace = false;
        }

        if (composingStart == composingEnd) {
            mComposing = new StringBuilder();
        }

        updateComposing(inputConnection);

        inputConnection.endBatchEdit();

        // Update input contents
        mInputContents = getInputContents();
        if (mInputContents.length() == 0)
            mComposing.setLength(0);
    }


    private void updateComposing(InputConnection inputConnection) {
        if (!mPredictionOn || (mComposing != null && mComposing.length() > 0))
            // Predictions unnecessary, or already in progress
            return;

        if (inputConnection == null)
            return;

        CharSequence before = new StringBuilder(getWordBeforeCursor(inputConnection));
        CharSequence after = new StringBuilder(getWordAfterCursor(inputConnection));
        if (before.length() > 0) {
            int cursor = getCursorLocation(inputConnection);
            if (safeSetComposingRegion(inputConnection,
                    cursor - before.length(), cursor + after.length())) // Only in 2.3.3+ (see below)
                mComposing = new StringBuilder(before).append(after);
        }
        updateCandidates();
    }

    // Backwards-compatible implementation of
    // InputConnection.setComposingRegion()
    private static Method mInputConnection_setComposingRegion = null;

    static {
        initCompatibility();
    }

    ;

    private static void initCompatibility() {
        try {
            mInputConnection_setComposingRegion = InputConnection.class
                    .getMethod("setComposingRegion", new Class[]{int.class,
                            int.class});
            /* success, this is a newer device */
        } catch (NoSuchMethodException nsme) {
			/* failure, must be older device */
        }
    }

    private boolean safeSetComposingRegion(InputConnection inputConnection, int start, int end) {
        if (isGoogleMailBody())
            return false;

        if (mInputConnection_setComposingRegion != null) {
            try {
                Boolean success = (Boolean) mInputConnection_setComposingRegion.invoke(inputConnection, start, end);
                return success.booleanValue();
            } catch (InvocationTargetException ite) {
                // Unexpected exception; wrap and re-throw
                throw new RuntimeException(ite);
            } catch (IllegalAccessException ie) {
                // Do nothing?
            }
        }

        return false;
    }


    public void buyTranslatorCredits() {
        // Do nothing by default
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
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode,
                event);
        int c = event.getUnicodeChar(MetaKeyKeyListener
                .getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application. We get
     * first crack at them, and can either resume them or let them continue to
     * the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mWindowVisible)
            return false;

        bAutoSpace = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Ignore this event if the translator popup is open (onKeyUp
                // will close it)
                TranslatorView translatorView = getKeyboardView().mTranslatorView;
                if (translatorView != null) {
                    if (translatorView.findViewById(R.id.text_clipboard)
                            .getVisibility() != View.GONE)
                        return true;
                    if (translatorView.findViewById(R.id.menu_languages)
                            .getVisibility() != View.GONE)
                        return true;
                }

                // Ignore this event if popup windows are showing. It will be
                // handled in onKeyUp().
                if (mKeyboardView.isPopupShowing())
                    return true;

                // Ignore this event in certain activities. They will handle it.
                if (inPreviewMode())
                    return false;
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application. We get
     * first crack at them, and can either resume them or let them continue to
     * the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mWindowVisible)
            // Don't do anything if the keyboard is not visible.
            return false;

        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                TranslatorView translatorView = getKeyboardView().mTranslatorView;
                if (translatorView != null) {
                    // Close translator items (clipboard translation popup, or
                    // language menu)
                    TextView textClipboard = (TextView) getKeyboardView().mTranslatorView
                            .findViewById(R.id.text_clipboard);
                    if (textClipboard.getVisibility() != View.GONE) {
                        // Close the clipboard translation
                        textClipboard.setVisibility(View.GONE);
                        return true;
                    }

                    ListView menuLanguages = (ListView) getKeyboardView().mTranslatorView
                            .findViewById(R.id.menu_languages);
                    if (menuLanguages.getVisibility() != View.GONE) {
                        // Close a language menu
                        menuLanguages.setVisibility(View.GONE);
                        return true;
                    }
                }

                if (mKeyboardView.isPopupShowing()) {
                    // Close popup keyboard
                    dismissAllPopupWindows();
                    return true;
                }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onWindowHidden() {
        // Composing text is automatically committed, so clear mComposing and
        // candidates view
        mComposing.setLength(0);
        clearCandidates();

        dismissAllPopupWindows();

        // Keep track of window visibility.
        mWindowVisible = false;
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        // Keep track of window visibility.
        mWindowVisible = true;
    }

    /**
     * Commit the current composition. Do not replace with a suggestion.
     */
    protected CharSequence commitTyped() {
        setLastWord(mComposing.toString());

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null && mComposing.length() > 0) {
            inputConnection.commitText(mComposing, 1);
            mComposing.setLength(0);
            clearCandidates();
        }

        return mComposing;
    }

    /**
     * Update the shift key state based on the app preference, or force all-caps
     * if caps==true.
     *
     * @param ei   An EditorInfo provided by the app.
     * @param caps Set true to force caps. If false, use EditorInfo.
     */
    private void updateShiftKeyState(EditorInfo ei, boolean caps) {
        if (ei != null && mKeyboardView != null) {
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL
                    && mAutoCaps == true) {
                InputConnection inputConnection = getCurrentInputConnection();
                if (inputConnection == null)
                    return;
                if (caps == false) {
                    if (isGoogleMailBody(ei))
                        caps = mGoogleMailHackCapsMode;
                    else {
                        int capsMode = inputConnection.getCursorCapsMode(ei.inputType);
                        caps = capsMode > 0;
                    }
                }
            }
            mKeyboardView.setShifted(caps && mAutoCaps, getKeyboardView().getCapsLock());
        }
    }

    private void updateShiftKeyState() {
        updateShiftKeyState(safeGetCurrentInputEditorInfo(), false);
    }

    /**
     * Returns the current EditorInfo, processed by editorInfoHack().
     *
     * @return The current EditorInfo from the app (hacked).
     */
    private EditorInfo safeGetCurrentInputEditorInfo() {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        // Update the EditorInfo with hack overrides.
        editorInfo = editorInfoHack(editorInfo);
        return editorInfo;
    }

    /**
     * This method hacks the EditorInfo provided by the app. Why? Because Google
     * Mail body field is F***ED.
     *
     * @param editorInfo The EditorInfo to hack.
     * @return The hacked EditorInfo.
     */
    private EditorInfo editorInfoHack(EditorInfo editorInfo) {
        // Android Email uses an unknown input class
        if (isGoogleMailBody(editorInfo)) {
            if ((editorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) == 0xf)
                // Android Email uses an unknown input class. Change it to
                // standard text.
                editorInfo.inputType = (editorInfo.inputType & ~EditorInfo.TYPE_MASK_CLASS)
                        | EditorInfo.TYPE_CLASS_TEXT;
            if ((editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION) == 0xff)
                // Android Email uses unknown IME options. Change it to none
                editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE;
        }

        return editorInfo;
    }

    private void googleMailSetCapsModeHack(int code) {
        if (code == (int) '.' || code == (int) '?' || code == (int) '!')
            mGoogleMailHackCapsMode = true;
        else
            mGoogleMailHackCapsMode = false;
    }


    private boolean isGoogleMailBody(EditorInfo editorInfo) {
        if (editorInfo.packageName.equals("com.android.email")
                && (editorInfo.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) > 0) {
            // Android Email body doesn't handle auto-caps correctly

            return true;
        }

        return false;
    }

    private boolean isGoogleMailBody() {
        return isGoogleMailBody(getCurrentInputEditorInfo());
    }

    /**
     * Helper to determine if a given character code belongs in a word. In all
     * languages, letters and numbers are word characters. Depending on the
     * language, certain punctuation may be a word character. (e.g.
     * English:apostrophe, French:hyphen).
     *
     * @param code The character code to check.
     * @return true if code is a word character.
     */
    private boolean isWordCharacter(int code) {
        if (mLanguage.equals("fr")) {
            // FRENCH HACK
            if (Character.isLetter(code) || Character.isDigit(code)
                    || code == '-')
                return true;
        } else if (Character.isLetter(code) || Character.isDigit(code)
                || code == '\'') {
            return true;
        }

        return false;
    }

    /**
     * Returns the list of punctuation that may be followed by a smart space.
     *
     * @return A list of punctuation.
     */
    private String getSmartSpacePreceders() {
        return mSmartSpacePreceders;
    }

    /**
     * This method determines if a character code should be followed by a smart
     * space.
     *
     * @param code The code to check.
     * @return true if code should be followed by a smart space.
     */
    public boolean isSmartSpacePreceder(int code) {
        return getSmartSpacePreceders().contains(String.valueOf((char) code));
    }


    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
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
    public boolean isFnKeyPressed(int primaryCode) {
        switch (primaryCode) {
            case BaseKeyboard.KEYCODE_SETTINGS:
            case BaseKeyboard.KEYCODE_MODE_CHANGE:
            case BaseKeyboard.KEYCODE_SYM_MENU:
            case BaseKeyboard.KEYCODE_EMOJI:
            case BaseKeyboard.KEYCODE_ARROWS:
            case BaseKeyboard.KEYCODE_VOICE:
            case BaseKeyboard.KEYCODE_TRANSLATE:
            case BaseKeyboard.KEYCODE_LOCALE:
                return true;
        }

        return false;
    }

    /**
     * This method receives and handles all keystrokes.
     *
     * @param primaryCode The primary keystroke code.
     * @param keyCodes    All possible codes for this keystroke.
     */
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        writeDebug("KeyboardService.onKey(primaryCode=" + primaryCode + ")");

        if (mIME.inPreviewMode()) {
            // Keyboard is in preview mode
            if (primaryCode == BaseKeyboard.KEYCODE_ACTION)
                // Hide the keyboard
                onCancelKey();

            return;
        }

        dismissAllPopupWindows();

//		mProfileTracer.reset();
//		mProfileTracer.log("Round trip: onKey(" + ((char)primaryCode) + "): ENTER");

        // Send keystroke audio and vibrate feedback. Only sends one feedback
        // for repeatable keys.
        mIsVibrated = false;
        if (!mIsVibrated
                && (!isRepeatable(primaryCode) || mFirstRepeatablePress)) {
            // Send vibrate and/or sound feedback
            keyFeedback(primaryCode);

            // Only vibrate once for repeatable keys
            mFirstRepeatablePress = false;
        }

        // Ugh. Hack for Android
        googleMailSetCapsModeHack(primaryCode);

        if (mIsAlarmMessageFirstAppeared) {
            clearCandidates();
            mIsAlarmMessageFirstAppeared = false;
        }

        // If the user pressed certain punctuation (e.g. period) after an
        // auto-space, delete the space before inserting the punc.
        if (bAutoSpace && mSmartSpaces && isSmartSpacePreceder(primaryCode)) {
            // Delete the auto-space
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                CharSequence before = getTextBeforeCursor(inputConnection, 1);
                if (before.length() == 1 && before.charAt(0) == ' ')
                    inputConnection.deleteSurroundingText(1, 0);
            }
        }
        bAutoSpace = false;

        // Reset timer for double-tap space
        if (primaryCode != (int) ' ')
            mLastSpaceTime = 0;

        if (isWordSeparator(primaryCode)) {
            // Handle word separator
            onWordSeparator(primaryCode);
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            // Handle delete key
            onDelete();
        } else if (primaryCode == BaseKeyboard.KEYCODE_DEL_FORWARD) {
            // Handle delete forward key
            onDeleteForward();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            // Handle shift key
            onShiftKey();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            // Handle cancel key
            onCancelKey();
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            // Handle keyboard mode key
            if (mKeyboardView != null)
                if (mKeyboardView.mPopupKeyboardWindow.isShowing()) {
                    // Close sym quick menu
                    mKeyboardView.mPopupKeyboardWindow.dismiss();
                } else {
                    toggleKeyboardMode();
                }
        } else if (primaryCode == BaseKeyboard.KEYCODE_ACTION) {
            // Handle action key

            switch (getKeyboardView().getKeyboard().getEditorAction(false)) {
                case EditorInfo.IME_ACTION_DONE:
                case EditorInfo.IME_ACTION_GO:
                case EditorInfo.IME_ACTION_UNSPECIFIED:
                case EditorInfo.IME_ACTION_NEXT:
                case EditorInfo.IME_ACTION_SEARCH:
                case EditorInfo.IME_ACTION_SEND:
                    performEditorAction();
                    break;
                default:
                    // User pressed return.
                    onEnter();
                    EditorInfo editorInfo = safeGetCurrentInputEditorInfo();
                    updateShiftKeyState(editorInfo, isMultiLine());
            }
        } else if (primaryCode == BaseKeyboard.KEYCODE_ARROW_UP
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_LEFT
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_RIGHT
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_DOWN
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_HOME
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_END
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_BACK
                || primaryCode == BaseKeyboard.KEYCODE_ARROW_NEXT
                || primaryCode == BaseKeyboard.KEYCODE_CUT
                || primaryCode == BaseKeyboard.KEYCODE_COPY
                || primaryCode == BaseKeyboard.KEYCODE_PASTE
                || primaryCode == BaseKeyboard.KEYCODE_SELECT
                || primaryCode == BaseKeyboard.KEYCODE_SELECT_ALL) {
            // Handle cursor keys
            onArrowKey(primaryCode);
        } else if (primaryCode > 0) {
            // Handle character key
            onCharacter(primaryCode);
        } else {
            onFnKey(primaryCode);
        }

//		 mProfileTracer.log("Round trip: onKey(" + ((char) primaryCode) + "): EXIT");
    }

    /**
     * Handle any key
     *
     * @param primaryCode
     * @return
     */
    private boolean onFnKey(final int primaryCode) {
        switch (primaryCode) {
            case BaseKeyboard.KEYCODE_EMOJI:
                // Handle emoji key
                onEmojiKey();
                return true;
            case BaseKeyboard.KEYCODE_ARROWS:
                // Handle arrow keypad key
                onArrowKeypadKey();
                return true;
            case BaseKeyboard.KEYCODE_VOICE:
                onVoiceKey();
                return true;
            case BaseKeyboard.KEYCODE_TRANSLATE:
                // Handle translator key
                onTranslatorKey();
                return true;
            case BaseKeyboard.KEYCODE_LOCALE:
                // Handle locale key
                onLocaleKey();
                return true;
            case BaseKeyboard.KEYCODE_SYM_MENU:
                onSymMenuKey();
                return true;
            case BaseKeyboard.KEYCODE_SETTINGS:
                // Handle settings key
                onSettingsKey();
                return true;
        }
        return false;
    }

    /**
     * Called by the framework to insert text into the edit view.
     */
    @Override
    public void onText(CharSequence text) {
        writeDebug("KeyboardService.onText(text=" + text + ")");

        setLastWord(text.toString());

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.beginBatchEdit();
        if (mComposing.length() > 0)
            commitTyped();
        inputConnection.commitText(text, 0);
        inputConnection.endBatchEdit();
        updateShiftKeyState();

        // Close popup keyboard if open
        dismissAllPopupWindows();
    }

    /**
     * Toggles the password reveal
     *
     * @param reveal Set true to reveal the password, false to hide it.
     */
    protected void updatePassword(boolean reveal) {
        mShowPassword = reveal;
        String message = getString(R.string.candidate_check_password);
        if (mIsPassword && mShowPassword)
            message = mPassword.toString();
        mCandidateView.setDisplayMessage(message,
                CandidateView.MessageType.MESSAGE_STATIC, mOnClickPassword);
    }

    /**
     * Gets a list of suggestions based on prefix, to display to the user.
     *
     * @param prefix The prefix to match. Equal to the composition.
     */
    protected void updateCandidates(CharSequence prefix) {
        callTrace("updateCandidates(" + prefix + ")");

        if (mIsPassword) {
            updatePassword(mShowPassword);
            return;
        }

        if (mCompletionOn || !mPredictionOn)
            return;

        // Find suggestions asynchronously. Suggestor will call
        // returnCandidates() when done.
        mSuggestor.findSuggestionsAsync(prefix.toString());
        mPendingRequest = true;
    }

    protected void updateCandidates() {
        updateCandidates(mComposing);
    }

    /**
     * Called asynchronously by the Suggestor to update the CandidateView
     *
     * @param suggestions The list of suggestions to display.
     */
    protected void returnCandidates(Suggestions suggestions) {

        mPendingRequest = false;
        mSuggestions = suggestions;

        if (mDebug) {
            ArrayList<Suggestion> list = suggestions.getSuggestions();
            String debug = "returnCandidates(): suggestions={";
            for (int i = 0; i < list.size(); i++)
                debug += list.get(i) + ",";
            writeDebug(debug + "}");
        }

        // Save the suggestions
        setSuggestions(mSuggestions, true);
//		mProfileTracer.log("Round trip (" + suggestions.getPrefix() + "): complete");
    }

    /**
     * Update the list of available candidates from the current composing text.
     * This will need to be filled in by however you are determining candidates.
     */
    public void clearCandidates() {
        if (!mCompletionOn) {
            setLastWord("");
            setSuggestions(null, false);
        }
    }


    /**
     * Saves the suggestions and updates the CandidateView.
     *
     * @param suggestions The suggestions.
     * @param completions true if these are completions provided by the app, not the suggestor.
     */
    public void setSuggestions(Suggestions suggestions, boolean completions) {
        // Make sure CandidateView is visible.
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }

        // Update CandidateView
        if (mCandidateView != null)
            mCandidateView.setSuggestions(suggestions, completions);

        // Save suggestions to keyboard state
        mLastKeyboardState.saveSuggestions(suggestions, completions);
    }


    /**
     * Called by onKey() when the user presses delete (i.e. backspace) key.
     * Deletes the character in behind of the cursor.
     */
    @TargetApi(9)
    protected void onDelete() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.beginBatchEdit();

        CharSequence selectedText = inputConnection.getSelectedText(0);

        if (selectedText != null && selectedText.length() > 0) {
            // There is selected text. Delete it and return.
            int nCharsToDelete = selectedText.length();
            int cursorLocation = getCursorLocation(inputConnection) + nCharsToDelete;
            inputConnection.setSelection(cursorLocation, cursorLocation);
            inputConnection.finishComposingText();
            inputConnection.deleteSurroundingText(nCharsToDelete, 0);
            mComposing.setLength(0);
        } else {
            // Check if user is inline editing a word.
            StringBuilder before = new StringBuilder();
            StringBuilder after = new StringBuilder();
            if (isEditingWord(inputConnection, before, after)) {
                // User is inline editing a word. Stop composing and backspace.
                int cursor = getCursorLocation(inputConnection);
                inputConnection.finishComposingText();
                inputConnection.setSelection(cursor, cursor);
                inputConnection.deleteSurroundingText(1, 0);
                mComposing.setLength(0);
                if (before.length() > 0)
                    before.setLength(before.length() - 1);
            } else {
                // Process a standard backspace.
                final int length = mComposing.length();
                if (length > 1) {
                    // Delete the last character in the composition
                    mComposing.delete(length - 1, length);
                    inputConnection.setComposingText(mComposing, 1);
                } else if (length > 0) {
                    // Delete the only character in the composition
                    mComposing.setLength(0);
                    inputConnection.commitText("", 0);
                } else if (mIsPassword) {
                    if (mPassword.length() > 1)
                        mPassword.delete(mPassword.length() - 1,
                                mPassword.length());
                    else if (mPassword.length() == 1)
                        mPassword.setLength(0);
                    keyDownUp(KeyEvent.KEYCODE_DEL);
                } else {
                    // Just delete the preceding character
                    keyDownUp(KeyEvent.KEYCODE_DEL);
                }
            }
        }

        inputConnection.endBatchEdit();

        // Update keyboard state
        clearCandidates();
        updateShiftKeyState();
        updateCandidates();
    }

    /**
     * Called by onKey() when user presses delete forward key. Deletes the
     * character in front of the cursor.
     */
    @TargetApi(9)
    protected void onDeleteForward() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.beginBatchEdit();

        int nCharsToDelete = 1;
        CharSequence selectedText = null;
        selectedText = inputConnection.getSelectedText(0);

        if (selectedText != null && selectedText.length() > 0) {
            // There is selected text. Delete it and return.
            nCharsToDelete = selectedText.length();
            int cursorLocation = getCursorLocation(inputConnection);
            inputConnection.setSelection(cursorLocation, cursorLocation);
        }

        // Stop composing
        inputConnection.finishComposingText();
        inputConnection.deleteSurroundingText(0, nCharsToDelete);
        mComposing.setLength(0);
        inputConnection.endBatchEdit();

        // Update keyboard state
        clearCandidates();
        updateShiftKeyState();
        updateCandidates();
    }

    /**
     * Called by onKey() when the user presses the shift key. Updates the
     * keyboard shift state.
     */
    protected void onShiftKey() {
        if (mKeyboardView == null)
            return;

        // This key only affects the alpha keyboard
        if (isAlphaKeyboard()) {
            checkToggleCapsLock();
            boolean bShifted = mKeyboardView.isShifted();
            mKeyboardView.setShifted(!bShifted, getKeyboardView().getCapsLock());
        }
    }

    /**
     * Called by onKey() when the user presses a character key.
     *
     * @param code The character code.
     */
    @SuppressLint("NewApi")
    protected void onCharacter(int code) {
        if (isInputViewShown()) {
            if (mKeyboardView.isShifted()) {
                if ((char) code == 'ß') {
                    // Special case for upper-case �
                    onText("SS");
                    return;
                }
                // Shift is selected. Capitalize the letter.
                code = Character.toUpperCase(code);
            }
        }

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        updateComposing(inputConnection);

        inputConnection.beginBatchEdit();

        // Delete selected text
        CharSequence selectedText = inputConnection.getSelectedText(0);
        if (selectedText != null && selectedText.length() > 0) {
            // There is selected text. Delete it and return.
            int cursorLocation = getCursorLocation(inputConnection);
            inputConnection.setSelection(cursorLocation, cursorLocation);
            inputConnection.finishComposingText();
            inputConnection.deleteSurroundingText(0, selectedText.length());
            mComposing.setLength(0);
        }

        if ((!isWordCharacter(code) || code == '\'') && mPredictionOn && mComposing.length() == 0) {
            // Do not start composing with a non-letter. Just commit.
            inputConnection.commitText(Character.toString((char) code), 1);
        } else if (isWordCharacter(code) && mPredictionOn) {
            StringBuilder before = new StringBuilder();
            StringBuilder after = new StringBuilder();
            if (isEditingWord(inputConnection, before, after)) {
                // User is inline editing a word. Stop composing and insert text manually.
                int cursor = getCursorLocation(inputConnection);
                inputConnection.finishComposingText();
                inputConnection.setSelection(cursor, cursor);
                inputConnection.commitText("" + (char) code, 1);
                mComposing.setLength(0);
                before.append((char) code);
            } else {
                // Append to composing
                mComposing.append((char) code);
                inputConnection.setComposingText(mComposing, 1);
            }
            if (mDebug)
                writeDebug("onCharacter(code=" + code + "): mComposing=" + mComposing);
            updateCandidates();
        } else {
            // Prediction is off. Just append the character.
            String commit = ((char) code) + "";
            inputConnection.commitText(commit, 1);
            if (mIsPassword) {
                // Append to password reveal.
                mPassword.append(commit);
                updatePassword(mShowPassword);
            }
        }

        inputConnection.endBatchEdit();

        updateShiftKeyState();
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
    @TargetApi(9)
    private boolean isEditingWord(InputConnection inputConnection, StringBuilder before, StringBuilder after) {
        boolean result = false;

//		CharSequence selectedText = inputConnection.getSelectedText(0);
//		if (selectedText != null && selectedText.length() > 0)
//			result = true;
//		else {
        before.append(getWordBeforeCursor(inputConnection));
        after.append(getWordAfterCursor(inputConnection));
        final String composing = new StringBuilder(before).append(after).toString();
        if (composing.equals(mComposing.toString()) && after.length() > 0)
            result = true;
        else
            result = false;
//		}

        return result;
    }

    /**
     * Called by onKey() when the user presses the enter key.
     */
    protected void onEnter() {
        commitComposing();
        onText("\r\n");
    }

    /**
     * Returns the word in front of the cursor
     *
     * @param inputConnection The current InputConnection
     * @return A word, or empty string if there is no word directly in front of the cursor
     */
    private StringBuilder getWordBeforeCursor(InputConnection inputConnection) {
        if (inputConnection == null)
            return new StringBuilder("");

        StringBuilder wordBeforeCursor = new StringBuilder();
        CharSequence before = getTextBeforeCursor(inputConnection, MAX_WORD_LENGTH);

        if (before.length() == 0)
            return wordBeforeCursor;

        int iStartOfWord;
        for (iStartOfWord = before.length() - 1;
             iStartOfWord >= 0 && isWordCharacter(before.charAt(iStartOfWord));
             iStartOfWord--)
            ;

        if (++iStartOfWord < before.length())
            wordBeforeCursor.append(before.subSequence(iStartOfWord, before.length()));

        if (wordBeforeCursor.length() == 1
                && wordBeforeCursor.charAt(0) == '\'')
            // Don't start a word with '
            wordBeforeCursor.setLength(0);

        return wordBeforeCursor;
    }


    /**
     * Delete the word, and any trailing punctuation/whitespace, in front of the
     * cursor.
     */
    private void deleteWordBeforeCursor() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        inputConnection.beginBatchEdit();

        CharSequence letterBeforeCursor = getTextBeforeCursor(inputConnection, 1);
        if (letterBeforeCursor.length() == 1
                && !isWordCharacter(letterBeforeCursor.charAt(0))) {
            // First delete whitespace/punc before cursor
            while ((letterBeforeCursor = getTextBeforeCursor(inputConnection, 1)).length() == 1
                    && !isWordCharacter(letterBeforeCursor.charAt(0))) {
                inputConnection.deleteSurroundingText(1, 0);
            }
        }
        // Commit composing first
        if (mComposing.length() > 0)
            commitTyped();
        // Now, delete the word (if any)
        while ((letterBeforeCursor = getTextBeforeCursor(inputConnection, 1))
                .length() == 1 && isWordCharacter(letterBeforeCursor.charAt(0))) {
            inputConnection.deleteSurroundingText(1, 0);
        }

        inputConnection.endBatchEdit();
    }


    /**
     * Returns the word following the cursor
     *
     * @param inputConnection A valid InputConnection
     * @return A word, or empty string if there is no word immediately following the cursor
     */
    private StringBuilder getWordAfterCursor(InputConnection inputConnection) {
        if (inputConnection == null)
            return new StringBuilder("");

        // We are starting an append to a word. Make that word the new composing text.
        StringBuilder word = new StringBuilder();
        CharSequence after = getTextAfterCursor(inputConnection, MAX_WORD_LENGTH);

        if (after.length() == 0)
            return word;

        int iEndOfWord;
        for (iEndOfWord = 0;
             iEndOfWord < after.length() && isWordCharacter(after.charAt(iEndOfWord));
             iEndOfWord++)
            ;

        if (--iEndOfWord < after.length())
            word.append(after.subSequence(0, iEndOfWord + 1));


        return word;
    }


    /**
     * Safe wrapper for InputConnection.getTextBeforeCursor() that returns ""
     * instead of null.
     *
     * @param inputConnection An InputConnection. It is checked for null value.
     * @param n               The number of characters to return. The result may be shorter.
     * @return The text before the cursor, up to n characters, or "" if there is none.
     */
    private CharSequence getTextBeforeCursor(InputConnection inputConnection, int n) {
        if (inputConnection == null)
            return "";

        CharSequence before = inputConnection.getTextBeforeCursor(n, 0);
        if (before == null)
            return "";

        return before;
    }

    /**
     * Safe wrapper for InputConnection.getTextAfterCursor() that returns ""
     * instead of null.
     *
     * @param inputConnection An InputConnection. It is checked for null value.
     * @param n               The number of characters to return. The result may be shorter.
     * @return The text after the cursor, up to n characters, or "" if there is none.
     */
    private CharSequence getTextAfterCursor(InputConnection inputConnection, int n) {
        if (inputConnection == null)
            return "";

        CharSequence after = inputConnection.getTextAfterCursor(n, 0);
        if (after == null)
            return "";

        return after;
    }

    /**
     * Returns an index to the cursor location in the edit text. (Why is there
     * no framework method for this???)
     *
     * @param inputConnection The current InputConnection.
     * @return An index to the cursor location.
     */
    private int getCursorLocation(InputConnection inputConnection) {
        if (inputConnection == null)
            return 0;

        CharSequence textBeforeCursor = getTextBeforeCursor(inputConnection, 99);
        if (textBeforeCursor == null)
            return 0;
        if (textBeforeCursor.length() >= 99) {
            textBeforeCursor = getTextBeforeCursor(inputConnection, 999);
            if (textBeforeCursor.length() >= 999) {
                textBeforeCursor = getTextBeforeCursor(inputConnection, 9999);
                if (textBeforeCursor.length() >= 9999) {
                    textBeforeCursor = getTextBeforeCursor(inputConnection, 99999);
                }
            }
        }

        return textBeforeCursor.length();
    }

    /**
     * Move the cursor back one word. Skip any trailing punctuation/whitespace too.
     *
     * @param inputConnection The current InputConnection.
     */
    private void cursorBackWord(InputConnection inputConnection) {
        inputConnection.beginBatchEdit();

        int nChars = 0;
        CharSequence letterBeforeCursor;
        // First skip whitespace/punc before cursor
        while ((letterBeforeCursor = getTextBeforeCursor(inputConnection, nChars + 1)).length() == nChars + 1
                && !isWordCharacter(letterBeforeCursor.charAt(0))) {
            nChars++;
        }

        // Now, skip the word (if any)
        while ((letterBeforeCursor = getTextBeforeCursor(inputConnection, nChars + 1)).length() == nChars + 1
                && isWordCharacter(letterBeforeCursor.charAt(0))) {
            nChars++;
        }

        int cursor = getCursorLocation(inputConnection);
        inputConnection.setSelection(cursor - nChars, cursor - nChars);

        inputConnection.endBatchEdit();
    }

    /**
     * Move the cursor forward one word. Skip any preceding punctuation/whitespace too.
     *
     * @param inputConnection The current InputConnection.
     */
    private void cursorNextWord(InputConnection inputConnection) {
        inputConnection.beginBatchEdit();

        int nChars = 0;
        // First, skip the word
        CharSequence letterAfterCursor;
        while ((letterAfterCursor = getTextAfterCursor(inputConnection, nChars + 1)).length() == nChars + 1
                && isWordCharacter(letterAfterCursor.charAt(nChars))) {
            nChars++;
        }

        // Now skip any trailing whitespace or punctuation
        while ((letterAfterCursor = getTextAfterCursor(inputConnection, nChars + 1)).length() == nChars + 1
                && !isWordCharacter(letterAfterCursor.charAt(nChars))) {
            nChars++;
        }

        int cursor = getCursorLocation(inputConnection);
        inputConnection.setSelection(cursor + nChars, cursor + nChars);

        inputConnection.endBatchEdit();
    }

    /**
     * Returns up to two words in front of the prefix, less leading or trailing
     * whitespace and punctuation. For example, if the user is typing
     * "nice to meet", word1="nice", word2="to" and prefix="meet".
     *
     * @param word1 A StringBuilder that is filled with the word before word2.
     * @param word2 A StringBuilder that is filled with the word before the prefix/cursor.
     * @return The number of words returned. May be < 2.
     */
    public int getTwoWordsBeforePrefix(StringBuilder word1, StringBuilder word2) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return 0;

        // Clear the StringBuilders, just in case.
        word1.setLength(0);
        word2.setLength(0);
        int iEndOfWord = 0, iStartOfWord = 0, iStartOfPrefix = 0;


        // Get the text before the cursor, up to 50 chars.
        CharSequence textBeforeCursor = getTextBeforeCursor(inputConnection, MAX_WORD_LENGTH);
        if (textBeforeCursor.length() == 0)
            return 0;


        // Skip the prefix (if any)
        for (iStartOfPrefix = textBeforeCursor.length() - 1; iStartOfPrefix >= 0; iStartOfPrefix--)
            if (!isWordCharacter(textBeforeCursor.charAt(iStartOfPrefix)))
                break;

        if (iStartOfPrefix < 0)
            return 0;


		/*
		 * Get the word in front of the prefix/cursor (i.e. word2)
		 */

        // Skip trailing whitespace & punctuation.
        for (iEndOfWord = iStartOfPrefix; iEndOfWord >= 0; iEndOfWord--) {
            if (isWordCharacter(textBeforeCursor.charAt(iEndOfWord)))
                break;
            else if (isSentenceSeparator(textBeforeCursor.charAt(iEndOfWord)))
                return 0;
        }

        if (iEndOfWord < 0)
            return 0;

        // Scan word2
        for (iStartOfWord = iEndOfWord; iStartOfWord >= 0; iStartOfWord--)
            if (!isWordCharacter(textBeforeCursor.charAt(iStartOfWord)))
                break;

        if (iStartOfWord + 1 < 0)
            return 0;

        // Write word2 to StringBuilder
        word2.append(textBeforeCursor.subSequence(iStartOfWord + 1,
                iEndOfWord + 1));


		/*
		 * Get the word in front of word2 (i.e. word1)
		 */

        // Skip trailing whitespace & punctuation.
        for (iEndOfWord = iStartOfWord; iEndOfWord >= 0; iEndOfWord--) {
            if (isWordCharacter(textBeforeCursor.charAt(iEndOfWord)))
                break;
            else if (isSentenceSeparator(textBeforeCursor.charAt(iEndOfWord)))
                return 1;
        }

        if (iEndOfWord < 0)
            return 1;

        // Scan word1
        for (iStartOfWord = iEndOfWord; iStartOfWord >= 0; iStartOfWord--)
            if (!isWordCharacter(textBeforeCursor.charAt(iStartOfWord)))
                break;

        if (iStartOfWord + 1 < 0)
            return 1;

        // Write word1 to StringBuilder
        word1.append(textBeforeCursor.subSequence(iStartOfWord + 1,
                iEndOfWord + 1));

        return 2;
    }

    /**
     * Called by onKey() when the user presses a word separator key, which is
     * any non-word key. Commits the current composition, replacing it with the
     * default suggestion if necessary, then commits the character.
     *
     * @param code The character code.
     */
    @SuppressLint("NewApi")
    protected void onWordSeparator(final int code) {
        if (mPendingRequest) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onWordSeparator(code);
                }
            }, 5);
            return;
        }

        // Commit whatever is being typed
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        // Start a batch edit to commit the suggestion (if any) and this whitespace character.
        inputConnection.beginBatchEdit();


        // Delete selected text
        CharSequence selectedText = null;
        selectedText = inputConnection.getSelectedText(0);

        if (selectedText != null && selectedText.length() > 0) {
            // There is selected text. Delete it and return.
            int cursorLocation = getCursorLocation(inputConnection);
            inputConnection.setSelection(cursorLocation, cursorLocation);
            inputConnection.finishComposingText();
            inputConnection.deleteSurroundingText(0, selectedText.length());
            mComposing.setLength(0);
        }

        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        if (isEditingWord(inputConnection, before, after)) {
            // User is inline editing a word. Stop composing and insert text manually.

            int cursor = getCursorLocation(inputConnection);
            inputConnection.finishComposingText();
            inputConnection.setSelection(cursor, cursor);
            inputConnection.commitText("" + (char) code, 1);
            inputConnection.finishComposingText();
            mComposing.setLength(0);
            before.append((char) code);

            inputConnection.endBatchEdit();

            return;
        }


		/*
		 * First, commit the current composition
		 */
        CharSequence committed = "";
        if (mPredictionOn)
            if (mComposing.length() > 0)
                if (mAutoSelect && (isSmartSpacePreceder(code) || code == (int) ' '))
                    // Replace the composing with the default suggestion.
                    committed = pickDefaultCandidate();
                else
                    committed = commitTyped();

        // Set auto-space flag if necessary
        if (code == (int) ' ' && committed.length() > 0)
            bAutoSpace = true;

        // Handle password
        if (mIsPassword) {
            mPassword.append((char) code);
            updatePassword(mShowPassword);
        }

        if (code != (int) ' ')
            // Don't start a new bi-gram
            mLastWord = null;

		/*
		 * Now, commit the character typed
		 */
        StringBuilder commit = new StringBuilder();
        if (code == (int) ' ' && checkDoubleSpace()) {
            commit.append(". ");
        } else {
            commit = new StringBuilder().append((char) code);
            commit = appendSmartSpace(inputConnection, commit, (char) code);
        }

        if (inputConnection != null) {
            // Special case: Replace ". Com" with ".com"
            if (mSmartSpaces) {
                CharSequence dotcom = getTextBeforeCursor(inputConnection, 5);
                if (dotcom.length() == 5
                        && (dotcom.equals(". com") || dotcom.equals(". Com"))) {
                    inputConnection.deleteSurroundingText(5, 0);
                    commit = new StringBuilder(".com").append(commit);
                }
            }

            // Commit!
            inputConnection.commitText(commit, 1);
        }

        inputConnection.endBatchEdit();

        // Update keyboard state
        updateShiftKeyState();
        updateCandidates();
    }

    /**
     * Commits the composition, replacing it with the default suggestion if
     * necessary.
     *
     * @return The committed text.
     */
    private CharSequence commitComposing() {
        CharSequence committed = "";
        if (mPredictionOn) {
            // Commit the composition
            if (mComposing.length() > 0) {
                if (mAutoSelect) {
                    committed = pickDefaultCandidate();
                } else {
                    committed = commitTyped();
                }
            }
        }

        // Update keyboard state
        updateShiftKeyState();
        updateCandidates();

        return committed;
    }

    /**
     * Called by onKey() when the user presses a cursor key.
     *
     * @param code The cursor key code.
     */
    private void onArrowKey(int code) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;

        KeyEvent keyEvent = null;
        switch (code) {
            case BaseKeyboard.KEYCODE_ARROW_UP:
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP);
                break;
            case BaseKeyboard.KEYCODE_ARROW_LEFT:
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case BaseKeyboard.KEYCODE_ARROW_RIGHT:
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case BaseKeyboard.KEYCODE_ARROW_DOWN:
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case BaseKeyboard.KEYCODE_ARROW_HOME:
                inputConnection.setSelection(0, 0);
                break;
            case BaseKeyboard.KEYCODE_ARROW_END:
                String inputContent = getInputContents();
                if (inputContent != null)
                    inputConnection.setSelection(inputContent.length(),
                            inputContent.length());
                break;
            case BaseKeyboard.KEYCODE_ARROW_BACK:
                cursorBackWord(inputConnection);
                break;
            case BaseKeyboard.KEYCODE_ARROW_NEXT:
                cursorNextWord(inputConnection);
                break;
            case BaseKeyboard.KEYCODE_CUT:
                inputConnection.performContextMenuAction(android.R.id.cut);
                mComposing.setLength(0);
                updateCandidates();
                break;
            case BaseKeyboard.KEYCODE_COPY:
                inputConnection.performContextMenuAction(android.R.id.copy);
                break;
            case BaseKeyboard.KEYCODE_PASTE:
                inputConnection.performContextMenuAction(android.R.id.paste);
                break;
            case BaseKeyboard.KEYCODE_SELECT:
                // Select the current word.
                CharSequence before = getWordBeforeCursor(inputConnection);
                CharSequence after = getWordAfterCursor(inputConnection);
                int cursorLocation = 0;
                if (before.length() > 0 || after.length() > 0) {
                    cursorLocation = getCursorLocation(inputConnection);
                    inputConnection.setSelection(cursorLocation - before.length(),
                            cursorLocation + after.length());
                } else
                    inputConnection
                            .performContextMenuAction(android.R.id.startSelectingText);
                break;
            case BaseKeyboard.KEYCODE_SELECT_ALL:
                // Select all text.
                inputConnection.performContextMenuAction(android.R.id.selectAll);
                break;
        }

        if (keyEvent != null)
            inputConnection.sendKeyEvent(keyEvent);
    }

    /**
     * Called by onKey() when the user presses the cancel key.
     */
    protected void onCancelKey() {
        // Commit composition and hide keyboard.
        commitTyped();
        requestHideSelf(0);
        mKeyboardView.closing();
    }

    /**
     * Perform the default app action (e.g. send).
     */
    protected void performEditorAction() {
        EditorInfo ei = safeGetCurrentInputEditorInfo();
        if (ei == null)
            return;

        // Commit the composition first. This will perform a completion or typo-correction if necessary
        commitComposing();

        // Get the action code and send it to the app.
        int editorAction = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null)
            inputConnection.performEditorAction(editorAction);
    }

    /**
     * Toggle between alpha and number pad keyboards
     */
    protected void toggleKeyboardMode() {
        if (isAlphaKeyboard()) {
            getKeyboardView().setKeyboard(mNumPadKeyboard);
            getKeyboardView().setShifted(false, false);
        } else {
            if (mNumRowOn)
                getKeyboardView().setKeyboard(mAlphaNumKeyboard);
            else
                getKeyboardView().setKeyboard(mAlphaKeyboard);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        BaseKeyboard currKeyboard = getKeyboardView().getKeyboard();
        currKeyboard.setImeOptions(getResources(), safeGetCurrentInputEditorInfo());

        getTextForImeAction(0);
    }

    /**
     * Change keyboard mode to arrow keys
     */
    protected void arrowKeyboardMode() {
        dismissAllPopupWindows();
        getKeyboardView().setKeyboard(mArrowPadKeyboard);
    }

    /**
     * Methods to open/close the number row
     */
    protected void openNumRow() {
        // This only applies to the alpha keyboard
        if (getKeyboardView().getKeyboard() == mAlphaKeyboard) {
            // Display the alternate keyboard and update its state
            mAlphaNumKeyboard.setImeOptions(getResources(), safeGetCurrentInputEditorInfo());
            getKeyboardView().setKeyboard(mAlphaNumKeyboard);
            getKeyboardView().setShifted(mAlphaKeyboard.isShifted(), getKeyboardView().getCapsLock());
            mNumRowOn = true;
        }
    }

    protected void closeNumRow() {
        // This only applies to the alpha keyboard
        if (getKeyboardView().getKeyboard() == mAlphaNumKeyboard) {
            // Display the alternate keyboard and update its state
            mAlphaKeyboard.setImeOptions(getResources(), safeGetCurrentInputEditorInfo());
            getKeyboardView().setKeyboard(mAlphaKeyboard);
            getKeyboardView().setShifted(mAlphaNumKeyboard.isShifted(), getKeyboardView().getCapsLock());
            mNumRowOn = false;
        }
    }

    public void dismissAllPopupWindows() {
        dismissAllPopupWindows(false);
    }

    private void dismissAllPopupWindows(boolean rememberLastWindow) {
        if (getKeyboardView() == null)
            return;

        getKeyboardView().dismissAllPopupWindows(rememberLastWindow);
    }

    /**
     * Toggle the number row in the alpha keyboard.
     */
    protected void toggleNumRow() {
        if (getKeyboardView().getKeyboard() == mAlphaKeyboard)
            openNumRow();
        else if (getKeyboardView().getKeyboard() == mAlphaNumKeyboard)
            closeNumRow();
    }

	/*
	 * Methods to handle customized swipe action
	 */

    /**
     * Called by the framework swipe callbacks (see below).
     *
     * @param action The action to perform.
     */
    private void swipe(String action) {
        Assert.assertTrue(action != null && !action.equals(""));

        if (action.equals(getString(R.string.any_key_action_id_voice_input))) {
            openVoiceTyping();
        } else if (action
                .equals(getString(R.string.any_key_action_id_arrow_keypad))) {
            arrowKeyboardMode();
        } else if (action
                .equals(getString(R.string.any_key_action_id_translator))) {
            openTranslator();
        } else if (action.equals(getString(R.string.any_key_action_id_locale))) {
            onLocaleKey();
        } else if (action
                .equals(getString(R.string.swipe_action_id_delete_word))) {
            deleteWordBeforeCursor();
        } else if (action.equals(getString(R.string.swipe_action_id_num_row))) {
            toggleNumRow();
        } else if (action
                .equals(getString(R.string.swipe_action_id_toggle_mode))) {
            toggleKeyboardMode();
        } else if (action
                .equals(getString(R.string.swipe_action_id_do_nothing))) {
        }
    }

    @Override
    public void swipeDown() {
        if (mSwipeNumberRow) {
            // Perform the default swipe down action (close num row or keyboard)
            if (getKeyboardView().getKeyboard() == mAlphaNumKeyboard)
                closeNumRow();
            else if (getKeyboardView().getKeyboard() == mAlphaKeyboard)
                onCancelKey();
        } else {
            // Perform custom user action
            String action;
            SharedPreferences sharedPrefs = getSharedPreferences(
                    Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
            action = sharedPrefs.getString("swipe_down_action",
                    getString(R.string.any_key_action_id_arrow_keypad));
            swipe(action);
        }
    }

    @Override
    public void swipeUp() {
        if (mSwipeNumberRow) {
            // Perform the default swipe up action (open num row)
            if (getKeyboardView().getKeyboard() == mAlphaKeyboard)
                openNumRow();
        } else {
            // Perform custom user action
            SharedPreferences sharedPrefs = getSharedPreferences(
                    Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
            String action = sharedPrefs.getString("swipe_up_action",
                    getString(R.string.any_key_action_id_arrow_keypad));

            swipe(action);
        }
    }

    @Override
    public void swipeRight() {
        // Perform custom user action
        SharedPreferences sharedPrefs = getSharedPreferences(
                Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
        String action = sharedPrefs.getString("swipe_right_action",
                getString(R.string.any_key_action_id_arrow_keypad));

        swipe(action);
    }

    @Override
    public void swipeLeft() {
        // Perform custom user action
        SharedPreferences sharedPrefs = getSharedPreferences(
                Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
        String action = sharedPrefs.getString("swipe_left_action",
                getString(R.string.any_key_action_id_arrow_keypad));

        swipe(action);
    }

    /**
     * Check if we are using the alpha keyboard
     *
     * @return Returns true if keyboard is alpha, false otherwise
     */
    protected boolean isAlphaKeyboard() {
        return (getKeyboardView().getKeyboard() == mAlphaKeyboard || getKeyboardView()
                .getKeyboard() == mAlphaNumKeyboard);
    }

    /**
     * Checks if the user double-tapped the caps key
     */
    private void checkToggleCapsLock() {
        // Switch from caps to caps-lock on double-tap
        long now = System.currentTimeMillis();
        if (now - mLastShiftTime < DOUBLE_CLICK_TIME) {
            getKeyboardView().setCapsLock(true);
            mLastShiftTime = 0;
        } else {
            getKeyboardView().setCapsLock(false);
            mLastShiftTime = now;
        }
    }

    /**
     * Checks if the user double-tapped the space key
     */
    private boolean checkDoubleSpace() {
        if (mDoubleSpace == false)
            return false;

        // Insert a period-space on double-tap space bar
        long now = System.currentTimeMillis();
        if (now - mLastSpaceTime < DOUBLE_CLICK_TIME) {
            // Delete the extra space preceding
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null)
                inputConnection.deleteSurroundingText(1, 0);
            mLastSpaceTime = 0;
            return true;
        }

        mLastSpaceTime = now;
        return false;
    }


    public boolean isWordSeparator(int code) {
        return code > 0 && !isWordCharacter(code);
    }

    public boolean isSentenceSeparator(int code) {
        return code == (int) '\n' || isSmartSpacePreceder(code);
    }


    /**
     * Called by CandidateView if the user tapped a suggestion
     *
     * @param index The index of the suggestion.
     */
    public void touchCandidate(int index) {
//		mProfileTracer.reset();

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null)
            return;
        inputConnection.beginBatchEdit();

        // Complete the selection
        pickSuggestionManually(index);

        // Remember word
        if (index == 0) {
            Suggestion suggestion = mSuggestions.getSuggestion(index);
            rememberWord(suggestion.getWord());
        }

        if (mSmartSpaces) {
            // Append a space after completion
            if (inputConnection != null) {
                CharSequence after = getTextAfterCursor(inputConnection, 1);
                if (after.length() == 0 || after.charAt(0) != ' ')
                    inputConnection.commitText(" ", 1);
                else
                    // There is already a word separator following the word.
                    // Just move the cursor ahead.
                    inputConnection.commitText("", 2);
            }
            bAutoSpace = true;
        }

        inputConnection.endBatchEdit();
        updateShiftKeyState();
        updateCandidates();
    }


    /**
     * Inserts the default suggestion.
     *
     * @return The word inserted.
     */
    @SuppressLint("NewApi")
    protected CharSequence pickDefaultCandidate() {
        if (mSuggestions == null)
            return mComposing;

        String orgWord = mComposing.toString();
        CharSequence committedWord;
        if (!orgWord.equals(mSuggestions.getComposing()))
            committedWord = orgWord;
        else {
            // Insert the default suggestion
            Suggestion defaultSuggestion = mSuggestions.getDefaultSuggestion();
            committedWord = defaultSuggestion.getWord();
        }

        // If committed word is different from the composing, offer more suggestions in menu.
        boolean isDifferent = !DictionaryUtils.removePunc(committedWord.toString()).toLowerCase().startsWith(orgWord.toLowerCase());
        if (committedWord != null && mComposing != null && isDifferent) {
            // Wrap a SpannableString in a SuggestionSpan containing the original word
            ArrayList<String> suggestions = mSuggestions.getWords();
            suggestions.add(0, orgWord);
            String[] words = new String[mSuggestions.size() + 1];
            SuggestionSpan span = new SuggestionSpan(this, Locale.getDefault(), suggestions.toArray(words), SuggestionSpan.FLAG_EASY_CORRECT, SuggestionMenuReceiver.class);
            SpannableString suggestion = new SpannableString(committedWord);
            suggestion.setSpan(span, 0, committedWord.length(), 0);
            committedWord = suggestion;

            // If committed word is different from composing, not including punctuation, this is a typo
            boolean isTypo = !committedWord.toString().toLowerCase().startsWith(orgWord.toLowerCase());
            // Long vibrate for typo correction
            if (mVibrateOnTypoCorrection && isTypo) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATE_LENGTH_ON_TYPO_CORRECTION);

                mIsVibrated = true;
            }
        }

        commitText(committedWord);

        return committedWord;
    }


    /**
     * Insert a particular suggestion.
     *
     * @param index The index of the suggestion to insert.
     * @return The word inserted.
     */
    private CharSequence pickSuggestionManually(int index) {
        String committedStr = null;

        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            // Use app completion
            CompletionInfo ci = mCompletions[index];
            committedStr = ci.getText().toString();
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null)
                inputConnection.commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState();
        } else if (mPredictionOn && mSuggestions != null && index >= 0
                && mSuggestions.size() > 0 && index < mSuggestions.size()) {
            // Use dictionary suggestion
            String prediction = mSuggestions.getSuggestion(index).getWord();
            committedStr = prediction;

            setLastWord(prediction);

            // Commit prediction
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null)
                inputConnection.commitText(prediction, 1);

            // Reset candidates
            mComposing.setLength(0);
        } else if (mComposing.length() > 0) {
            committedStr = mComposing.toString();
            commitTyped();
        }

        return committedStr;
    }

    /**
     * Returns true if we should append a smart space after we append newChar to
     * composing.
     *
     * @param inputConnection The current InputConnection.
     * @param newChar         The character to check.
     * @return true if a space should be appended after newChar.
     */
    private boolean shouldAppendSmartSpace(InputConnection inputConnection, char newChar) {
        if (!mSmartSpaces)
            return false;

        if (inputConnection == null)
            return false;

        CharSequence textBeforeCursor = getTextBeforeCursor(inputConnection, 3);
        if (textBeforeCursor.length() < 1)
            return false;

        char endChar = textBeforeCursor.charAt(textBeforeCursor.length() - 1);
        if ((newChar == '.' || newChar == ':') && Character.isDigit(endChar))
            // User is typing a floating point number, IP address or time
            return false;

        // Don't append after www.
        if (textBeforeCursor != null && textBeforeCursor.length() == 3
                && textBeforeCursor.toString().toLowerCase().equals("www")
                && newChar == '.')
            return false;

        if (isSmartSpacePreceder(newChar) && isWordCharacter(endChar))
            return true;

        return false;
    }

    /**
     * Append a smart space to the composition, if necessary.
     *
     * @param inputConnection The current InputConnection.
     * @param composition     The composition.
     * @param newChar         The character following the composition.
     * @return composing + optional smart space.
     */
    private StringBuilder appendSmartSpace(InputConnection inputConnection,
                                           StringBuilder composition, char newChar) {
        if (!shouldAppendSmartSpace(inputConnection, newChar))
            return composition;

        composition.append(" ");
        bAutoSpace = true;

        return composition;
    }

    /**
     * Creates a regex pattern matcher for words and saves it in mWordMatcher.
     */
    private void createWordMatcher() {
        Pattern wordPattern = Pattern.compile(WORD_REGEX);
        mWordMatcher = wordPattern.matcher("");
    }

    /**
     * Check if a word is a "word". That is, if it is composed entirely of word character.
     *
     * @param word The word to check.
     * @return true if it is a word.
     */
    private boolean isWord(String word) {
        return mWordMatcher.reset(word.toLowerCase()).matches();
    }

    /**
     * Saves the last word typed. This is used for redo, and look-ahead word prediction.
     *
     * @param word The word to save.
     */
    public void setLastWord(CharSequence word) {
        if (!mPredictionOn)
            return;

        String sWord = word.toString();
        if (isWord(sWord))
            mLastWord = sWord.toLowerCase();
        else
            mLastWord = null;
    }

    /**
     * Returns true if the keyboard is in preview mode. In preview mode,
     * all input is ignored except for the action key, which closes
     * the keyboard.
     *
     * @return true if the keyboard is in preview mode.
     */
    public boolean inPreviewMode() {
        return mInPreviewMode;
    }

    public void setPreviewMode(boolean enabled) {
        mInPreviewMode = enabled;
    }

    public void setSuperLabelEnabled(boolean enabled) {
        mSuperLabelEnabled = enabled;
    }

    public boolean isSuperLabelEnabled() {
        return mSuperLabelEnabled;
    }

    /**
     * Checks if this is multi-line input.
     *
     * @return True if it is a multi-line input.
     */
    private boolean isMultiLine() {
        EditorInfo editorInfo = safeGetCurrentInputEditorInfo();
        return (editorInfo.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) > 0;
    }

    /**
     * Called by the framework when a key is pressed. Similar to onKey(), but
     * only called once for repeatable keys.
     */
    @Override
    public void onPress(int code) {
        // Keep track of multi keypresses to avoid vibration on multipress
        if (isRepeatable(code))
            mFirstRepeatablePress = true;
    }

    @Override
    public void onRelease(int primaryCode) {
        // Do nothing.
    }

    /**
     * Checks if this is a repeatable key.
     *
     * @param code The key code to check.
     * @return true if it is a repeatable key.
     */
    private boolean isRepeatable(int code) {
        // TODO: Can we use android:isRepeatable instead?
        if (code == Keyboard.KEYCODE_DELETE || code == 32
                || code == BaseKeyboard.KEYCODE_ARROW_BACK
                || code == BaseKeyboard.KEYCODE_ARROW_NEXT
                || code == BaseKeyboard.KEYCODE_ARROW_UP
                || code == BaseKeyboard.KEYCODE_ARROW_DOWN
                || code == BaseKeyboard.KEYCODE_ARROW_LEFT
                || code == BaseKeyboard.KEYCODE_ARROW_RIGHT
                || code == BaseKeyboard.KEYCODE_DEL_FORWARD)
            return true;

        return false;
    }

    /**
     * Gets the height of the CandidateView.
     *
     * @return The height of the CandidateView, in (device?) pixels.
     */
    public int getCandidateHeight() {
        int height = 0;

        if (mCandidateView != null && mCandidateView.isShown()) {
            height = mCandidateView.getHeight();
        }

        return height;
    }

    /**
     * Updates the currency keys on all keyboards with the user's currency
     * symbol.
     */
    protected void updateCurrencyKeys() {
        updateCurrencyKeys(mAlphaKeyboard);
        updateCurrencyKeys(mAlphaNumKeyboard);
        updateCurrencyKeys(mNumPadKeyboard);
    }

    /**
     * Updates the currency keys on a keyboard with the user's currency symbol.
     *
     * @param keyboard The keyboard to update.
     */
    protected void updateCurrencyKeys(Keyboard keyboard) {
        Key key = ((BaseKeyboard) keyboard).mCurrencyKey;
        if (key == null)
            return;

        if (keyboard == mNumPadKeyboard) {
            // This is the numpad keyboard. Update currency key label and popup
            // menu
            key.label = mDefaultCurrency;
            key.codes[0] = (int) mDefaultCurrency.charAt(0);
            key.popupCharacters = getString(R.string.currency_keys).replace(mDefaultCurrency.toString(), "");

            BaseKeyboard.checkForNulls("updateCurrencyKeys()", key);
        } else
            // This is a regular keyboard. Update currency key long-press symbol
            key.popupCharacters = mDefaultCurrency;
    }

    /**
     * Launch a dictionary update.
     *
     * @param isFirstTime ???
     */
    public void launchDictionaryUpdate(boolean isFirstTime) {
        Intent intent = new Intent(this, DictionaryDownloader.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(Settings.IN_SETTINGS, !isFirstTime);
        intent.putExtra(Settings.BUNDLE_KEY, bundle);
        startActivity(intent);
    }

    public void launchDictionariesUpdate() {
        ArrayList<DictionaryItem> t = new ArrayList<DictionaryItem>();

        DatabaseHelper.safeGetDatabaseHelper(this).getDictionariesForUpdate(this, t);

        String[] list = new String[t.size()];
        for (int i = 0; i < t.size(); i++) {
            DictionaryItem item = t.get(i);
            list[i] = item.lang;
        }

        launchDictionariesUpdate(list);
    }

    /**
     * Launches all dictionaries updates
     */
    public void launchDictionariesUpdate(String[] list) {

        Intent intent = new Intent(this, DictionaryDownloader.class);
        Bundle bundle = new Bundle();
        bundle.putStringArray(DictionaryDownloader.LANG_LIST, list);
        intent.putExtra(Settings.BUNDLE_KEY, bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        DictionaryDownloader.setOnResultListener(new OnResultListener() {
            public void onSuccess() {

                String currLangPrefix = LanguageSelector.getLanguagePreference(KeyboardService.this);

                DictionaryItem dicItem = KeyboardApp.getApp().getUpdater().getDictionaryItem(currLangPrefix);
                if (dicItem != null && !dicItem.isNeedUpdate) {
                    // Set install step to final
                    Installer.setCurrStep(KeyboardService.this,
                            InstallStep.INSTALL_FINISHED);
                }

                KeyboardApp.getApp().getUpdater().markAsReadAll();

                KeyboardApp.getApp().getUpdater().saveDicUpdatedTime(Utils.getTimeMilis());

                KeyboardApp.getApp().removeNotificationTry();

                clearCandidates();

                mSuggestor.reloadLanguageDictionary();
            }

            public void onFail() {
                writeDebug("failed");
            }
        });

        startActivity(intent);

    }

    /**
     * Send keystroke audio and haptic feedback.
     *
     * @param code The code of the keystroke.
     */
    public void keyFeedback(int code) {
        if (mVibrateLength > 0) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(mVibrateLength);
        }
        if (mSoundVolume > 0) {

            if (code == 32) {
                // Spacebar
                mSoundPool.play(mSoundsMap.get(SOUND_CLICK), mSoundVolume,
                        mSoundVolume, 1, 0, 1);
            } else {
                mSoundPool.play(mSoundsMap.get(SOUND_CLICK), mSoundVolume,
                        mSoundVolume, 1, 0, 1);
            }

        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        callTrace("onConfigurationChanged()");
        if (mLastKeyboardState != null)
            mLastKeyboardState.saveKeyboardState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callTrace("onDestroy()");

//        UserDB.close();

        mIME = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        callTrace("onLowMemory()");
    }

    public String toString() {
        return toString("KeyboardService");
    }

    public void setSoundVolume(float val) {
        mSoundVolume = val;
    }

    public float getSoundVolume() {
        return mSoundVolume;
    }

    public boolean isNeedUpgradeApp() {
        return mNeedUpgradeApp;
    }

    public void setNeedUpgradeApp(boolean mIsNeedUpgradeApp) {
        this.mNeedUpgradeApp = mIsNeedUpgradeApp;
    }

    public boolean isNeedUpdateDicts() {
        return mNeedUpdateDicts;
    }

    public void setNeedUpdateDicts(boolean mIsNeedUpdateDicts) {
        this.mNeedUpdateDicts = mIsNeedUpdateDicts;
    }



	/*
	 * DEBUG METHODS
	 */

    public ArrayList<String> getCallTrace() {
        return mCallTrace;
    }

    private void callTrace(String call) {
        if (mCallTrace.size() > 250) {
            mCallTrace.clear();
            mCallTrace.add("...truncated");
        }
        mCallTrace.add(call);

        writeDebug("KeyboardService." + call);
    }

    @SuppressLint("SimpleDateFormat")
    public static void writeDebug(CharSequence message) {
        if (mDebug && getIME() != null) {
            Log.v(KeyboardApp.LOG_TAG, message.toString());

            try {
                // Prepare message
                String now = (new SimpleDateFormat("dd-MMM-yyyy kk:mm:ss.SSS")).format(new Date());
                String entry = now + ": " + message + "\n";

                // Open log file
                OutputStreamWriter logWriter = new OutputStreamWriter(getIME().openFileOutput("debug.log", MODE_APPEND));

                // Write message to log file and close
                logWriter.write(entry, 0, entry.length());
                logWriter.flush();
                logWriter.close();
            } catch (IOException e) {
                Log.e(KeyboardApp.LOG_TAG, e.getMessage());
            }
        }
    }


    /**
     * is keyboard input view created
     *
     * @return
     */
    public boolean isInputViewCreated() {
        return mInputViewCreated;
    }

    public void writeInputDebug() {
        writeDebug("====================================================================================================================================================");
        writeDebug(mInputContents);
        writeDebug("---------------------------------------------------------");
    }

    public String toString(String prefix) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getName());
        String subPrefix = prefix + "\t";
        Utils.appendLine(buffer, prefix, "{");

        // Add important fields
        Utils.appendLine(buffer, subPrefix, "mKeyboardType" + " = " + mKeyboardLayoutId);
        Utils.appendLine(buffer, subPrefix, "mKeyboardLayout" + " = " + mKeyboardLayout);
        Utils.appendLine(buffer, subPrefix, "mKeyboardView" + " = " + mKeyboardView);
        if (mKeyboardView != null)
            Utils.appendLine(buffer, subPrefix, "mKeyboardView.getKeyboard()" + " = " + mKeyboardView.getKeyboard());
        Utils.appendLine(buffer, subPrefix, "mCandidateView" + " = " + mCandidateView);
        Utils.appendLine(buffer, subPrefix, "mCompletions" + " = " + mCompletions);
        Utils.appendLine(buffer, subPrefix, "mPredictions" + " = " + mSuggestions);
        Utils.appendLine(buffer, subPrefix, "mIsAlarmMessageFirstAppeared" + " = " + mIsAlarmMessageFirstAppeared);
        Utils.appendLine(buffer, subPrefix, "mLastWord" + " = " + mLastWord);
        Utils.appendLine(buffer, subPrefix, "mWordMatcher" + " = " + mWordMatcher);
        Utils.appendLine(buffer, subPrefix, "mWindowVisible" + " = " + mWindowVisible);
        Utils.appendLine(buffer, subPrefix, "mPrefsChanged" + " = " + mPrefsChanged);
        Utils.appendLine(buffer, subPrefix, "mComposing" + " = " + mComposing);
        Utils.appendLine(buffer, subPrefix, "mLastDisplayWidth" + " = " + mLastDisplayWidth);
        Utils.appendLine(buffer, subPrefix, "mLastShiftTime" + " = " + mLastShiftTime);
        Utils.appendLine(buffer, subPrefix, "mLastSpaceTime" + " = " + mLastSpaceTime);
        Utils.appendLine(buffer, subPrefix, "mMetaState" + " = " + mMetaState);
        Utils.appendLine(buffer, subPrefix, "bAutoSpace" + " = " + bAutoSpace);
        Utils.appendLine(buffer, subPrefix, "mIsPassword" + " = " + mIsPassword);
        Utils.appendLine(buffer, subPrefix, "mURL" + " = " + mURL);
        Utils.appendLine(buffer, subPrefix, "mNumRowOn" + " = " + mNumRowOn);
        // Utils.appendLine(buffer, subPrefix, "mWordSeparators" + " = " +
        // mWordSeparators);
        Utils.appendLine(buffer, subPrefix, "mSmartSpacePreceders" + " = " + mSmartSpacePreceders);
        Utils.appendLine(buffer, subPrefix, "suggestor" + " = " + mSuggestor);
        Utils.appendLine(buffer, subPrefix, "mVoiceIsShifted" + " = " + mVoiceIsShifted);
        Utils.appendLine(buffer, subPrefix, "mVoiceCapsLock" + " = " + mVoiceCapsLock);
        Utils.appendLine(buffer, subPrefix, "mLang" + " = " + mLanguage);
        Utils.appendLine(buffer, subPrefix, "mTheme" + " = " + mTheme);
        Utils.appendLine(buffer, subPrefix, "mDefaultCurrency" + " = " + mDefaultCurrency);

        // Add KeyboardView
        if (mKeyboardView != null)
            Utils.appendLine(buffer, subPrefix, "mKeyboardView = " + mKeyboardView.toString(subPrefix));

        if (mAlphaKeyboard != null) {
            Utils.appendLine(buffer, subPrefix, "mAlphaKeyboard = ");
            Utils.append(buffer, subPrefix,
                    ((BaseKeyboard) mAlphaKeyboard).toString(subPrefix));
        }
        if (mNumPadKeyboard != null) {
            Utils.appendLine(buffer, subPrefix, "mNumPadKeyboard = ");
            Utils.appendLine(buffer, subPrefix, ((BaseKeyboard) mNumPadKeyboard).toString(subPrefix));
        }
        if (mAlphaNumKeyboard != null) {
            Utils.appendLine(buffer, subPrefix, "mAlphaNumKeyboard = ");
            Utils.appendLine(buffer, subPrefix, ((BaseKeyboard) mAlphaNumKeyboard).toString(subPrefix));
        }

        // Add all native fields
        Utils.appendLine(buffer, subPrefix, "");
        Utils.appendLine(buffer, subPrefix,
                Utils.getClassString(this, subPrefix));
        Utils.appendLine(buffer, prefix, "}");

        return buffer.toString();
    }

    /**
     * KeyboardState stores the current keyboard state in case it needs to be
     * re-initialized.
     *
     * @author Barry Fruitman
     */
    class KeyboardState {

        // Keyboard
        protected int keyboardResID = -1;
        protected boolean bShifted = false;
        protected boolean mCapsLock = false;
        protected Object keyboardParm = null;

        // Popup window
        protected int popupLayoutID = -1;
        protected int popupKeyboardID = -1;
        protected Object popupKeyboardParm = null;

        // Candidate View
        protected boolean isCandidateViewShown = false;
        protected String orgWord;
        protected Suggestions suggestions;
        protected boolean completions;

        protected boolean mChanged = false;
        protected boolean mNeedToUpdate = false;

        // TODO: Add portrait and landscape num row state
        protected KeyboardState() {
            if (mKeyboardView == null)
                return;

            saveKeyboardState();
        }

        protected void saveKeyboardState() {
            if (mKeyboardView == null)
                return;

            // Keyboard State
            if (mKeyboardView != null) {
                keyboardResID = mKeyboardView.getKeyboard().getXMLResID();
                bShifted = mKeyboardView.getKeyboard().isShifted();
            }
            mCapsLock = getKeyboardView().getCapsLock();

            // / Popup Keyboard Status
            if (mKeyboardView != null) {
                popupLayoutID = mKeyboardView.getCurrPopupLayoutID();
                popupKeyboardID = mKeyboardView.getCurrPopupKeyboardID();

                if (mKeyboardView.getCurrPopupLayoutID() == R.id.main_menu)
                    popupKeyboardParm = mKeyboardView.getKeyboard().getAnyKey();
            }
            mChanged = true;
        }

        // Reset suggestions
        protected void resetSuggestions() {
            suggestions = null;
            completions = false;
            orgWord = null;
        }

        // Save current suggestions
        protected void saveSuggestions(Suggestions suggestions,
                                       boolean completions) {
            this.suggestions = suggestions;
            this.completions = completions;
        }
    }
    // End of class


    public static class DebugTracer extends ProfileTracer {
        @Override
        protected void write(String message) {
            writeDebug(message);
        }
    }
}
