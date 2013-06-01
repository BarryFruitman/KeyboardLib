package com.comet.keyboard.settings;

import java.util.Arrays;

import junit.framework.Assert;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
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
import com.comet.keyboard.dictionary.updater.DictionaryItem;
import com.comet.keyboard.dictionary.updater.DictionaryUpdater;
import com.comet.keyboard.settings.LanguageProfileManager;
import com.comet.keyboard.settings.Settings;

public class LanguageSelector extends Activity implements View.OnClickListener {
	public static String LANG_LIST = "lang_list";

	private String[] mLangNames;
	private String[] mLangCodes;

	private AlertDialog mLanguageDialog;
	private RadioGroup mRadioLanguages;


	// Current language id
	private String mCurrLangCode;
	private DictionaryItem mDicItem;
	
	private String[] mDicts;

	private static OnResultListener mResultListener = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get parameter
		Bundle bundle = getIntent().getBundleExtra(Settings.BUNDLE_KEY);
		if (bundle != null) {
//			mInSettings = bundle.getBoolean(Settings.IN_SETTINGS, false);
			mDicts = bundle.containsKey(LANG_LIST)  ? bundle.getStringArray(LANG_LIST) :  null;
		}

		// Load language
		mLangNames = getResources().getStringArray(R.array.language_names);
		mLangCodes = getResources().getStringArray(R.array.language_codes);

		Assert.assertTrue(mLangNames != null);
		Assert.assertTrue(mLangCodes != null);

		// Retrieve system language
//		Locale currLocale = java.util.Locale.getDefault();
//		mCurrLocaleLang = currLocale.getLanguage().toString();
		
		// Load language preference
		mCurrLangCode = getLanguagePreference(this);
		
		showLanguageDialog();
	}




	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mLanguageDialog != null && mLanguageDialog.isShowing())
			mLanguageDialog.dismiss();
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
			if(KeyboardApp.getApp().mAppStore == KeyboardApp.AppStore.Nook)
				newOption.setTextColor(getResources().getColor((R.color.black)));
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



	@Override
	public void onClick(View arg0) {
		int selectedID;

		Assert.assertTrue(mLanguageDialog != null);
		Assert.assertTrue(mLanguageDialog.isShowing());

		selectedID = mRadioLanguages.getCheckedRadioButtonId();
		mCurrLangCode = (String)(((RadioButton)mRadioLanguages.findViewById(selectedID)).getTag());

		// Save shared pref
		putLanguagePreference();

		// Return when user click the other option
		if (mCurrLangCode.equals(getString(R.string.lang_code_other))) {
			success();
		} else if(DictionaryUpdater.isDictionaryExist(this, KeyboardApp.getApp().getUpdater().getDictionaryItem(mCurrLangCode))) {
			success();
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

	public static void setOnResultListener(OnResultListener handler) {
		mResultListener = handler;
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
	

	protected void success() {
		setResult(1);
		done();
	}


	protected void fail() {
		setResult(0);
		done();
	}

	
	
	/**
	 * Check connection to internet
	 */
	public boolean isDeviceOnline(){
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (cm.getActiveNetworkInfo() == null) {
	      return false;
	    }
	    return cm.getActiveNetworkInfo().isConnected();
	}


	
	
	
	public static String getNameFromCode(Context context, String langCode) {
		String[] names = context.getResources().getStringArray(R.array.language_names); 
		String[] codes = context.getResources().getStringArray(R.array.language_codes);

		for (int i = 0 ; i < codes.length ; i++) {
			String code = codes[i];
			if (code.equals(langCode)) {
				return names[i];
			}
		}

		return null;
	}
	
	
	
	public static String getCodeFromName(Context context, String langName) {
		String[] names = context.getResources().getStringArray(R.array.language_names); 
		String[] codes = context.getResources().getStringArray(R.array.language_codes);

		for (int i = 0 ; i < names.length ; i++) {
			String name = names[i];
			if (name.equals(langName)) {
				return codes[i];
			}
		}

		return null;
	}
	
	
	protected void done() {
		saveTranslateLanguage();
		saveCurrentLanguage();

		finish();
	}


	/**
	 * Save language profile to database
	 */
	protected void saveCurrentLanguage() {
		String lang = getLanguagePreference(this);

		LanguageProfileManager manager = LanguageProfileManager.getProfileManager(); 
		if(manager.getProfile(lang) == null)
			// This language has no profile. Prompt for keyboard layout.
			selectKeyboard();
		else
			manager.setCurrentProfile(lang);
	}

	
	
	/**
	 * Save translate language
	 */
	protected void saveTranslateLanguage() {
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

	
	protected void selectKeyboard() {
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), KeyboardSelector.class);
		
		startActivity(intent);
	}
}