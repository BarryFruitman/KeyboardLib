/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.voice.dragon;

import java.util.ArrayList;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DragonVoiceView extends FrameLayout implements View.OnClickListener {

	private Handler mHandler = null;
	private final Recognizer.Listener mListener;
	private TextView mTextStatus;
	private TextView mTextLevel;
	private ProgressBar mBarLevel;
	private Button mBtnDone;
	private Button mBtnRetry;
	private Recognizer mCurrentRecognizer;
	private boolean mIsRecording = false;

	public DragonVoiceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mListener = createListener();
	}

	@Override
	public void onClick(View view) {
		if(view == mBtnDone) {
			if(mCurrentRecognizer != null && mIsRecording)
				mCurrentRecognizer.stopRecording();
			else
				KeyboardService.getIME().dismissAllPopupWindows();
		} else if(view == mBtnRetry) {
			startVoiceInput();
		}
	}


	private Recognizer.Listener createListener()
	{
		return new Recognizer.Listener()
		{            
			@Override
			public void onRecordingBegin(Recognizer recognizer) 
			{
				mTextStatus.setText("Speak Now");
				mTextStatus.setTextColor(Color.RED);
				mIsRecording = true;

				// Create a repeating task to update the audio level
				Runnable r = new Runnable()
				{
					public void run()
					{
						if (mIsRecording && mCurrentRecognizer != null)
						{
							setLevel((int) mCurrentRecognizer.getAudioLevel());
							mHandler.postDelayed(this, 500);
						}
					}
				};
				r.run();
			}

			@Override
			public void onRecordingDone(Recognizer recognizer) 
			{
				mTextStatus.setText("Processing...");
				mTextStatus.setTextColor(Color.BLUE);
				mIsRecording = false;
			}

			@Override
			public void onError(Recognizer recognizer, SpeechError error) 
			{
				if (recognizer != mCurrentRecognizer)
					return;
				mCurrentRecognizer = null;

				mTextStatus.setText(error.getErrorDetail());
				mTextStatus.setTextColor(Color.YELLOW);
				mBtnRetry.setVisibility(View.VISIBLE);

				// Log this error
				String detail = error.getErrorDetail();
				String suggestion = error.getSuggestion();
				if (suggestion == null)
					suggestion = "";
				Log.d(KeyboardApp.LOG_TAG, "Recognizer.Listener.onError: detail='" + detail + "',suggestion='" + suggestion);
			}

			@Override
			public void onResults(Recognizer recognizer, Recognition results) {
				mCurrentRecognizer = null;
				ArrayList<String> rs = new ArrayList<String>();
				int count = results.getResultCount();
				for (int i = 0; i < count; i++)
					rs.add(results.getResult(i).getText());

				// Post result message
				Message result = new Message();
				result.obj = rs;
				KeyboardService.getIME().getVoiceHandler().sendMessageDelayed(result, 500);

				KeyboardService.getIME().dismissAllPopupWindows();
			}
		};
	}



	public void startVoiceInput() {
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(layoutParams); // HACK! For some reason the XML layout params are ignored :(

		mTextStatus = (TextView) findViewById(R.id.voice_label_status);
		mTextStatus.setText("Initializing...");   
		mTextStatus.setTextColor(Color.YELLOW);

		mTextLevel = (TextView) findViewById(R.id.voice_label_level);
		mBarLevel = (ProgressBar) findViewById(R.id.voice_bar_level);
		mBtnDone = (Button) findViewById(R.id.voice_btn_done);
		mBtnDone.setOnClickListener(this);
		mBtnRetry = (Button) findViewById(R.id.voice_btn_retry);
		mBtnRetry.setOnClickListener(this);
		mBtnRetry.setVisibility(View.GONE);

		if(mHandler == null)
			mHandler = new Handler();
		if(mCurrentRecognizer == null)
			mCurrentRecognizer = VoiceSDK.getSpeechKit(KeyboardService.getIME()).createRecognizer(Recognizer.RecognizerType.Dictation, Recognizer.EndOfSpeechDetection.Long, "en_US", mListener, mHandler);

		mCurrentRecognizer.start();
	}



	protected void setLevel(int level) {
		mTextLevel.setText(level + "%");
		mBarLevel.setProgress(level);
	}
}
