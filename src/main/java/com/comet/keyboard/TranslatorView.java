/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import java.util.Vector;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.KeyboardApp.AppStore;
import com.comet.keyboard.dictionary.DictionaryUtils;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.ErrorReport;
import com.google.api.translate.Language;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TranslatorView extends RelativeLayout implements View.OnClickListener {

	public static String PREF_LANG_FROM = "language_from";
	public static String PREF_LANG_TO = "language_to";
	private String mLanguageFrom = "";
	private String mLanguageTo = "";
	private String mPhrase = "";
	protected Vector<String> vLanguages;
	protected Button btnMenuSelected;
	private final int TRANSLATE_TEXT = 1;
	private final int TRANSLATE_CLIPBOARD = 2;
	private final int TRANSLATE_VOICE_INPUT = 3;

	public TranslatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TranslatorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message result) {
			// Hide progress dialog
			View progressView = findViewById(R.id.layout_progress);
			progressView.setVisibility(View.GONE);

			Translator translator = (Translator) result.obj;
			if(translator.error == Translator.RESULT_ERROR) {
				// Display an error message
				Toast toast = Toast.makeText(getContext(), translator.translation, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.BOTTOM, 0, 100);
				toast.show();
				ErrorReport.reportShortError(getContext(), "translator", "source=" + mLanguageFrom + ",target=" + mLanguageTo + ",phrase=" + mPhrase);
			} else if(translator.caller == TRANSLATE_TEXT) {
				// Show the translation in a text edit
				if(!translator.translation.equals("")) {
					KeyboardService.getIME().replaceText(translator.translation);

					if(creditsShowing()) {
						// Decrease purchased item point
						int point = getCredits();
						setCredits(point - 1);
						onUpdateUI();
					}
				}
			} else if(translator.caller == TRANSLATE_VOICE_INPUT) {
				// Show the translation in a text edit
				if(!translator.translation.equals("")) {
					KeyboardService.getIME().inputVoiceResult(translator.translation);
				}
			} else if(translator.caller == TRANSLATE_CLIPBOARD) {
				// Show the translation in a popup
				TextView textClipboard = (TextView) findViewById(R.id.text_clipboard);
				textClipboard.setText(translator.translation);
				textClipboard.setVisibility(View.VISIBLE);
				ImageView imgCloseButton = (ImageView) findViewById(R.id.img_close_button);
				imgCloseButton.setVisibility(View.VISIBLE);
			}

			KeyboardService.getIME().clearCandidates();
		}
	};



	protected void onCreate() {
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(layoutParams); // HACK! For some reason the XML layout params are ignored :(

		// Assign a listener to language buttons
		findViewById(R.id.menu_source_language).setOnClickListener(this);
		findViewById(R.id.menu_arrow_source_language).setOnClickListener(this);
		findViewById(R.id.menu_target_language).setOnClickListener(this);
		findViewById(R.id.menu_arrow_target_language).setOnClickListener(this);

		// Assign a listener to translate button
		findViewById(R.id.btn_translate).setOnClickListener(this);

		// Assign a listener to translate clipboard button
		findViewById(R.id.btn_translate_clipboard).setOnClickListener(this);

		// Assign a listener to translate selected button
		// findViewById(R.id.btn_translate_selected).setOnClickListener(this);

		// Assign a listener to translate voice button
//		findViewById(R.id.img_voice_input).setOnClickListener(this);

		ImageView imgCloseButton = (ImageView) findViewById(R.id.img_close_button);
		imgCloseButton.setOnClickListener(this);

		// Assign a scrolling movement method to the clipboard view
		((TextView) findViewById(R.id.text_clipboard)).setMovementMethod(new ScrollingMovementMethod());

		// Populate the languages menu 
		vLanguages = getLanguageList();
		((ListView) findViewById(R.id.menu_languages)).setAdapter(new ArrayAdapter<String>(getContext(), R.layout.language_item, vLanguages));

		// Get preferences
		onUpdateUI();
	}



	@Override
	public void onClick(View view) {
		// Hide languages menu (just in case)
		findViewById(R.id.menu_languages).setVisibility(View.GONE);
		if(view == view.findViewById(R.id.menu_source_language)
				|| view == view.findViewById(R.id.menu_arrow_source_language)
				|| view == view.findViewById(R.id.menu_target_language)
				|| view == view.findViewById(R.id.menu_arrow_target_language))
			onClickLanguageMenu(view);
		else if(view == view.findViewById(R.id.btn_translate))
			onClickTranslate();
		else if(view == view.findViewById(R.id.btn_translate_clipboard))
			onClickTranslateClipboard();
		/* else if(view == view.findViewById(R.id.btn_translate_selected))
			onClickTranslateSelected(); */
//		else if(view == view.findViewById(R.id.img_voice_input))
//			onClickTranslateVoiceInput();
		else if(view == view.findViewById(R.id.img_close_button))
			onClickCloseClipboard();
	}



	private void onClickLanguageMenu(View view) {
		ListView menuLanguages =(ListView) findViewById(R.id.menu_languages);

		int viewId = view.getId();
		int languageMenuId = R.id.menu_source_language;
		if(viewId == R.id.menu_target_language || viewId == R.id.menu_arrow_target_language)
			languageMenuId = R.id.menu_target_language;
		Button btnLanguage = (Button) findViewById(languageMenuId);

		// Align list with button and make it visible
		RelativeLayout.LayoutParams rllp = (RelativeLayout.LayoutParams) menuLanguages.getLayoutParams();
		rllp.addRule(RelativeLayout.ALIGN_LEFT, languageMenuId);
		rllp.addRule(RelativeLayout.ALIGN_RIGHT, languageMenuId);
		menuLanguages.setLayoutParams(rllp);

		// Scroll to currently selected language
		menuLanguages.setSelection(vLanguages.indexOf(btnLanguage.getText()));
		menuLanguages.setVisibility(View.VISIBLE);

		btnMenuSelected = (Button) btnLanguage;
	}



	public void onClickLanguageMenuItem(TextView item)	{
		// Get language
		String language = item.getText().toString();

		// Set the label of the menu button
		btnMenuSelected.setText(language);

		// Save the language in preferences
		if(btnMenuSelected == findViewById(R.id.menu_source_language) || btnMenuSelected == findViewById(R.id.menu_arrow_source_language)) {
			savePref(PREF_LANG_FROM, language);
		} else if(btnMenuSelected == findViewById(R.id.menu_target_language) | btnMenuSelected == findViewById(R.id.menu_arrow_target_language)) {
			savePref(PREF_LANG_TO, language);
		}

		// Hide languages menu
		findViewById(R.id.menu_languages).setVisibility(View.GONE);
	}



	public String getPref(String key, String defValue) {
		SharedPreferences sharedPrefs = getContext().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		String value = sharedPrefs.getString(key, defValue);

		return value;

	}



	private void savePref(String key, String value) {
		SharedPreferences sharedPrefs = getContext().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editPrefs = sharedPrefs.edit();
		editPrefs.putString(key, value);
		editPrefs.commit();

	}



	/**
	 * Translate phrase string
	 * @param phrase
	 */
	private void translateText(String phrase, int source) {
		CharSequence error = "";
		if(phrase.equals(""))
			error = getContext().getString(R.string.translator_nothing_to_translate);

		mLanguageFrom = ((Button) findViewById(R.id.menu_source_language)).getText().toString();
		mLanguageTo = ((Button) findViewById(R.id.menu_target_language)).getText().toString();
		if(mLanguageFrom.equals(mLanguageTo))
			error = getContext().getString(R.string.translator_same_languages);
		mPhrase = phrase;

		if(phrase.length() > 100)
			error = getContext().getString(R.string.translator_too_long);

		if(!error.equals("")) {
			// Show an error message and abort
			Toast toast = Toast.makeText(getContext(), error, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.BOTTOM, 0, 100);
			toast.show();
			return;
		}

		translate(mLanguageFrom, mLanguageTo, mPhrase, source);
	}



	private void onClickTranslate()	{
		if (!checkTranslateAvailiability(true))
			return;

		InputConnection inputConnection = KeyboardService.getIME().getCurrentInputConnection();
		if(inputConnection == null)
			return;

		// Commit any outstanding typing
		KeyboardService.getIME().commitTyped();
		// Get text from TextEdit
		ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if(extractedText == null || extractedText.text == null)
			return;
		String phrase = extractedText.text.toString();

		translateText(phrase, TRANSLATE_TEXT);
	}

	@SuppressWarnings("unused")
	private void onClickTranslateSelected()	{
		if (!checkTranslateAvailiability(true))
			return;

		// Commit any outstanding typing
		KeyboardService.getIME().commitTyped();

		// Get selected text from TextEdit
		// String phrase = inputConnection.getExtractedText(new ExtractedTextRequest(), 0).text.toString();
		// String phrase = inputConnection.setSelection ();
		String phrase = "";

		CharSequence error = "";
		if(phrase.equals(""))
			error = getContext().getString(R.string.translator_nothing_to_translate);

		String languageFrom = ((Button) findViewById(R.id.menu_source_language)).getText().toString();
		String languageTo = ((Button) findViewById(R.id.menu_target_language)).getText().toString();
		Assert.assertTrue(languageFrom != null && languageTo != null);
		if(languageFrom.equals(languageTo))
			error = getContext().getString(R.string.translator_same_languages);

		if(!error.equals("")) {
			// Show an error message and abort
			Toast toast = Toast.makeText(getContext(), error, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.BOTTOM, 0, 100);
			toast.show();
			return;
		}

		translate(languageFrom, languageTo, phrase, TRANSLATE_TEXT);
	}



	private void onClickTranslateClipboard() {
		if (!checkTranslateAvailiability(true))
			return;

		// Get text from clipboard
		ClipboardManager clipboardMgr = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		CharSequence text = clipboardMgr.getText();
		String phrase;
		if(text == null || (phrase = text.toString()).equals("")) {
			Toast.makeText(getContext(), getContext().getString(R.string.translator_empty_clipboard), Toast.LENGTH_SHORT).show();
			return;
		}

		String languageFrom = ((Button) findViewById(R.id.menu_target_language)).getText().toString();
		String languageTo = ((Button) findViewById(R.id.menu_source_language)).getText().toString();
		Assert.assertTrue(languageFrom != null && languageTo != null);
		if(languageFrom.equals(languageTo)) {
			Toast.makeText(getContext(), getContext().getString(R.string.translator_same_languages), Toast.LENGTH_SHORT).show();
			return;
		}

		// Translate if there was no error above
		translate(languageFrom, languageTo, phrase, TRANSLATE_CLIPBOARD);
	}


	private void onClickBuyMore() {
		KeyboardService.getIME().buyTranslatorCredits();
	}

	private void onClickCloseClipboard() {
		TextView textClipboard = (TextView) findViewById(R.id.text_clipboard);
		textClipboard.setVisibility(View.GONE);
		ImageView imgCloseButton = (ImageView) findViewById(R.id.img_close_button);
		imgCloseButton.setVisibility(View.GONE);
	}

	/**
	 * Update current view
	 */
	public void onUpdateUI() {
		String languageFrom = getPref(PREF_LANG_FROM, "English");
		String languageTo = getPref(PREF_LANG_TO, "Spanish");
		((Button) findViewById(R.id.menu_source_language)).setText(languageFrom);
		((Button) findViewById(R.id.menu_target_language)).setText(languageTo);
	}

	private void translate(String languageFrom, String languageTo, String phrase, int caller) {

		// Show progress dialog
		View progressView = findViewById(R.id.layout_progress);
		progressView.setVisibility(View.VISIBLE);

		// Launch a translator thread
		new Thread(new Translator(languageFrom, languageTo, phrase, caller, mHandler)).start();
	}



	/**
	 * Is there purchase item more for the translating?
	 * @return
	 */
	private boolean checkTranslateAvailiability(boolean showMessage) {
//		if(KeyboardApp.getKeyboardApp().mAppStore != AppStore.Google)
//			return true;

		int point = getCredits();

		if (point == 0) {
			KeyboardService.getIME().buyTranslatorCredits();
			return false;
		}

		return true;
	}



	// Return a list of all languages supported by Google Translate
	private Vector<String> getLanguageList() {
		Vector<String> languages = new Vector<String>();
		for (Language language : Language.values()) {
			// Skip "DETECT_LANGUAGE"
			if(language.toString().equals(""))
				continue;
			// Convert name to mixed case
			String languageFormatted = DictionaryUtils.capFirstChar(language.name());
			languages.add(languageFormatted);
		}

		return languages;
	}



	private boolean creditsShowing() {
		if(KeyboardApp.getApp().mAppStore != AppStore.Google || KeyboardApp.getApp().getProductType().equals("upgrade"))
			return false;

		return true;
	}



	/*
	 * TODO Refactor this
	 */
	public static void resetView() {
		KeyboardService IME = KeyboardService.getIME();
		if(IME != null) {
			KeyboardView kbv = IME.getKeyboardView();
			if(kbv != null) {
				TranslatorView tv = kbv.mTranslatorView;
				if(tv != null)
					tv.onCreate();
			}
		}
	}



	public static int getCredits() {
		upgradeCredits();

		return KeyboardApp.getApp()
				.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE)
				.getInt("trans_point", -1);
	}



	public static void setCredits(int credits) {
		Assert.assertTrue(credits >= 0);

		SharedPreferences sharedPrefs = KeyboardApp.getApp().getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
		sharedPrefsEditor.putInt("trans_point", credits);
		sharedPrefsEditor.commit();
	}



	private static void upgradeCredits() {
		// Upgrade from TK 1.0
		// Move credits from old prefs to new prefs
		int credits = KeyboardApp.getApp()
				.getSharedPreferences("translatingkeyboard.prefs", Context.MODE_PRIVATE)
				.getInt("trans_point", -1);

		if(credits != -1) {
			// Delete old credits
			KeyboardApp.getApp()
					.getSharedPreferences("translatingkeyboard.prefs", Context.MODE_PRIVATE)
					.edit().remove("trans_point").commit();

			setCredits(credits);
		}
	}
}
