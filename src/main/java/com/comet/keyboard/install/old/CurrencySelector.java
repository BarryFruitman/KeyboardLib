/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.install.old;

import java.util.Locale;

import junit.framework.Assert;

import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class CurrencySelector extends Activity implements OnClickListener {

	// Selecting Dialog
	private AlertDialog mDlgSelecting;
	private RadioGroup mRGCurrency;
	
	// available keyboard names and values
	private String[] mCurrNames;
	private String[] mCurrSymbols;

	// selected currency name
	private String mCurrCurrency;
	
	/**
	 * Defines the country name for the currency
	 */
	private final static String[] dollarCountry = new String[] {
		"US", "CA", "AU", "AR", "BB", "BS", "AS", "IO", "EC", "GU",
		"HT", "PR", "MH", "FM", "MP", "MX", "PW", "PA", "TL", "TC",
		"UM", "VG", "VI"
	};
	private final static String[] euroCountry = new String[] {
		"AD", "AT", "BE", "FI", "FR", "GF", "TF", "DE", "GR", "GP",
		"IE", "IT", "LU", "MQ", "YT", "MC", "NL", "PT", "RE", "PM",
		"SM", "RS", "ES"
	};
	private final static String[] poundCountry = new String[] {
		"UK", "GB", "LB", "GI"
	};
	private final static String[] yuanCountry = new String[] {
		"CN", "JP"
	};
	
	/**
	 * Keep the currency and related country
	 */
	public class Currency {
		public String[] mCountry;
		public int mCurrencyID;
		
		public Currency(String[] country, int currency) {
			mCountry = country;
			mCurrencyID = currency;
		}
	}
	
	// All currency info
	private final Currency[] mCurrencyList = new Currency[] {
		new Currency(dollarCountry, R.string.curr_dollar),
		new Currency(euroCountry, R.string.curr_euro), 
		new Currency(poundCountry, R.string.curr_pound), 
		new Currency(yuanCountry, R.string.curr_yuan), 
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load language
		mCurrNames = getResources().getStringArray(R.array.currency_names);
		mCurrSymbols = getResources().getStringArray(R.array.currency_symbols);
		
		Assert.assertTrue(mCurrNames != null);
		Assert.assertTrue(mCurrSymbols != null);
		
		loadCurrencyPreference();
		
		showSelectDialog();
	}

	/**
	 * Save last preference
	 */
	protected void onDestroy () {
		super.onDestroy();
		
		// Save current currency to preference
		putCurrencyPreference();
	}
	
	/**
	 * Get default currency name
	 */
	private String getDefaultCurrencyName() {
		Resources resource;
		Currency currencyObj;
		
		String currCountry;
		Locale currLocale;
		String currency = null;
		
		resource = getResources();
		currLocale = java.util.Locale.getDefault();
		currCountry = currLocale.getCountry();
		currency = resource.getString(R.string.curr_dollar);
		for (int nCurrency = 0 ; nCurrency < mCurrencyList.length ; nCurrency++) {
			currencyObj = mCurrencyList[nCurrency];
			for (int i = 0 ; i < currencyObj.mCountry.length ; i++) {
				if (currCountry.equalsIgnoreCase(currencyObj.mCountry[i])) {
					currency = resource.getString(currencyObj.mCurrencyID);
					nCurrency = mCurrencyList.length;
					break;
				}
			}
		}
		
		return currency;
	}
	
	/**
	 * Show selecting dialog
	 */
	private void showSelectDialog() {
//		String autoDectectedCurrencyName;
//		boolean isChecked = false;
		
//		autoDectectedCurrencyName = getDefaultCurrencyName();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View languageLayout = inflater.inflate(R.layout.currency_options,
				(ViewGroup) findViewById(R.id.llCurrency));

		builder.setTitle(R.string.install_select_currency_title);
		builder.setView(languageLayout);

		mDlgSelecting = builder.create();
		mDlgSelecting.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					finish();
					return true;
				}
				return false;
			}
		});
		
		// Get component from dialog resource
		mRGCurrency = (RadioGroup) languageLayout.findViewById(R.id.rgCurrency);
		// Load language options
		for (int i = 0 ; i < mCurrNames.length ; i++) {
			RadioButton newOption = new RadioButton(this);
			newOption.setId(i);
			newOption.setText(mCurrNames[i]);
			/* newOption.setPadding(
					getResources().getDimensionPixelSize(R.dimen.lang_option_padding_left), 
					0, 0, 0); */
			newOption.setOnClickListener(this);
			mRGCurrency.addView(newOption);

//			if (autoDectectedCurrencyName.equals(mCurrNames[i])) {
//				mRGCurrency.check(i);
//				isChecked = true;
//			}
		}
//		if (isChecked == false) {
//			mRGCurrency.check(0);
//		}
		
		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mDlgSelecting.show();
	}

	/**************************************************************************
	 * UI Event Handler
	 *************************************************************************/
	@Override
	public void onClick(View arg0) {
		int selectedID;
		
		Assert.assertTrue(mDlgSelecting != null);
		Assert.assertTrue(mDlgSelecting.isShowing());
		
		selectedID = mRGCurrency.getCheckedRadioButtonId();
		// mCurrKeyboard = ((RadioButton)mRGCurrency.findViewById(selectedID)).getText().toString();
		mCurrCurrency = mCurrSymbols[selectedID];
		
		// Save current currency to preference
		putCurrencyPreference();
		
		// Close selecting dialog
		mDlgSelecting.dismiss();
		mDlgSelecting = null;
		
		finish();
	}
	
	// Load currency preference
	public void loadCurrencyPreference() {
		// Get preferences
        SharedPreferences preference = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);       
        
		mCurrCurrency = preference.getString("currency", mCurrSymbols[0]);
	}

	// Save currency preference
	public void putCurrencyPreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
		
		preferenceEditor.putString("currency", mCurrCurrency);
		preferenceEditor.commit();
	}
}
