/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;


public class Translator implements Runnable {

	String languageFrom = "";
	String languageTo = "";
	String phrase = "";
	String translation = "";
	Handler handler = null;
	int caller = 0;
	String HTTP_REFERRER = "http://cometinc.ca";
	static int RESULT_OK = 0, RESULT_ERROR = 1;
	int error = RESULT_OK;


	public Translator(String languageFromIn, String languageToIn, String phraseIn, int callerIn, Handler handlerIn) {
		languageFrom = languageFromIn;
		languageTo = languageToIn;
		phrase = phraseIn;
		caller = callerIn;
		handler = handlerIn;
	}

	@Override
	public void run() {
		// Translate text via Google
		translation = phrase;
		Translate.setHttpReferrer(HTTP_REFERRER);
		Translate.setKey("AIzaSyB0lK06QEREfJ2AsDujunN6qX8WhNgysp4");
		Language languageCodeFrom = getLanguageCode(languageFrom.toUpperCase());
		Language languageCodeTo = getLanguageCode(languageTo.toUpperCase());
		try {
			translation = Translate.execute(phrase, languageCodeFrom, languageCodeTo);
		} catch (Exception e) {
			// Show an error message and abort
			Log.e(KeyboardApp.LOG_TAG, "Error while translating from " + languageCodeFrom + " to " + languageCodeTo, e);

			translation = e.getMessage();
			error = RESULT_ERROR;
		}

		// If Google returns the same phrase, that means it could not translate the phrase.
		if(phrase.equals(translation)) {
			translation = KeyboardService.IME.getString(R.string.google_translate_failed);
			error = RESULT_ERROR;
		}

		translation += " ";

		// Store this translator in a message
		Message result = new Message();
		result.obj = this;
		// Send result to handler
		handler.sendMessage(result);
	}



	public static Language getLanguageCode(String language) {
		for (Language l : Language.values()) {
			if (language.equalsIgnoreCase(l.name())) {
				return l;
			}
		}

		// Just in case
		return Language.ENGLISH;
	}
}
