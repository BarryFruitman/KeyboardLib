/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.settings;

import com.comet.keyboard.R;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.DatabaseHelper.DBError;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class EditShortcut extends Activity implements OnClickListener, 
	OnTouchListener, OnFocusChangeListener, OnEditorActionListener {
	// Defines intent parameter
	public final static String EXTRA_KEYSTROKE = "keystroke";
	public final static String EXTRA_EXPAND = "expand";
	public final static String EXTRA_ISNEW = "isNew";
	
	// Current shortcut
	private ShortcutData mOrgShortcut;
	private ShortcutData mCurrShortcut = null;
	private boolean mIsNew = true;
	
	// UI Objects
	private EditText mETKeystroke;
	private EditText mETExpand;
	private Button mBtnSave;
	private Button mBtnDelete;
	
	private AlertDialog mDlgAlert;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.title_settings_edit_shortcut);
        
        setContentView(R.layout.settings_edit_shortcut);
        
        // Retrieve paramaters
        Bundle bundle = getIntent().getExtras();
        String keystroke = "", expand = "", isNew;
        
        if (bundle != null) {
        	keystroke = bundle.getString(EXTRA_KEYSTROKE);
        	expand = bundle.getString(EXTRA_EXPAND);
        	
        	isNew = bundle.getString(EXTRA_ISNEW);
        	if (isNew != null) {
        		mIsNew = Boolean.valueOf(isNew).booleanValue();
        	}
        }
        mOrgShortcut = new ShortcutData(keystroke, expand);
        
        // Retrieve ui objects
        mETKeystroke = (EditText)findViewById(R.id.etKeystroke);
        mETExpand = (EditText)findViewById(R.id.etExpand);
        mBtnSave = (Button)findViewById(R.id.btnSave);
        mBtnDelete = (Button)findViewById(R.id.btnDelete);
        
        // Set default value
        mETKeystroke.setText(keystroke);
        mETExpand.setText(expand);
        
        // Set event listener
        mETKeystroke.setOnTouchListener(this);
        mETKeystroke.setOnFocusChangeListener(this);
        mETKeystroke.setOnEditorActionListener(this);
        mETKeystroke.addTextChangedListener(mETKeystrokeTextChanged);
        
        mETExpand.setOnFocusChangeListener(this);
        mETExpand.setOnEditorActionListener(this);
        mETExpand.setOnTouchListener(this);
        
        mBtnSave.setOnClickListener(this);
        mBtnDelete.setOnClickListener(this);
        
        // Show input dialog forcely
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		InputMethodManager mInputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mInputMgr.showSoftInput(mETKeystroke, InputMethodManager.SHOW_FORCED);
		
        onUpdateUI();
    }

    /**
     * Update ui component status
     */
    private void onUpdateUI() {
    	String keyStroke, expand;
		
		keyStroke = mETKeystroke.getText().toString();
		expand = mETExpand.getText().toString();
		
		if (keyStroke == null || keyStroke.equals("")
			|| expand == null || expand.equals("")) {
			mBtnSave.setEnabled(false);
			mBtnDelete.setEnabled(false);
		} else {
			mBtnSave.setEnabled(true);
			if (!mIsNew)
				mBtnDelete.setEnabled(true);
		}
    }
    
	/**
	 * Save & Update & Delete shortcut item
	 */
	public void onClick(View view) {
		String keyStroke, expand;
		DBError error = DBError.DB_ERROR_NONE;
		
		
		keyStroke = mETKeystroke.getText().toString();
		expand = mETExpand.getText().toString();
		mCurrShortcut = new ShortcutData(keyStroke, expand);
		
		if (view == mBtnSave) {
			if (mIsNew) {
				error = DatabaseHelper.safeGetDatabaseHelper(view.getContext()).addShortcutItem(mCurrShortcut);
			} else {
				error = DatabaseHelper.safeGetDatabaseHelper(view.getContext()).updateShortcutItem(mOrgShortcut, 
						mCurrShortcut);
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.normal_error_title);
			switch (error) {
			case DB_ERROR_NONE:
				finish();
				return;
			case DB_ERROR_FAILED:
				builder.setMessage(R.string.database_error_message);
				break;
			case DB_ERROR_ALREADY_EXIST:
				builder.setMessage(R.string.setting_shortcut_conflict_message);
				break;
			case DB_ERROR_NOT_EXIST:
				builder.setMessage(R.string.setting_shortcut_notexist_message);
				break;
			}
			
			mDlgAlert = builder.create();
			mDlgAlert.setButton(DialogInterface.BUTTON_POSITIVE, 
					getResources().getString(R.string.install_select_language_notice_ok), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mDlgAlert.dismiss();
					}
				}
			);
			mDlgAlert.show();
		} else if (view == mBtnDelete) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.setting_delete_shortcut_title);
			builder.setMessage(R.string.setting_delete_shortcut_description);
			
			mDlgAlert = builder.create();
			mDlgAlert.setButton(DialogInterface.BUTTON_POSITIVE, 
					getResources().getString(R.string.ok), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						DatabaseHelper.safeGetDatabaseHelper(EditShortcut.this).removeShortcutItems(mCurrShortcut);
						mDlgAlert.dismiss();
						finish();
					}
				}
			);
			mDlgAlert.setButton(DialogInterface.BUTTON_NEGATIVE, 
					getResources().getString(R.string.cancel), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mDlgAlert.dismiss();
					}
				}
			);
			mDlgAlert.show();
		}
	}

	/**
	 * Update button status
	 */

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		onUpdateUI();
		
		return false;
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		onUpdateUI();
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			mETExpand.requestFocus();
			return true;
		}
		
		return false;
	}

	public TextWatcher mETKeystrokeTextChanged = new TextWatcher() {
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// Do nothing
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			String orgStr = s.toString();
			String str = orgStr.replaceAll(" ", "");
			if (!str.equals(orgStr))
				mETKeystroke.setText(str);
			
			onUpdateUI();
		}

		@Override
		public void afterTextChanged(Editable s) {
			// Do nothing
		}
	};
}
