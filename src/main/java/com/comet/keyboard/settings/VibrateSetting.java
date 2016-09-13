/*
 * Comet Keyboard Library Copyright (C) 2011-2012 Comet Inc. All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Vibrate Setting Activity
 * 
 * It allows to setup vibrate time on key press
 * 
 * @author Kuban Dzhakipov <kuban.dzhakipov@sibers.com>
 * @version $Id: VibrateSetting.java 3056 2012-11-21 23:31:31Z cometinc $
 */
public class VibrateSetting extends Activity implements OnSeekBarChangeListener,
    View.OnClickListener, DialogInterface.OnClickListener {
  // Defines preference key name
  public static final String PREFERENCE_VIBRATE = "vibrate";

  // Load current vibrate setting time, otherwise it returns default time in ms
  public static int getVibrateTimePreference(Context context) {
    return Integer.parseInt(context.getSharedPreferences(Settings.SETTINGS_FILE,
        Context.MODE_PRIVATE).getString(PREFERENCE_VIBRATE,
        context.getString(R.string.settings_vibrate_default)));
  }

  // services
  Vibrator mVibrator;

  // views
  private AlertDialog mKeyboardSettingDialog;
  private SeekBar mSBVibrateTime;
  private View mSettingLayout;
  private TextView mTvVibrateSummary;

  private int mVibrateTime = 0, mVibrateTimePrev = 0; // in ms

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_BACK:
        // User is exiting.
        finish();
        return true;
    }

    return false;
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    mVibrateTime = progress;

    String summary = getString(R.string.vibrate_time_summary, progress);

    if (progress == 0)
      // no vibrate message
      summary = getString(R.string.vibrate_notime);

    mTvVibrateSummary.setText(summary);
  }

  @Override
  public void onStartTrackingTouch(SeekBar arg0) {
    // Do nothing
  }

  @Override
  public void onStopTrackingTouch(SeekBar arg0) {
    // Do nothing
  }

  // Save vibrate time preference
  public void putVibrateTimePreference() {

    SharedPreferences.Editor preferenceEditor =
        getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

    preferenceEditor.putString(PREFERENCE_VIBRATE, String.valueOf(mVibrateTime));
    preferenceEditor.commit();

    KeyboardService.writeDebug("VibrateSetting.putVibrateTimePreference(): time=" + mVibrateTime);
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // get configs
    mVibrateTime = mVibrateTimePrev = getVibrateTimePreference(this);

    KeyboardService.writeDebug("VibrateSetting.onCreate(): time=" + mVibrateTime);

    // get services
    mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    // Load layouts
    // Inflate the overlapped keyboard view
    mSettingLayout = (View) getLayoutInflater().inflate(R.layout.settings_vibrate, null);

    // setup views
    mTvVibrateSummary = (TextView) mSettingLayout.findViewById(R.id.tvVibrateTimeSummary);
    mSBVibrateTime = (SeekBar) mSettingLayout.findViewById(R.id.sbVibrateTime);
    mSBVibrateTime.setOnSeekBarChangeListener(this);
    mSBVibrateTime.setProgress(mVibrateTime);

    showVibrateDialog();
  }

  /**
   * Perform button clicks in dialog view
   */
  public void onClick(View v) {
    if (v.getId() == R.id.btTimeIncrease) {
      // increase time
      if(mVibrateTime < 100)
        mSBVibrateTime.setProgress(mVibrateTime + 1);
    } else if (v.getId() == R.id.btTimeDecrease) {
      // decrease time
      if(mVibrateTime > 0)
        mSBVibrateTime.setProgress(mVibrateTime - 1);
    }
  }

  /**
   * Perform dialog button clicks
   * 
   * @param dialog
   * @param which
   */
  public void onClick(DialogInterface dialog, int which) {
	  KeyboardService.writeDebug("VibrateSetting.onClick(): which=" + which);
	    
    switch (which) {
    /**
     * Ok
     */
      case DialogInterface.BUTTON_POSITIVE:
        // Store vibrate time value to shared preferences
        putVibrateTimePreference();

        // vibrate
        mVibrator.vibrate(mVibrateTime);

        mKeyboardSettingDialog.dismiss();
        finish();
        break;

      /**
       * Cancel
       */
      case DialogInterface.BUTTON_NEGATIVE:
        // Store vibrate time value to shared preferences
        mVibrateTime = mVibrateTimePrev;
        putVibrateTimePreference();

        mKeyboardSettingDialog.dismiss();
        finish();
        break;
    }
  }

  /**
   * Show vibrate dialog
   */
  private void showVibrateDialog() {
    // Create new setting dialog
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle(R.string.settings_title_vibrate_on_key_press);
    builder.setView(mSettingLayout);

    mSettingLayout.findViewById(R.id.btTimeDecrease).setOnClickListener(this);
    mSettingLayout.findViewById(R.id.btTimeIncrease).setOnClickListener(this);

    mKeyboardSettingDialog = builder.create();
        

    // set new vibrate time
    mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_POSITIVE,
        getResources().getString(R.string.ok), this);

    // cancel
    mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
        getResources().getString(R.string.cancel), this);

    // cancel
    mKeyboardSettingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
      public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
          // Store vibrate time value to shared preferences
          mVibrateTime = mVibrateTimePrev;
          putVibrateTimePreference();

          finish();
          return true;
        }
        return false;
      }
    });

    // test
    mKeyboardSettingDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
        getResources().getString(R.string.test), this);

    // it helps to avoid from closing dialog after click on Test Vibration button
    mKeyboardSettingDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {
        mKeyboardSettingDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                /**
                 * Run vibrator with new vibrate time
                 */
                mVibrator.vibrate(mVibrateTime);
              }
            });
      }
    });
    
    mKeyboardSettingDialog.setCancelable(false);

    // Show keyboard setting dialog
    mKeyboardSettingDialog.show();
  }
}
