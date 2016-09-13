/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import com.comet.keyboard.KeyboardView.OnKeyboardActionListener;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * @author Barry Fruitman
 * 
 */
public class SymMenuLayout extends LinearLayout {

	private PopupKeyboardView mKbViewStandard;

	private PopupKeyboardView mKbViewSmileys;

	private PopupKeyboardView mKbViewDingbats;

	private PopupKeyboardView mKbViewCurrency;

	private PopupKeyboardView mKbViewBusiness;

	private PopupKeyboardView mKbViewArrows;

	private PopupKeyboardView mKbViewFractions;

	private PopupKeyboardView mKbViewMath;

	private PopupKeyboardView mKbViewLatinExtended;

	public SymMenuLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SymMenuLayout(Context context) {
		super(context);
	}

	protected void createKeyboards(KeyboardView parentKeyboard,
			OnKeyboardActionListener onKeyboardActionListener) {
		mKbViewStandard = (PopupKeyboardView) findViewById(R.id.sym_menu_extended);
		PopupKeyboard kbStandard = new PopupKeyboard(getContext(),
				R.xml.sym_extended_keys);
		mKbViewStandard.setKeyboard(kbStandard);
		mKbViewStandard.setPopupParent(parentKeyboard);
		mKbViewStandard.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewSmileys = (PopupKeyboardView) findViewById(R.id.sym_menu_smileys);
		PopupKeyboard kbSmileys = new PopupKeyboard(getContext(),
				R.xml.smiley_keys);
		mKbViewSmileys.setKeyboard(kbSmileys);
		mKbViewSmileys.setPopupParent(parentKeyboard);
		mKbViewSmileys.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewDingbats = (PopupKeyboardView) findViewById(R.id.sym_menu_dingbats);
		PopupKeyboard kbDingbats = new PopupKeyboard(getContext(),
				R.xml.popup_keyboard, getContext().getString(
						R.string.symbols_dingbats), -1, getPaddingLeft()
						+ getPaddingRight());
		mKbViewDingbats.setKeyboard(kbDingbats);
		mKbViewDingbats.setPopupParent(parentKeyboard);
		mKbViewDingbats.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewCurrency = (PopupKeyboardView) findViewById(R.id.sym_menu_currency);
		PopupKeyboard kbCurrency = new PopupKeyboard(getContext(),
				R.xml.popup_keyboard, getContext().getString(
						R.string.symbols_currency), -1, getPaddingLeft()
						+ getPaddingRight());
		mKbViewCurrency.setKeyboard(kbCurrency);
		mKbViewCurrency.setPopupParent(parentKeyboard);
		mKbViewCurrency.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewBusiness = (PopupKeyboardView) findViewById(R.id.sym_menu_business);
		PopupKeyboard kbBusiness = new PopupKeyboard(getContext(),
				R.xml.popup_keyboard, getContext().getString(
						R.string.symbols_business), -1, getPaddingLeft()
						+ getPaddingRight());
		mKbViewBusiness.setKeyboard(kbBusiness);
		mKbViewBusiness.setPopupParent(parentKeyboard);
		mKbViewBusiness.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewArrows = (PopupKeyboardView) findViewById(R.id.sym_menu_arrows);
		PopupKeyboard kbArrows = new PopupKeyboard(getContext(),
				R.xml.popup_keyboard, getContext().getString(
						R.string.symbols_arrows), -1, getPaddingLeft()
						+ getPaddingRight());
		mKbViewArrows.setKeyboard(kbArrows);
		mKbViewArrows.setPopupParent(parentKeyboard);
		mKbViewArrows.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewFractions = (PopupKeyboardView) findViewById(R.id.sym_menu_fractions);
		PopupKeyboard kbFractions = new PopupKeyboard(getContext(),
				R.xml.popup_keyboard, getContext().getString(
						R.string.symbols_fractions), -1, getPaddingLeft()
						+ getPaddingRight());
		mKbViewFractions.setKeyboard(kbFractions);
		mKbViewFractions.setPopupParent(parentKeyboard);
		mKbViewFractions.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewMath = (PopupKeyboardView) findViewById(R.id.sym_menu_math);
		PopupKeyboard kbMath = new PopupKeyboard(getContext(), R.xml.math_keys);
		mKbViewMath.setKeyboard(kbMath);
		mKbViewMath.setPopupParent(parentKeyboard);
		mKbViewMath.setOnKeyboardActionListener(onKeyboardActionListener);

		mKbViewLatinExtended = (PopupKeyboardView) findViewById(R.id.sym_menu_latin_extended);
		PopupKeyboard kbLatinExtended = new PopupKeyboard(getContext(),
				R.xml.sym_latin_extended_keys);
		mKbViewLatinExtended.setKeyboard(kbLatinExtended);
		mKbViewLatinExtended.setPopupParent(parentKeyboard);
		mKbViewLatinExtended
				.setOnKeyboardActionListener(onKeyboardActionListener);
	}
}
