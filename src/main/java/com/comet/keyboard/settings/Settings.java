/*
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import java.io.File;

import junit.framework.Assert;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.comet.keyboard.About;
import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.comet.keyboard.theme.KeyboardTheme;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.KillActivityReceiver;
import com.comet.keyboard.util.Utils;


public class Settings extends PreferenceActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 99;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 98;
    private static final String TAG = Settings.class.getSimpleName();
    public static String IN_SETTINGS = "in_settings";
    public static String BUNDLE_KEY = "get_bundle";

    public class IndexResult {
        int index;
    }

    public final static String LAUNCH_ACTIVITY_KEY = "launch_activity_key";
    public final static String SETTINGS_BACKUP_FILE = "keyboard_for_paid.prefs";

    public final static String SETTINGS_FILE = "keyboard.prefs";
    private final static int REQ_IMAGE_PICK = 1;
    private final static int REQ_EMAIL_SEND = 100;
    private final static int REQ_LANGUAGE_SELECTOR = 101;
    private final static int REQ_KEYBOARD_SELECTOR = 102;

    private final static String SURVEY_WEB_URL = "http://www.surveymonkey.com/s/S579B9K";
    private String mLaunchKey = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(Utils.formatStringWithAppName(this, R.string.title_settings));

        // Retrieve activity parameter
        mLaunchKey = getIntent().getStringExtra(LAUNCH_ACTIVITY_KEY);

        // Save preferences to a file
        getPreferenceManager().setSharedPreferencesName(SETTINGS_FILE);

        // Load the page layout
        addPreferencesFromResource(R.xml.settings);

        initPrefs();

        // Show preference screen
        if (mLaunchKey != null && !mLaunchKey.equals("")) {
            openPreferenceScreen(mLaunchKey);
        }

        mKillReceiver = new KillActivityReceiver(this);
        registerReceiver(mKillReceiver, IntentFilter.create("killMyActivity", "text/plain"));
    }


    /**
     * Initializes all the Preferences. Only needs to be called once.
     */
    private void initPrefs() {
        Preference p;
        ListPreference lp;

        p = findPreference("about");
        if (p != null)
            p.setOnPreferenceClickListener(aboutListener);

        p = findPreference("rate_app");
        if (p != null)
            p.setOnPreferenceClickListener(rateListener);

        p = findPreference("survey");
        if (p != null) {
            p.setSummary(Utils.formatStringWithAppName(this, R.string.settings_survey_description));
            p.setOnPreferenceClickListener(surveyListener);
        }

//        p = findPreference("knowledge_base");
//        if (p != null)
//            p.setOnPreferenceClickListener(knowledgeBaseListener);
//
//        p = findPreference("open_ticket");
//        if (p != null)
//            p.setOnPreferenceClickListener(openTicketClickListener);
//
//        p = findPreference("my_tickets");
//        if (p != null)
//            p.setOnPreferenceClickListener(myTicketsClickListener);
//
//        p = findPreference("email_support");
//        if (p != null)
//            p.setOnPreferenceClickListener(mEmailListener);

        p = findPreference("key_follow_twitter");
        if (p != null)
            p.setOnPreferenceClickListener(followUsListener);

        p = findPreference("key_follow_facebook");
        if (p != null)
            p.setOnPreferenceClickListener(followUsListener);

        p = findPreference("key_follow_google_plus");
        if (p != null)
            p.setOnPreferenceClickListener(followUsListener);

        p = findPreference("key_follow_youtube");
        if (p != null)
            p.setOnPreferenceClickListener(followUsListener);

        // Set callback listeners for every check box
        p = (Preference) findPreference("language");
//		CharSequence entry = lp.getEntry();
//		if (entry == null)
//			entry = getString(R.string.unknown);
//		String language = entry.toString();
//		lp.setSummary(language);
        p.setOnPreferenceClickListener(languageListener);
        p.setOnPreferenceChangeListener(preferenceChangeListener);

        p = (Preference) findPreference("keyboard");
//		entry = lp.getEntry();
//		if (entry == null)
//			entry = getString(R.string.unknown);
//		String sKeyboard = entry.toString();
//		lp.setSummary(sKeyboard);
        p.setOnPreferenceClickListener(keyboardListener);
        p.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("currency");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("behavior_locale_button");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        p = findPreference("smart_spaces");
        if (p != null)
            p.setOnPreferenceChangeListener(preferenceChangeListener);

        p = findPreference("show_suggestions");
        if (p != null)
            p.setOnPreferenceChangeListener(preferenceChangeListener);

        p = findPreference("auto_select");
        if (p != null) {
            p.setDependency("show_suggestions");
            p.setOnPreferenceChangeListener(preferenceChangeListener);
        }

        p = findPreference("include_corrections");
        if (p != null) {
            p.setDependency("show_suggestions");
            p.setOnPreferenceChangeListener(preferenceChangeListener);
        }
        p = findPreference("nextword");
        if (p != null) {
            p.setDependency("show_suggestions");
            p.setOnPreferenceChangeListener(preferenceChangeListener);
        }
        p = findPreference("include_contacts");
        if (p != null) {
            p.setDependency("show_suggestions");
//            p.setOnPreferenceChangeListener(preferenceChangeListener);
            p.setOnPreferenceClickListener(preferenceClickListener);
        }
        p = findPreference("shortcut");
        if (p != null)
            p.setOnPreferenceClickListener(shortcutClickListener);

        p = findPreference("auto_caps");
        if (p != null)
            p.setOnPreferenceChangeListener(preferenceChangeListener);

        // Sets vibrate time summary and click listener
        p = findPreference("vibrate");
        if (p != null)
            p.setOnPreferenceClickListener(vibrateClickListener);

        // set long press click listener
        p = findPreference("long_press_duration");
        if (p != null)
            p.setOnPreferenceClickListener(longPressClickListener);

        // set sound volume listener
        p = findPreference("sound_volume");
        if (p != null)
            p.setOnPreferenceClickListener(soundVolumeClickListener);

        // Disable speech option when does not support speech capability on user phone.
        PreferenceCategory pc = (PreferenceCategory) findPreference("speech_input");
        p = findPreference("speech_input_closest_match");
        if (p != null) {
            if (Utils.isExistVoiceRecognizeActivity(this))
                p.setOnPreferenceChangeListener(preferenceChangeListener);
            else {
                pc.setEnabled(false);
                p.setEnabled(false);
            }
        }

        lp = (ListPreference) findPreference("any_key_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("any_key_long_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("return_key_long_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        p = (Preference) findPreference("theme");
        if (p != null)
            p.setOnPreferenceClickListener(themeClickListener);

        p = findPreference("photo_wallpaper");
        if (p != null)
            p.setOnPreferenceClickListener(wallpaperPhotoClickListener);

        p = findPreference("portrait_key_height");
        if (p != null)
            p.setOnPreferenceClickListener(portraitKeyHeightClickListener);

        p = findPreference("landscape_key_height");
        if (p != null)
            p.setOnPreferenceClickListener(landscapeKeyHeightClickListener);

        p = findPreference("keyboard_padding_bottom");
        if (p != null)
            p.setOnPreferenceClickListener(keyboardPaddingBottomClickListener);

        p = findPreference("candidates_height");
        if (p != null)
            p.setOnPreferenceClickListener(candidatesHeightClickListener);

        // Update swipe options
        p = findPreference("swipe_num_row");
        if (p != null)
            p.setOnPreferenceChangeListener(preferenceChangeListener);

//        // submit logs
//        p = findPreference("submit_logs");
//        if (p != null)
//            p.setOnPreferenceClickListener(submitLogsClickListener);

        p = findPreference("debug_mode");
        if (p != null)
            p.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("swipe_left_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("swipe_right_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("swipe_up_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        lp = (ListPreference) findPreference("swipe_down_action");
        if (lp != null)
            lp.setOnPreferenceChangeListener(preferenceChangeListener);

        updatePrefs();
    }


    private Preference.OnPreferenceClickListener preferenceClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().equals("include_contacts")) {

                final CheckBoxPreference contactsPreference = (CheckBoxPreference) preference;
                final boolean state = ((Boolean) contactsPreference.isChecked()).booleanValue();

                if (state == false) {
                    contactsPreference.setChecked(false);
                    return true;
                }

                // Get permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(Settings.this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                    contactsPreference.setChecked(false);
                    return true;
                }
            }

            return false;
        }
    };


    /**
     * Listener for preference changes. Performs various housekeeping duties.
     */
    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String name, key;

            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            key = preference.getKey();

            if (preference instanceof ListPreference) {
                if (!key.equals("behavior_locale_button")) { // HACK !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    // Display the current value in the summary
                    name = getNameFromValue(Settings.this, key, (String) newValue);
                    preference.setSummary(name);
                }

                editor.putString(key, (String) newValue);
                editor.commit();
            } else if (preference.getKey().equals("include_contacts")
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                final CheckBoxPreference contactsPreference = (CheckBoxPreference) preference;
                final boolean state = ((Boolean) contactsPreference.isChecked()).booleanValue();

                if (state == false) {
//                    contactsPreference.setChecked(false);
                    return false;
                }

                final int granted = ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.READ_CONTACTS);

                // Get permission
                if (ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(Settings.this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                    return true;
                }
            } else {
                final CheckBoxPreference contactsPreference = (CheckBoxPreference) preference;
                final boolean state = ((Boolean) contactsPreference.isChecked()).booleanValue();

                editor.putBoolean(key, ((Boolean) newValue).booleanValue());
                editor.commit();
            }

            if (key.equals("debug_mode")) {
                KeyboardService.mDebug = (Boolean) newValue;
                if (KeyboardService.mDebug)
                    Toast.makeText(Settings.this,
                            R.string.personal_info_can_be_send_to_server,
                            Toast.LENGTH_LONG).show();

                editor.putBoolean(key, ((Boolean) newValue).booleanValue());
                editor.commit();
            }

            // Notify the IME that prefs have changed.
            if (KeyboardService.getIME() != null)
                KeyboardService.getIME().onPrefsChanged();

            updatePrefs();

            return true;
        }
    };

    /**
     * Updates the Preference summaries
     */
    private void updatePrefs() {
        Preference p;
        ListPreference lp;

        // Set callback listeners for every check box
        p = findPreference("language");
        String language = getNameFromValue(this, "language", LanguageSelector.getLanguagePreference(this));
        p.setSummary(language);

        p = findPreference("keyboard");
        String keyboardName = getNameFromValue(this, "keyboard", KeyboardSelector.getKeyboardPreference(this));
        p.setSummary(keyboardName);

        lp = (ListPreference) findPreference("currency");
        if (lp != null) {
            String sCurrency = lp.getEntry().toString();
            lp.setSummary(sCurrency);
        }

        // sets vibrate time summary and click listener
        p = findPreference("vibrate");
        if (p != null) {
            int vibrateLength = VibrateSetting.getVibrateTimePreference(this);
            if (vibrateLength > 0)
                p.setSummary(getString(R.string.vibrate_time_summary, vibrateLength));
        }

        // set long press click listener
        p = findPreference("long_press_duration");
        if (p != null) {
            p.setSummary(getString(R.string.long_press_duration_summary,
                    LongPressDurationSetting.getLongPressDurationPreference(this)));
        }

        // set sound volume listener
        p = findPreference("sound_volume");
        if (p != null) {
            p.setSummary(getString(R.string.volume_val_summary,
                    (int) (SoundVolumeSetting.getVolumePreference(this) * 100)));
        }

        lp = (ListPreference) findPreference("any_key_action");
        if (lp != null) {
            String anyKeyAction = lp.getEntry().toString();
            lp.setSummary(anyKeyAction);
        }

        lp = (ListPreference) findPreference("any_key_long_action");
        if (lp != null) {
            String anyKeyLongAction = lp.getEntry().toString();
            lp.setSummary(anyKeyLongAction);
        }

        lp = (ListPreference) findPreference("return_key_long_action");
        if (lp != null) {
            String returnKeyLongAction = lp.getEntry().toString();
            lp.setSummary(returnKeyLongAction);
        }

        p = (Preference) findPreference("theme");
        if (p != null) {
            KeyboardTheme currTheme = KeyboardThemeManager.getCurrentTheme();
            if (!currTheme.isPublicTheme())
                p.setSummary(currTheme.getName());
            else
                p.setSummary(getResources().getString(R.string.theme_none_photo));
        }

        // Swipe gestures
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        String sSwipeLeft = prefs.getString("swipe_left_action",
                getString(R.string.swipe_action_id_delete_word));
        String sSwipeRight = prefs.getString("swipe_right_action",
                getString(R.string.any_key_action_id_locale));
        String sSwipeUp = prefs.getString("swipe_up_action",
                getString(R.string.swipe_action_id_do_nothing));
        String sSwipeDown = prefs.getString("swipe_down_action",
                getString(R.string.swipe_action_id_do_nothing));

        boolean bSwipeNumberRow = prefs.getBoolean("swipe_num_row", true);
        String[] names = getResources().getStringArray(
                R.array.settings_swipe_action_names);
        String[] values = getResources().getStringArray(
                R.array.settings_swipe_action_values);
        for (int i = 0; i < names.length; i++) {
            if (values[i].equals(sSwipeLeft))
                sSwipeLeft = names[i];

            if (values[i].equals(sSwipeRight))
                sSwipeRight = names[i];

            if (values[i].equals(sSwipeUp))
                sSwipeUp = names[i];

            if (values[i].equals(sSwipeDown))
                sSwipeDown = names[i];
        }
        lp = (ListPreference) findPreference("swipe_left_action");
        if (lp != null)
            lp.setSummary(sSwipeLeft);

        lp = (ListPreference) findPreference("swipe_right_action");
        if (lp != null)
            lp.setSummary(sSwipeRight);

        lp = (ListPreference) findPreference("swipe_up_action");
        if (lp != null) {
            lp.setEnabled(!bSwipeNumberRow);
            lp.setSummary(sSwipeUp);
        }

        lp = (ListPreference) findPreference("swipe_down_action");
        if (lp != null) {
            lp.setEnabled(!bSwipeNumberRow);
            lp.setSummary(sSwipeDown);
        }
    }


    /**
     * Activity result callback for certain activities.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_EMAIL_SEND) {
            // Support email
            // Google's Bug (RESULT_OK -> 0)
            // http://code.google.com/p/android/issues/detail?id=5512
            if (resultCode == 0) {
                Toast.makeText(
                        Settings.this,
                        Settings.this.getResources().getString(
                                R.string.contact_email_sent_ok),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(
                        Settings.this,
                        Settings.this.getResources().getString(
                                R.string.contact_email_sent_fail),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_LANGUAGE_SELECTOR) {
            updatePrefs();
        } else if (requestCode == REQ_KEYBOARD_SELECTOR) {
            updatePrefs();
        } else if (requestCode == REQ_IMAGE_PICK) {
            // Photo wallpaper image picker
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = managedQuery(selectedImageUri, projection, null, null, null);
                int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String selectedImagePath = cursor.getString(column_index_data);

                // Launch the photo wallpaper setting activity.
                Intent intent = new Intent(Settings.this, WallpaperPhoto.class);
                intent.putExtra("image_path", selectedImagePath);
                startActivity(intent);
            }
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus)
            return;

        updatePrefs();
    }


    public void onDestroy() {
        super.onDestroy();

        // Unregister the kill receiver
        unregisterReceiver(mKillReceiver);
    }


    /**
     * Get the name of an option from its value. e.g. if key="language" and
     * value="en", this method returns "English".
     *
     * @param context A context (this activity).
     * @param key     The setting key.
     * @param value   The option value.
     * @return The option name.
     */
    public static String getNameFromValue(Context context, String key, String value) {
        String names[] = {""};
        String values[] = {""};
        String name = null;

        // Get the value and name arrays for this key.
        if (key.equals("language")) {
            names = context.getResources().getStringArray(R.array.language_names);
            values = context.getResources().getStringArray(R.array.language_codes);
        } else if (key.equals("keyboard")) {
            names = context.getResources().getStringArray(R.array.keyboard_names);
            values = context.getResources().getStringArray(R.array.keyboard_ids);
        } else if (key.equals("currency")) {
            names = context.getResources().getStringArray(R.array.currency_names);
            values = context.getResources().getStringArray(R.array.currency_symbols);
        } else if (key.equals("theme")) {
            names = context.getResources().getStringArray(R.array.theme_names);
            values = context.getResources().getStringArray(R.array.theme_ids);
        } else if (key.equals("any_key_action")
                || key.equals("any_key_long_action")
                || key.equals("return_key_long_action")) {
            names = context.getResources().getStringArray(R.array.settings_any_key_action_names);
            values = context.getResources().getStringArray(R.array.settings_any_key_action_values);
        } else if (key.equals("behavior_locale_button")) {
            names = context.getResources().getStringArray(R.array.settings_locale_button_behavior_names);
            values = context.getResources().getStringArray(R.array.settings_locale_button_behavior_values);
        }

        // Find the name of the current setting for the corresponding new value
        for (int iValue = 0; iValue < values.length; iValue++) {
            if (values[iValue].equals(value)) {
                name = names[iValue];
                break;
            }
        }

        return name;
    }

    /**
     * Enables/disables debug mode
     *
     * @param flag
     */
    public static void enableDebugMode(boolean flag) {
        KeyboardService.mDebug = flag;
        KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit().putBoolean("debug_mode", flag).commit();
    }

    /**
     * Delete debug log
     */
    public void deleteDebugLogs() {
        File log = getFileStreamPath("debug.log");
        if (log.exists() && log.canWrite()) {
            log.delete();
        }
    }


    /******************************
     * OnPreferenceClickListeners *
     ******************************/

    /**
     * Listener for "About" option.
     */
    private Preference.OnPreferenceClickListener languageListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {

            // Launch language selector
            Intent intent = new Intent();
            intent.setClass(Settings.this, LanguageSelector.class);
            startActivityForResult(intent, REQ_LANGUAGE_SELECTOR);
            return true;
        }
    };

    /**
     * Listener for "About" option.
     */
    private Preference.OnPreferenceClickListener keyboardListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {

            // Launch keyboard selector
            Intent intent = new Intent(Settings.this, KeyboardSelector.class);
            startActivityForResult(intent, REQ_KEYBOARD_SELECTOR);
            return true;
        }
    };

    /**
     * Listener for "About" option.
     */
    private Preference.OnPreferenceClickListener aboutListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {

            // Launch about activity
            Intent intent = new Intent(Settings.this, About.class);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for suggestions height option.
     */
    private Preference.OnPreferenceClickListener candidatesHeightClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this,
                    CandidateHeightSetting.class);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for social media options.
     */
    private Preference.OnPreferenceClickListener followUsListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            String url = "";
            if (p.getKey().equals("key_follow_twitter")) {
                url = Settings.this.getResources().getString(
                        R.string.settings_follow_twitter_url);
            } else if (p.getKey().equals("key_follow_facebook")) {
                url = Settings.this.getResources().getString(
                        R.string.settings_follow_facebook_url);
            } else if (p.getKey().equals("key_follow_google_plus")) {
                url = Settings.this.getResources().getString(
                        R.string.settings_follow_google_plus_url);
            } else if (p.getKey().equals("key_follow_youtube")) {
                url = Settings.this.getResources().getString(
                        R.string.settings_follow_youtube_url);
            } else {
                return false;
            }

            // Open a social media site/app.
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for bottom padding option.
     */
    private Preference.OnPreferenceClickListener keyboardPaddingBottomClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this,
                    KeyboardPaddingBottomSetting.class);
            startActivity(intent);
            return true;
        }
    };

//    /**
//     * Listener for help option.
//     */
//    private Preference.OnPreferenceClickListener knowledgeBaseListener = new Preference.OnPreferenceClickListener() {
//        public boolean onPreferenceClick(Preference p) {
//
//            // Launch the knowledge base in a browser
//            Uri uri = Uri.parse("http://support.cometapps.com/forums/20173382-tips-how-to-s"); // TEMPORARILY CHANGED FROM http://support.cometapps.com/forums
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//            return true;
//        }
//    };

    /**
     * Listener for portrait key height option.
     */
    private Preference.OnPreferenceClickListener landscapeKeyHeightClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this, KeyHeightSetting.class);
            intent.putExtra(KeyHeightSetting.PARMS_ORIENTATION_MODE,
                    KeyHeightSetting.PREFERENCE_LANDSCAPE_KEY_HEIGHT);
            startActivity(intent);
            return true;
        }
    };


    /**
     * Listener for support email option.
     */
    private Preference.OnPreferenceClickListener mEmailListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            final Intent emailIntent = new Intent(
                    android.content.Intent.ACTION_SEND);
            emailIntent.setType("plain/text");

            emailIntent.putExtra(
                    android.content.Intent.EXTRA_EMAIL,
                    new String[]{Settings.this.getResources().getString(
                            R.string.contact_email_address)});

            startActivityForResult(Intent.createChooser(
                    emailIntent,
                    Settings.this.getResources().getString(
                            R.string.contact_email_dlg_title)), REQ_EMAIL_SEND);

            return true;
        }
    };

    private KillActivityReceiver mKillReceiver;

//    /**
//     * Listener for open tickets option.
//     */
//    private Preference.OnPreferenceClickListener myTicketsClickListener = new Preference.OnPreferenceClickListener() {
//        public boolean onPreferenceClick(Preference p) {
//
//            // Launch the ticket list in a browser
//            Uri uri = Uri.parse("http://support.cometapps.com/requests");
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//            return true;
//        }
//    };
//
//    /**
//     * Listener for support ticket option.
//     */
//    private Preference.OnPreferenceClickListener openTicketClickListener = new Preference.OnPreferenceClickListener() {
//        public boolean onPreferenceClick(Preference p) {
//
//            // Launch the ticket list in a browser
//            Uri uri = Uri.parse("http://support.cometapps.com/anonymous_requests/new");
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//            return true;
//        }
//    };

    /**
     * Listener for portrait key height option.
     */
    private Preference.OnPreferenceClickListener portraitKeyHeightClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this, KeyHeightSetting.class);
            intent.putExtra(KeyHeightSetting.PARMS_ORIENTATION_MODE,
                    KeyHeightSetting.PREFERENCE_PORTRAIT_KEY_HEIGHT);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for "Rate Us" option.
     */
    private Preference.OnPreferenceClickListener rateListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            // Launch about activity
            Settings.this.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(KeyboardApp.getApp().getRatingUrl())));

            return true;
        }
    };


    /**
     * Listener for shortcuts option.
     */
    private Preference.OnPreferenceClickListener shortcutClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            // Display new shortcut form
            Intent intent = new Intent(Settings.this, ShortcutSetting.class);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for survey option.
     */
    private Preference.OnPreferenceClickListener surveyListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Settings.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SURVEY_WEB_URL)));
            return true;
        }
    };

    /**
     * Listener for theme option.
     */
    private Preference.OnPreferenceClickListener themeClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            // Launch a theme chooser
            Intent intent = new Intent(Settings.this, ThemeSelector.class);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for changing vibrate time option.
     */
    private Preference.OnPreferenceClickListener vibrateClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this, VibrateSetting.class);
            startActivity(intent);
            return true;
        }
    };

    /**
     * Listener for changing vibrate time option.
     */
    private Preference.OnPreferenceClickListener soundVolumeClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this, SoundVolumeSetting.class);
            startActivity(intent);
            return true;
        }
    };

//    /**
//     * Listener for submit logs
//     */
//    private Preference.OnPreferenceClickListener submitLogsClickListener = new Preference.OnPreferenceClickListener() {
//        public boolean onPreferenceClick(Preference p) {
//
//            File file = getFileStreamPath("debug.log");
//            if (file.exists() && file.length() > 0) {
//
//                HttpLogSender sender = new HttpLogSender() {
//                    ProgressDialog loaderDialog;
//
//                    @Override
//                    protected void onPreExecute() {
//                        loaderDialog = new ProgressDialog(Settings.this);
//                        loaderDialog.setMessage(Settings.this.getString(R.string.submit_logs_loader));
//                        loaderDialog.setCancelable(false);
//                        loaderDialog.setOnCancelListener(new OnCancelListener() {
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                cancel(true);
//                            }
//                        });
//                        loaderDialog.show();
//                    }
//
//                    @Override
//                    protected void onPostExecute(Boolean result) {
//                        loaderDialog.dismiss();
//
//                        if (!isCancelled()) {
//                            if (result == false) {
//                                Toast.makeText(Settings.this,
//                                        R.string.submit_logs_fail_msg,
//                                        Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(Settings.this,
//                                        R.string.submit_logs_success_msg,
//                                        Toast.LENGTH_SHORT).show();
//                            }
//
//                            enableDebugMode(false);
//                            ((CheckBoxPreference) findPreference("debug_mode")).setChecked(false);
//
//                            deleteDebugLogs();
//                        }
//                    }
//
//                };
//                sender.execute(file);
//
//            } else {
//                Toast.makeText(Settings.this, R.string.submit_logs_no_file,
//                        Toast.LENGTH_LONG).show();
//            }
//
//            return true;
//        }
//    };


    /**
     * Listener for changing vibrate time option.
     */
    private Preference.OnPreferenceClickListener longPressClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {
            Intent intent = new Intent(Settings.this, LongPressDurationSetting.class);
            startActivity(intent);
            return true;
        }
    };


    /**
     * Listener for wallpaper photo option.
     */
    private Preference.OnPreferenceClickListener wallpaperPhotoClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference p) {

            // Request permission, if necessary.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(Settings.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                showWallpaperChooser();
            }

            return true;
        }
    };


    private void showWallpaperChooser() {
        // Launch a photo chooser
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_IMAGE_PICK);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        showWallpaperChooser();
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.wallpaper_permission_dialog_message)
                                .setTitle(R.string.wallpaper_permission_dialog_title);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.dismiss();
                            }
                        });

                        final AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    Log.w(TAG, "Unexpected permission request for " + permissions[0]);
                }
                break;

            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:
                if (permissions[0].equals(Manifest.permission.READ_CONTACTS)) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        CheckBoxPreference contactsPreference = (CheckBoxPreference) findPreference("include_contacts");
                        contactsPreference.setChecked(true);
                        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                        editor.putBoolean("include_contacts", true);
                        editor.commit();
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.contacts_permission_dialog_message)
                                .setTitle(R.string.contacts_permission_dialog_title);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.dismiss();
                            }
                        });

                        final AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    Log.w(TAG, "Unexpected permission request for " + permissions[0]);
                }
                break;
        }
    }


    /**
     * Find a preference screen by using key
     *
     * @param key         A PreferenceScreen key
     * @param group       ???
     * @param resultGroup ???
     * @param resultIndex ???
     * @param type        ???
     * @return ???
     */
    private PreferenceGroup findPreferenceGroupForPreference(String key,
                                                             PreferenceGroup group, PreferenceGroup resultGroup,
                                                             IndexResult resultIndex, @SuppressWarnings("rawtypes") Class type) {
        IndexResult orgIndex = new IndexResult();

        if (group == null) {
            group = (PreferenceGroup) getPreferenceScreen();
        }

        orgIndex.index = resultIndex.index;

        if (group.getClass().equals(type)) {
            resultGroup = group;
            resultIndex.index = 0;
        }

        int count;
        Preference pref;
        PreferenceGroup ret;

        count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            pref = group.getPreference(i);

            if (pref != null && pref.getKey() != null
                    && pref.getKey().equals(key)) {
                return resultGroup;
            }

            resultIndex.index++;
            if (pref instanceof PreferenceGroup) {
                ret = findPreferenceGroupForPreference(key,
                        (PreferenceGroup) pref, resultGroup, resultIndex, type);
                if (ret != null) {
                    return ret;
                }
            }
        }

        if (group.getClass().equals(type)) {
            resultIndex.index = orgIndex.index;
        }

        return null;
    }

    /**
     * Open a particular preference screen.
     *
     * @param key The key of the screen to open.
     */
    private void openPreferenceScreen(String key) {
        Assert.assertTrue(key != null && !key.equals(""));

        IndexResult result = new IndexResult();
        result.index = 0;
        PreferenceScreen screen = (PreferenceScreen) findPreferenceGroupForPreference(
                key, null, null, result, PreferenceScreen.class);
        if (screen != null) {
            screen.onItemClick(null, null, result.index, 0);
        }
    }
}
