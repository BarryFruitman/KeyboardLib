/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;

import java.util.ArrayList;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.util.Utils;

/**
 * Sample code that invokes the speech recognition intent API.
 */
public class VoiceInput extends Activity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 12340;
    private ArrayList<String> mResult;
    private static Handler mHandler;
    private static String mLang;

    // Global object to share with handler
    static Activity voiceRecognition;

//    public static boolean isCalledFromRightRoot = false;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Assert.assertTrue(Utils.isExistVoiceRecognizeActivity(this));

        voiceRecognition = this;

//        if (isCalledFromRightRoot) {
//        	isCalledFromRightRoot = false;
//        } else {
//        	finish();
//        	return;
//        }

        setContentView(R.layout.voice_input);

        startVoiceRecognitionActivity();
    }

    /**
     * Set recognition handler
     */
    public static void setRecognitionHandler(Handler handlerIn) {
        mHandler = handlerIn;
    }

    /**
     * Set input language
     */
    public static void setLanguage(String lang) {
        mLang = Translator.getLanguageCode(lang).toString();
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        if (mLang != null)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLang);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_input_title));
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            mResult = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
        }

        // Log.e("*********** Service", "onActivityResult");
        if (mHandler != null) {
            // Post result message
            Message result = new Message();
            result.obj = mResult;
            mHandler.sendMessageDelayed(result, 500);

            // Just send message before start new input
            // mHandler.sendMessageAtFrontOfQueue(result);
        }
        finish();
    }
}