/*
 * TypeSmart Keyboard
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.theme;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;


/**
 * This class represents a keyboard theme. It contains all the attributes for a theme
 * including colors, drawables and other metrics.
 * 
 * TODO: Remove either mThemeName or mName as they are redundant.
 * TODO: Base theme identification on theme id, not theme name.
 * 
 * 
 * @author Ding
 *
 */
public class KeyboardTheme {
	
	// ICS THEME HACK
	public static int icsFnSuperColor = Color.argb(255, 128, 128, 128);
	
	private Context mContext;
	
	private String mThemeName;
	private String mThemeID;
	
	/* theme's property */
	private int mKeyboardBGColor;
	private int mKeyFGColor;
	private int mKeySuperFGColor;
	private int mKeySuperHorizOffset;
	
	private String mName;
	private String mKeyboardBG;
	private String mKeyBG;
	private int mKeyBGAlpha;
	private int mKeyPressedAlpha;
	private String mKeyFNBG;
	private String mKeyAnyBG;
	private String mKeyPressedBG;
	private String mKeyFNPressedBG;

	private String mKeySymArrowDown;
	private String mKeySymArrowLeft;
	private String mKeySymArrowRight;
	private String mKeySymArrowUp;
	private String mKeySymArrowBack;
	private String mKeySymArrowNext;
	private String mKeySymArrows;
	private String mKeySymDelete;
	private String mKeySymMic;
	private String mKeySymReturn;
	private String mKeySymSearch;
	private String mKeySymSettings;
	private String mKeySymShiftOn;
	private String mKeySymShiftOff;
	private String mKeySymShiftLocked;
	private String mKeySymSpace;
	private String mKeySymTranslate;
	private String mKeySymLocale;
	private String mKeySymEmoji;
	
	private String mCandidateBG;
	private int mCandidateBGColor;
	private int mCandidateNormalColor;
	private int mCandidateRecommendedColor;
	private int mCandidateDividerColor;
	private int mCandidateMessageColor;

	// Defines theme's field id
	public static final int KEY_SYM_RETURN = 0;
	public static final int KEY_SYM_SHIFT_LOCKED = KEY_SYM_RETURN + 1;
	public static final int KEY_SYM_SHIFT_ON = KEY_SYM_SHIFT_LOCKED + 1;
	public static final int KEY_SYM_SHIFT_OFF = KEY_SYM_SHIFT_ON + 1;
	public static final int KEY_SYM_ARROW_UP = KEY_SYM_SHIFT_OFF + 1;
	public static final int KEY_SYM_ARROW_LEFT = KEY_SYM_ARROW_UP + 1;
	public static final int KEY_SYM_ARROW_RIGHT = KEY_SYM_ARROW_LEFT + 1;
	public static final int KEY_SYM_ARROW_DOWN = KEY_SYM_ARROW_RIGHT + 1;
	public static final int KEY_SYM_ARROW_BACK = KEY_SYM_ARROW_DOWN + 1;
	public static final int KEY_SYM_ARROW_NEXT = KEY_SYM_ARROW_BACK + 1;
	public static final int KEY_SYM_ARROWS = KEY_SYM_ARROW_NEXT + 1;
	public static final int KEY_SYM_EMOJI = KEY_SYM_ARROWS + 1;
	public static final int KEY_SYM_DELETE = KEY_SYM_EMOJI + 1;
	public static final int KEY_SYM_MIC = KEY_SYM_DELETE + 1;
	public static final int KEY_SYM_SEARCH = KEY_SYM_MIC + 1;
	public static final int KEY_SYM_SETTINGS = KEY_SYM_SEARCH + 1;
	public static final int KEY_SYM_SPACE = KEY_SYM_SETTINGS + 1;
	public static final int KEY_SYM_TRANSLATE = KEY_SYM_SPACE + 1;
	public static final int KEY_SYM_LOCALE = KEY_SYM_TRANSLATE + 1;
	public static final int KEY_ANY = KEY_SYM_LOCALE + 1;
	public static final int KEY_FN_SS = KEY_ANY + 1;
	public static final int KEY_FN_NORMAL = KEY_FN_SS + 1;
	public static final int KEY_NORMAL = KEY_FN_NORMAL + 1;
	public static final int KEY_SS = KEY_NORMAL + 1;
	public static final int KB_BG = KEY_SS + 1;
	public static final int CANDIDATE_BG = KB_BG + 1;
	public static final int KEY_SYM_SUPER_ARROWS = CANDIDATE_BG + 1;
	public static final int KEY_SYM_SUPER_MIC = KEY_SYM_SUPER_ARROWS + 1;
	public static final int KEY_SYM_SUPER_SEARCH = KEY_SYM_SUPER_MIC + 1;
	public static final int KEY_SYM_SUPER_SETTINGS = KEY_SYM_SUPER_SEARCH + 1;
	public static final int KEY_SYM_SUPER_SPACE = KEY_SYM_SUPER_SETTINGS + 1;
	public static final int KEY_SYM_SUPER_TRANSLATE = KEY_SYM_SUPER_SPACE + 1;
	public static final int KEY_SYM_SUPER_LOCALE = KEY_SYM_SUPER_TRANSLATE + 1;
	public static final int KEY_SYM_SUPER_EMOJI = KEY_SYM_SUPER_LOCALE + 1;
	
	public static final int KEY_SYM_POPUP_SHIFT_LOCKED = KEY_SYM_SUPER_EMOJI + 1;
	public static final int KEY_SYM_POPUP_SHIFT = KEY_SYM_POPUP_SHIFT_LOCKED + 1;
	public static final int KEY_SYM_POPUP_EMOJI = KEY_SYM_POPUP_SHIFT + 1;
	public static final int KEY_SYM_POPUP_ARROWS = KEY_SYM_POPUP_EMOJI + 1;
	public static final int KEY_SYM_POPUP_MIC = KEY_SYM_POPUP_ARROWS + 1;
	public static final int KEY_SYM_POPUP_TRANSLATE = KEY_SYM_POPUP_MIC + 1;
	public static final int KEY_SYM_POPUP_LOCALE = KEY_SYM_POPUP_TRANSLATE + 1;
	public static final int KEY_SYM_POPUP_SETTINGS = KEY_SYM_POPUP_LOCALE + 1;
	public static final int KEY_SYM_POPUP_SEARCH = KEY_SYM_POPUP_SETTINGS + 1;
	public static final int KEY_SYM_POPUP_RETURN = KEY_SYM_POPUP_SEARCH + 1;
	
	private Drawable mKeySymReturnDrawable;
	private Drawable mKeySymShiftLockedDrawable;
	private Drawable mKeySymShiftOnDrawable;
	private Drawable mKeySymShiftOffDrawable;
	
	private Drawable mKeySymArrowUpDrawable;
	private Drawable mKeySymArrowLeftDrawable;
	private Drawable mKeySymArrowRightDrawable;
	private Drawable mKeySymArrowDownDrawable;
	private Drawable mKeySymArrowHomeDrawable;
	private Drawable mKeySymArrowEndDrawable;
	private Drawable mKeySymArrowsDrawable;
	private Drawable mKeySymSuperArrowsDrawable;
	private Drawable mKeySymDeleteDrawable;
	private Drawable mKeySymMicDrawable;
	private Drawable mKeySymSuperMicDrawable;
	private Drawable mKeySymSearchDrawable;
	private Drawable mKeySymSuperSearchDrawable;
	private Drawable mKeySymSettingsDrawable;
	private Drawable mKeySymSuperSettingsDrawable;
	private Drawable mKeySymSpaceDrawable;
	private Drawable mKeySymTranslateDrawable;
	private Drawable mKeySymSuperTranslateDrawable;
	private Drawable mKeySymLocaleDrawable;
	private Drawable mKeySymSuperLocaleDrawable;
	private Drawable mKeySymEmojiDrawable;
	private Drawable mKeySymSuperEmojiDrawable;
	private Drawable mKeyAnyDrawable;
	
	private Drawable mKeySymPopupShiftLockedDrawable;
	private Drawable mKeySymPopupShiftDrawable;
	private Drawable mKeySymPopupEmojiDrawable;
	private Drawable mKeySymPopupArrowsDrawable;
	private Drawable mKeySymPopupMicDrawable;
	private Drawable mKeySymPopupTranslateDrawable;
	private Drawable mKeySymPopupSettingsDrawable;
	private Drawable mKeySymPopupSearchDrawable;
	private Drawable mKeySymPopupReturnDrawable;
	private Drawable mKeySymPopupLocaleDrawable;	
	
	private Drawable mKeyFnBGDrawable;
	private Drawable mKeyFnSSBGDrawable;
	
	private Drawable mKeyBGDrawable;
	private Drawable mKeySSBGDrawable;
	private Drawable mKeyboardBGDrawable;
	private Drawable mCandidateBGDrawable;
	
	private String mKeySymPopupShift;

	private String mKeySymPopupShiftLocked;

	private String mKeySymPopupEmoji;

	private String mKeySymPopupArrows;

	private String mKeySymPopupMic;

	private String mKeySymPopupTranslate;

	private String mKeySymPopupSettings;

	private String mKeySymPopupSearch;

	private String mKeySymPopupReturn;

	private String mKeySymPopupLocale;



	/**
	 * Constructor. NOTE: The theme ID is not stored in the XML file but should be.
	 * This constructor SHOULD only take a theme id param and load the theme name
	 * from the XML file.
	 * 
	 * @param context		A context.
	 * @param themeName		The name of this theme.
	 * @param themeID		The id for this theme.
	 */
	public KeyboardTheme(Context context, String themeName, String themeID) {
		mContext = context;
		
		Assert.assertTrue(themeName != null && !themeName.trim().equals(""));
		Assert.assertTrue(themeID != null && !themeID.trim().equals(""));
		
		mThemeName = themeName;
		mThemeID = themeID;
		
		loadKeyboardTheme();
	}

	
	/**
	 * Get the name of this theme.
	 * 
	 * @return	The name of this theme.
	 */
	public String getThemeName() {
		return mThemeName;
	}
	
	
	/**
	 * Get the id of this theme.
	 * @return	The id of this theme.
	 */
	public String getThemeID() {
		return mThemeID;
	}

	
	/**
	 * Checks if this theme should appear in the themes preference.
	 * The black and white themes are the only themes that cannot be selected this way.
	 * They are used for wallpaper themes.
	 * 
	 * @return	True if this theme is user-selectable.
	 */
	public boolean isPublicTheme() {
		if (getName().equals(mContext.getResources().getString(R.string.theme_name_white)) || 
			getName().equals(mContext.getResources().getString(R.string.theme_name_black))) {
			return true;
		}
		
		return false;
	}

	
	/**
	 * Loads this theme from XML. First loads all the attribute values then
	 * calls loadKeyboardThemeDrawable() to load the drawables resources.
	 */
	public void loadKeyboardTheme() {
		try {
			Node node;
			NamedNodeMap propMap = null;
			InputStream input;
			Resources res = mContext.getResources();

			String xmlPath = res.getString(R.string.themes_xml_path);

			input = mContext.getAssets().open(xmlPath);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(input));
			
			doc.getDocumentElement().normalize();
			
			NodeList nodeList = doc.getElementsByTagName(res.getString(R.string.xml_theme));
			String name;
			boolean isFound = false;
			for (int i = 0 ; i < nodeList.getLength() ; i++) {
				node = nodeList.item(i);
				propMap = node.getAttributes();
				
				name = propMap.getNamedItem(res.getString(R.string.xml_theme_property_name)).getNodeValue();
				if (name.equals(mThemeName)) {
					isFound = true;
					break;
				}
			}
			
			if (!isFound) {
				throw new Exception("Not Found '" + mThemeName + "' Theme");
			}
			
			// Retrieve properties
			String basePath = res.getString(R.string.themes_path);
			
			// Replace
			mName = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_name))
					.getNodeValue();
			
			// Key background drawables
			mKeyboardBGColor = Color.parseColor(propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_keyboard_background_color))
					.getNodeValue());
			mKeyboardBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_keyboard_background))
					.getNodeValue();
			mKeyBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_background))
					.getNodeValue();
			
			mKeyPressedBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_pressed_background))
					.getNodeValue();			
			
			mKeyBGAlpha = safeParseInt(propMap, 
					res.getString(R.string.xml_theme_property_key_background_alpha), mKeyBGAlpha);
			
			mKeyPressedAlpha = safeParseInt(propMap, 
					res.getString(R.string.xml_theme_property_key_pressed_alpha), mKeyPressedAlpha);			
			
			mKeyFNBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_fn_background))
					.getNodeValue();
			
			mKeyFNPressedBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_fn_pressed_background))
					.getNodeValue();			
			
			mKeyAnyBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_kb_any))
					.getNodeValue();
			
			// Key foreground colors
			mKeyFGColor = Color.parseColor(propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_text_color))
					.getNodeValue());
			mKeySuperFGColor = Color.parseColor(propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_super_text_color))
					.getNodeValue());

			// Super script offset
			mKeySuperHorizOffset = safeParseInt(propMap, 
					res.getString(R.string.xml_theme_property_key_super_horiz_offset), mKeySuperHorizOffset);

			// Key symbol icons
			mKeySymArrowDown = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_down))
					.getNodeValue();
			mKeySymArrowLeft = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_left))
					.getNodeValue();
			mKeySymArrowRight = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_right))
					.getNodeValue();
			mKeySymArrowUp = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_up))
					.getNodeValue();
			mKeySymArrowBack = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_back))
					.getNodeValue();
			mKeySymArrowNext = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrow_next))
					.getNodeValue();
			mKeySymDelete = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_delete))
					.getNodeValue();
			mKeySymShiftOn = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_shift_on))
					.getNodeValue();
			mKeySymShiftOff = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_shift_off))
					.getNodeValue();
			mKeySymShiftLocked = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_shift_locked))
					.getNodeValue();
			mKeySymSpace = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_space))
					.getNodeValue();
			
		

			// Any Key symbol icons
			mKeySymArrows = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_arrows))
					.getNodeValue();
			mKeySymMic = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_mic))
					.getNodeValue();
			mKeySymReturn = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_return))
					.getNodeValue();
			mKeySymSearch = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_search))
					.getNodeValue();
			mKeySymSettings = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_settings))
					.getNodeValue();
			mKeySymTranslate = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_translate))
					.getNodeValue();
			mKeySymLocale = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_locale))
					.getNodeValue();
			mKeySymEmoji = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_emoji))
					.getNodeValue();
			mKeySymPopupShiftLocked = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_shift_locked))
					.getNodeValue();	
			mKeySymPopupShift =  propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_shift))
					.getNodeValue();
			mKeySymPopupEmoji = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_emoji))
					.getNodeValue();
			mKeySymPopupArrows = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_arrows))
					.getNodeValue();
			mKeySymPopupMic =   propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_mic))
					.getNodeValue();
			mKeySymPopupTranslate = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_translate))
					.getNodeValue();
			mKeySymPopupSettings = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_settings))
					.getNodeValue();
			mKeySymPopupSearch = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_search))
					.getNodeValue();
			mKeySymPopupReturn = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_return))
					.getNodeValue();
			mKeySymPopupLocale = propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_sym_kb_popup_locale))
					.getNodeValue();			


			// Candidates colors and background
			mCandidateBG = basePath + propMap.getNamedItem(
					res.getString(R.string.xml_theme_property_candidate_background))
					.getNodeValue();
			mCandidateBGColor = safeParseColor(propMap, 
					res.getString(R.string.xml_theme_property_candidate_background_color), mCandidateBGColor);
			mCandidateNormalColor = safeParseColor(propMap, 
					res.getString(R.string.xml_theme_property_candidate_normal_color), mCandidateNormalColor);
			mCandidateRecommendedColor = safeParseColor(propMap, 
					res.getString(R.string.xml_theme_property_candidate_recommended_color), mCandidateRecommendedColor);
			mCandidateDividerColor = safeParseColor(propMap, 
					res.getString(R.string.xml_theme_property_candidate_divider_color), mCandidateDividerColor);
			mCandidateMessageColor = safeParseColor(propMap, 
					res.getString(R.string.xml_theme_property_candidate_message_color), mCandidateMessageColor);
		} catch (Exception e) {
			ErrorReport.reportShortError(e, mContext, "loadKeyboardTheme");
		}
		
		loadKeyboardThemeDrawable();
	}
	

	
	/** Parse an integer attribute.
	 * 
	 * @param propMap
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	private int safeParseInt(NamedNodeMap propMap, String name, int defaultValue) {
		int value = defaultValue;
		
		try {
			value = Integer.parseInt(propMap.getNamedItem(name)
				.getNodeValue());
		} catch (Exception e) {
			// e.printStackTrace();
		}
		
		return value;
	}
	
	
	/**
	 * Parse a color attribute.
	 * 
	 * @param propMap
	 * @param name
	 * @param defaultColor
	 * @return
	 */
	private int safeParseColor(NamedNodeMap propMap, String name, int defaultColor) {
		int color = defaultColor;
		
		try {
			color = Color.parseColor(propMap.getNamedItem(name)
				.getNodeValue());
		} catch (Exception e) {
			// e.printStackTrace();
		}
		
		return color;
	}
	
	
	/**
	 * Load the theme drawable resources.
	 */
	private void loadKeyboardThemeDrawable() {
		try {
			// Key icons
			mKeySymReturnDrawable = Utils.getBitmapDrawable(mContext, mKeySymReturn, mKeyFGColor);
			mKeySymShiftLockedDrawable = Utils.getBitmapDrawable(mContext, mKeySymShiftLocked, mKeyFGColor);
			mKeySymShiftOnDrawable = Utils.getBitmapDrawable(mContext, mKeySymShiftOn, mKeyFGColor);
			mKeySymShiftOffDrawable = Utils.getBitmapDrawable(mContext, mKeySymShiftOff, mKeyFGColor);

			// Arrow key icons
			mKeySymArrowUpDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowUp, mKeyFGColor);
			mKeySymArrowLeftDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowLeft, mKeyFGColor);
			mKeySymArrowRightDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowRight, mKeyFGColor);
			mKeySymArrowDownDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowDown, mKeyFGColor);
			mKeySymArrowHomeDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowBack, mKeyFGColor);
			mKeySymArrowEndDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrowNext, mKeyFGColor);

			// Any Key icons
			mKeySymArrowsDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrows, mKeyFGColor);
			
			mKeySymSuperArrowsDrawable = Utils.getBitmapDrawable(mContext, mKeySymArrows, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymDeleteDrawable = Utils.getBitmapDrawable(mContext, mKeySymDelete, mKeyFGColor);
			mKeySymMicDrawable = Utils.getBitmapDrawable(mContext, mKeySymMic, mKeyFGColor);
			mKeySymSuperMicDrawable = Utils.getBitmapDrawable(mContext, mKeySymMic, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymSearchDrawable = Utils.getBitmapDrawable(mContext, mKeySymSearch, mKeyFGColor);
			mKeySymSuperSearchDrawable = Utils.getBitmapDrawable(mContext, mKeySymSearch, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymSettingsDrawable = Utils.getBitmapDrawable(mContext, mKeySymSettings, mKeyFGColor);
			mKeySymSuperSettingsDrawable = Utils.getBitmapDrawable(mContext, mKeySymSettings, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymSpaceDrawable = Utils.getBitmapDrawable(mContext, mKeySymSpace, mKeyFGColor);
			mKeySymTranslateDrawable = Utils.getBitmapDrawable(mContext, mKeySymTranslate, mKeyFGColor);
			mKeySymSuperTranslateDrawable = Utils.getBitmapDrawable(mContext, mKeySymTranslate, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymLocaleDrawable = Utils.getBitmapDrawable(mContext, mKeySymLocale, mKeyFGColor);
			mKeySymSuperLocaleDrawable = Utils.getBitmapDrawable(mContext, mKeySymLocale, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);
			mKeySymEmojiDrawable = Utils.getBitmapDrawable(mContext, mKeySymEmoji, mKeyFGColor);
			mKeySymSuperEmojiDrawable = Utils.getBitmapDrawable(mContext, mKeySymEmoji, getThemeName().equals("ICS") ? icsFnSuperColor : mKeySuperFGColor);

			mKeySymPopupShiftLockedDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupShiftLocked);
			mKeySymPopupShiftDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupShift);
			mKeySymPopupEmojiDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupEmoji);
			mKeySymPopupArrowsDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupArrows);
			mKeySymPopupMicDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupMic);
			mKeySymPopupTranslateDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupTranslate);
			mKeySymPopupLocaleDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupLocale);
			mKeySymPopupSettingsDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupSettings);
			mKeySymPopupSearchDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupSearch);
			mKeySymPopupReturnDrawable = Utils.getBitmapDrawable(mContext, mKeySymPopupReturn);

			// Key backgrounds
			mKeyAnyDrawable = Utils.getBitmapDrawable(mContext, mKeyAnyBG);
			/*mKeyFnBGDrawable = Utils.getBitmapDrawable(mContext, mKeyFNBG);
			mKeyBGDrawable = Utils.getBitmapDrawable(mContext, mKeyBG);*/
			
			mKeyFnBGDrawable = createStateListDrawable(mContext, mKeyFNBG, mKeyFNPressedBG);
			mKeyBGDrawable = createStateListDrawable(mContext, mKeyBG, mKeyPressedBG);
			
			
			mKeyboardBGDrawable = Utils.getBitmapDrawable(mContext, mKeyboardBG);
			mCandidateBGDrawable = Utils.getBitmapDrawable(mContext, mCandidateBG);
		} catch (Exception e) {
			Log.e(KeyboardApp.LOG_TAG, "exception ", e);
			reportError_loadKeyboardThemeDrawable(e);
		}
		
		
		Assert.assertNotNull(mKeySymReturnDrawable);
		Assert.assertNotNull(mKeySymShiftLockedDrawable);
		Assert.assertNotNull(mKeySymShiftOnDrawable);
		Assert.assertNotNull(mKeySymShiftOffDrawable);
		Assert.assertNotNull(mKeySymArrowUpDrawable);
		Assert.assertNotNull(mKeySymArrowLeftDrawable);
		Assert.assertNotNull(mKeySymArrowRightDrawable);
		Assert.assertNotNull(mKeySymArrowDownDrawable);
		Assert.assertNotNull(mKeySymArrowHomeDrawable);
		Assert.assertNotNull(mKeySymArrowEndDrawable);
		Assert.assertNotNull(mKeySymArrowsDrawable);
		Assert.assertNotNull(mKeySymSuperArrowsDrawable);
		Assert.assertNotNull(mKeySymDeleteDrawable);
		Assert.assertNotNull(mKeySymMicDrawable);
		Assert.assertNotNull(mKeySymSuperMicDrawable);
		Assert.assertNotNull(mKeySymSearchDrawable);
		Assert.assertNotNull(mKeySymSuperSearchDrawable);
		Assert.assertNotNull(mKeySymSettingsDrawable);
		Assert.assertNotNull(mKeySymSuperSettingsDrawable);
		Assert.assertNotNull(mKeySymSpaceDrawable);
		Assert.assertNotNull(mKeySymTranslateDrawable);
		Assert.assertNotNull(mKeySymSuperTranslateDrawable);
		Assert.assertNotNull(mKeySymLocaleDrawable);
		Assert.assertNotNull(mKeySymSuperLocaleDrawable);
		Assert.assertNotNull(mKeySymEmojiDrawable);
		Assert.assertNotNull(mKeySymSuperEmojiDrawable);
		Assert.assertNotNull(mKeySymPopupShiftLockedDrawable);
		Assert.assertNotNull(mKeySymPopupShiftDrawable);
		Assert.assertNotNull(mKeySymPopupEmojiDrawable);
		Assert.assertNotNull(mKeySymPopupArrowsDrawable);
		Assert.assertNotNull(mKeySymPopupMicDrawable);
		Assert.assertNotNull(mKeySymPopupTranslateDrawable);
		Assert.assertNotNull(mKeySymPopupLocaleDrawable);
		Assert.assertNotNull(mKeySymPopupSettingsDrawable);
		Assert.assertNotNull(mKeySymPopupSearchDrawable);
		Assert.assertNotNull(mKeySymPopupReturnDrawable);
		Assert.assertNotNull(mKeyAnyDrawable);
		Assert.assertNotNull(mKeyFnBGDrawable);
		Assert.assertNotNull(mKeyBGDrawable);
	}
	
	
	/**
	 * I DON'T KNOW WHAT THIS DOES
	 * @param context
	 * @param value
	 */
	public static void setThemeKey(Context context, String value) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString("theme", value);
		editor.commit();
	}
	
	
	
	/**
	 * I DON'T KNOW WHAT THIS DOES
	 * @param context
	 * @param value
	 */
	public static String getThemeKey(Context context) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		String theme = sharedPrefs.getString("theme", context.getString(R.string.default_theme_id));
		
		return theme;
	}
	
	
	
	/**
	 * Create a state list drawable object with two states: normal and pressed
	 */
	public Drawable createStateListDrawable(Context context, String normal, String pressed) throws IOException {
		Drawable normalDrawable = null, pressedDrawable = null;

		StateListDrawable listDrawable = null;
		int[] stateSet = new int[] {};
		int[] statePressedSet = new int[] {android.R.attr.state_pressed};

		Assert.assertTrue(normal != null);
		Assert.assertTrue(pressed != null);

		normalDrawable = Utils.getBitmapDrawable(context, normal);
		pressedDrawable = Utils.getBitmapDrawable(context, pressed);
		listDrawable = new StateListDrawable();
		listDrawable.addState(statePressedSet, pressedDrawable);
		listDrawable.addState(stateSet, normalDrawable);
		
		return listDrawable;
	}
	
	
	
	/**
	 * Get a drawable.
	 * @param mContext	A context.
	 * @param fieldID	The id of this drawable.
	 * 
	 * @return			The requested drawable, or null if not found.
	 */
	public Drawable getDrawable(int fieldID) {
		Drawable drawable = null;
		
		switch (fieldID) {
		case KEY_SYM_RETURN:
			drawable = mKeySymReturnDrawable;
			break;
		case KEY_SYM_SHIFT_LOCKED:
			drawable = mKeySymShiftLockedDrawable;
			break;
		case KEY_SYM_SHIFT_ON:
			drawable = mKeySymShiftOnDrawable;
			break;
		case KEY_SYM_SHIFT_OFF:
			drawable = mKeySymShiftOffDrawable;
			break;
		case KEY_SYM_ARROW_UP:
			drawable = mKeySymArrowUpDrawable;
			break;
		case KEY_SYM_ARROW_LEFT:
			drawable = mKeySymArrowLeftDrawable;
			break;
		case KEY_SYM_ARROW_RIGHT:
			drawable = mKeySymArrowRightDrawable;
			break;
		case KEY_SYM_ARROW_DOWN:
			drawable = mKeySymArrowDownDrawable;
			break;
		case KEY_SYM_ARROW_BACK:
			drawable = mKeySymArrowHomeDrawable;
			break;
		case KEY_SYM_ARROW_NEXT:
			drawable = mKeySymArrowEndDrawable;
			break;
		case KEY_SYM_ARROWS:
			drawable = mKeySymArrowsDrawable;
			break;
    case KEY_SYM_SUPER_ARROWS:
      drawable = mKeySymSuperArrowsDrawable;
      break;						
		case KEY_SYM_DELETE:
			drawable = mKeySymDeleteDrawable;
			break;
		case KEY_SYM_MIC:
			drawable = mKeySymMicDrawable;
			break;
    case KEY_SYM_SUPER_MIC:
      drawable = mKeySymSuperMicDrawable;
      break;			
		case KEY_SYM_SEARCH:
			drawable = mKeySymSearchDrawable;
			break;
    case KEY_SYM_SUPER_SEARCH:
      drawable = mKeySymSuperSearchDrawable;
      break;			
		case KEY_SYM_SETTINGS:
			drawable = mKeySymSettingsDrawable;
			break;
    case KEY_SYM_SUPER_SETTINGS:
      drawable = mKeySymSuperSettingsDrawable;
      break;			
		case KEY_SYM_SPACE:
			drawable = mKeySymSpaceDrawable;
			break;
		case KEY_SYM_TRANSLATE:
			drawable = mKeySymTranslateDrawable;
			break;
    case KEY_SYM_SUPER_TRANSLATE:
      drawable = mKeySymSuperTranslateDrawable;
      break;
		case KEY_SYM_LOCALE:
			drawable = mKeySymLocaleDrawable;
			break;
    case KEY_SYM_SUPER_LOCALE:
      drawable = mKeySymSuperLocaleDrawable;
      break;			
		case KEY_SYM_EMOJI:
			drawable = mKeySymEmojiDrawable;
			break;
    case KEY_SYM_SUPER_EMOJI:
      drawable = mKeySymSuperEmojiDrawable;
      break;
		case KEY_ANY:
			drawable = mKeyAnyDrawable;
			break;
		case KEY_FN_SS:
			drawable = mKeyFnSSBGDrawable;
			break;
		case KEY_FN_NORMAL:
			drawable = mKeyFnBGDrawable;
			break;
		case KEY_NORMAL:
			drawable = mKeyBGDrawable;
			break;
		case KEY_SS:
			drawable = mKeySSBGDrawable;
			break;
		case KB_BG:
			drawable = mKeyboardBGDrawable;
			break;
		case CANDIDATE_BG:
			drawable = mCandidateBGDrawable;
			break;
		case KEY_SYM_POPUP_SHIFT_LOCKED:
			drawable = mKeySymPopupShiftLockedDrawable;
			break;
		case KEY_SYM_POPUP_SHIFT:
			drawable = mKeySymPopupShiftDrawable;
			break;
		case KEY_SYM_POPUP_EMOJI:
			drawable = mKeySymPopupEmojiDrawable;
			break;
		case KEY_SYM_POPUP_ARROWS:
			drawable = mKeySymPopupArrowsDrawable;
			break;
		case KEY_SYM_POPUP_MIC:
			drawable = mKeySymPopupMicDrawable;
			break;
		case KEY_SYM_POPUP_TRANSLATE:
			drawable = mKeySymPopupTranslateDrawable;
			break;
		case KEY_SYM_POPUP_LOCALE:
			drawable = mKeySymPopupLocaleDrawable;
			break;
		case KEY_SYM_POPUP_SETTINGS:
			drawable = mKeySymPopupSettingsDrawable;
			break;
		case KEY_SYM_POPUP_SEARCH:
			drawable = mKeySymPopupSearchDrawable;
			break;
		case KEY_SYM_POPUP_RETURN:
			drawable = mKeySymPopupReturnDrawable;
			break;
			
		default:
			Assert.assertTrue("Not Implemented", false);
		}
		
		return drawable;
	}
	
	public int getBGColor() {
		return mKeyboardBGColor;
	}
	
	public int getBGAlpha() {
		return mKeyBGAlpha;
	}
	
	
	public int getKeyPressedAlpha() {
		return mKeyPressedAlpha;
	}
	
	
	public int getKeyFGColor() {
		return mKeyFGColor;
	}
	
	public int getKeySuperFGColor() {
		return mKeySuperFGColor;
	}
	
	public int getSuperHorizOffset() {
		return mKeySuperHorizOffset;
	}
	
	/******************************************************************
	 * CANDIDATE
	 *****************************************************************/
	public int getCandidateBGColor() {
		return mCandidateBGColor;
	}
	
	public int getCandidateNormalColor() {
		return mCandidateNormalColor;
	}
	
	public int getCandidateRecommendedColor() {
		return mCandidateRecommendedColor;
	}
	
	public int getCandidateDividerColor() {
		return mCandidateDividerColor;
	}
	
	public int getCandidateMessageColor() {
		return mCandidateMessageColor;
	}

	public String getName() {
		return mName;
	}
	
	public String getValue() {
		return KeyboardThemeManager.getKeyboardThemeValue(mContext, getName());
	}
	
	public void reportError_loadKeyboardThemeDrawable(Throwable e) {
		ErrorReport errorReport = new ErrorReport(mContext, e, "loadKeyboardThemesDrawable");
		
		try {
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
//			errorReport.putRunningProcesses();
//			errorReport.putInstalledPackages();
//			errorReport.putMainObjects();
			errorReport.putCallTrace();
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();

//		Toast toast = Toast.makeText(mContext, "Error report sent", Toast.LENGTH_LONG);
//		toast.show();
	}
}
