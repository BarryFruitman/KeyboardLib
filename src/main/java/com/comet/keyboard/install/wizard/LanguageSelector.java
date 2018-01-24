package com.comet.keyboard.install.wizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.TranslatorView;
import com.comet.keyboard.dictionary.updater.DictionaryDownloader;
import com.comet.keyboard.dictionary.updater.DictionaryFileItem;
import com.comet.keyboard.dictionary.updater.DictionaryItem;
import com.comet.keyboard.dictionary.updater.DictionaryUpdater;
import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;

public class LanguageSelector extends Activity implements View.OnClickListener {
	public static String LANG_LIST = "lang_list";

	private String[] mLangNames;
	private String[] mLangCodes;

	private AlertDialog mLanguageDialog;
	private RadioGroup mRadioLanguages;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mLanguageDialog != null && mLanguageDialog.isShowing())
			mLanguageDialog.dismiss();
	}

	// Current language id
	private String mCurrLangCode;
	private DictionaryItem mDicItem;
	
	private String[] mDicts;

	private static OnResultListener mResultHandler = null;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get parameter
		Bundle bundle = getIntent().getBundleExtra(Settings.BUNDLE_KEY);
		if (bundle != null) {
			mDicts = bundle.containsKey(LANG_LIST)  ? bundle.getStringArray(LANG_LIST) :  null;
		}

		// Load language
		mLangNames = getResources().getStringArray(R.array.language_names);
		mLangCodes = getResources().getStringArray(R.array.language_codes);

		Assert.assertTrue(mLangNames != null);
		Assert.assertTrue(mLangCodes != null);

		// Load language preference
		mCurrLangCode = getLanguagePreference(this);
		
		showLanguageDialog();
	}


	/**
	 * Show selecting dialog
	 */
	public void showLanguageDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View languageLayout = inflater.inflate(R.layout.language,
				(ViewGroup) findViewById(R.id.llLanguage));

		builder.setTitle(R.string.install_select_language_title);
		builder.setView(languageLayout);
		builder.setCancelable(false);

		mLanguageDialog = builder.create();
		mLanguageDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					finish();
					return true;
				}
				return false;
			}
		});

		// Get component from dialog resource
		mRadioLanguages = (RadioGroup) languageLayout.findViewById(R.id.rgLanguage);
		// Load language options
		for (int i = 0 ; i < mLangNames.length ; i++) {
			RadioButton newOption = new RadioButton(this);
			newOption.setId(i);
			newOption.setText(mLangNames[i]);
			newOption.setTag(mLangCodes[i]);
			newOption.setPadding(
					getResources().getDimensionPixelSize(R.dimen.lang_option_padding_left), 
					0, 0, 0);
			newOption.setOnClickListener(this);
			mRadioLanguages.addView(newOption);
		}

		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mLanguageDialog.show();
	}


	/**
	 * Gets dialog message 
	 * 
	 * @return
	 */
	private String getDialogMessage(String langCode){
		String description = String.format(getResources().getString(R.string.install_downloading_description), getLangNameFromCode(langCode));
		return description;
	}


	@Override
	public void onClick(View arg0) {
		int selectedID;

		Assert.assertTrue(mLanguageDialog != null);
		Assert.assertTrue(mLanguageDialog.isShowing());

		selectedID = mRadioLanguages.getCheckedRadioButtonId();
		mCurrLangCode = (String)(((RadioButton)mRadioLanguages.findViewById(selectedID)).getTag());

		// Set the default language to other
		putLanguagePreference();

		// Return when user click the other option
		if (mCurrLangCode.equals(mLangCodes[mLangCodes.length - 1])) {
			fail();
		} else {
			startDownload();
		}
	}


    private final int REQ_DOWNLOAD = 10001;
    public void startDownload() {
        Intent intent = new Intent(this, DictionaryDownloader.class);
        Bundle bundle = new Bundle();
        if(mDicts != null)
            bundle.putStringArray(DictionaryDownloader.LANG_LIST, mDicts);
        intent.putExtra(Settings.BUNDLE_KEY, bundle);

        startActivityForResult(intent, REQ_DOWNLOAD);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQ_DOWNLOAD:
                if(resultCode == 1)
                    success();
                else
                    fail();
                break;
        }
    }


    public static String getLanguagePreference(Context context) {
		SharedPreferences preference = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);       
		String language = preference.getString("language", context.getResources().getString(R.string.lang_code_default));

		return language;
	}


	// Save currency preference
	public void putLanguagePreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putString("language", mCurrLangCode);
		preferenceEditor.commit();
	}


	/**
	 * Get language name from value
	 */
	public String getLangNameFromCode(String prefix) {
		Assert.assertTrue(mLangCodes != null);
		
		return getLangNameFromCode(this, mLangNames, mLangCodes, prefix);
	}
	
	/**
	 * Get language name from value
	 */
	public static String getLangNameFromCode(Context context, String code) {
		String[] mLangNames;
		String[] mLangCodes;
		
		mLangNames = context.getResources().getStringArray(R.array.language_names);
		mLangCodes = context.getResources().getStringArray(R.array.language_codes);
		
		return getLangNameFromCode(context, mLangNames, mLangCodes, code);
	}
	
	public static String getLangNameFromCode(Context context, String[] mLangNames, String[] mLangCodes, String code) {
		for (int i = 0 ; i < mLangCodes.length ; i++) {
			if (code.equals(mLangCodes[i])) {
				return mLangNames[i];
			}
		}

		Log.d(KeyboardApp.LOG_TAG, "Unknown language code '" + code + "' in " + Arrays.toString(mLangCodes) + " - " + Arrays.toString(mLangNames));
		
		Assert.assertTrue(false);
		return null;
	}

	private void success() {
		if(mResultHandler != null)
			mResultHandler.onSuccess();
		
		done();
	}


	private void fail() {
		if(mResultHandler != null)
			mResultHandler.onFail();
		
		done();
	}
	

	private void done() {
        saveCurrentProfile();
        saveTranslateLanguage();
        selectKeyboard();
	}

	
	/**
	 * Save current profile
	 */
	private void saveCurrentProfile() {
		String lang, keyboard;
		SharedPreferences sharedPrefs = getSharedPreferences(Settings.SETTINGS_FILE, 
				Context.MODE_PRIVATE);
		
		lang = sharedPrefs.getString("language", getResources().getString(R.string.lang_code_default));
		keyboard = KeyboardLayout.getCurrentLayout().getId();
		
		LanguageProfileManager.getProfileManager().addProfile(lang, keyboard);
		LanguageProfileManager.getProfileManager().setCurrentProfile(lang);
	}

	
	
	/**
	 * Save translate language
	 */
	private void saveTranslateLanguage() {
		// Save default translating language
		String langPrefix, lang;

		SharedPreferences preference = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editPrefs = preference.edit();
		langPrefix = preference.getString("language", getResources().getString(R.string.lang_code_default));
		lang = LanguageSelector.getLangNameFromCode(this, langPrefix);
		
		// Set from language
		editPrefs.putString(TranslatorView.PREF_LANG_FROM, lang);
		if (lang.equals(getResources().getString(R.string.lang_english))) {
			editPrefs.putString(TranslatorView.PREF_LANG_TO, getResources().getString(R.string.lang_spanish));
		} else {
			editPrefs.putString(TranslatorView.PREF_LANG_TO, getResources().getString(R.string.lang_english));
		}
		editPrefs.commit();
	}
	
	
	private void selectKeyboard() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), KeyboardSelector.class);
		
		startActivity(intent);
	}
}