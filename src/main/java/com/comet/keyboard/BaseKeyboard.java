/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import com.comet.keyboard.models.KeyHeight;
import com.comet.keyboard.models.KeyScale;
import com.comet.keyboard.settings.KeyHeightSetting;
import com.comet.keyboard.settings.KeyPaddingHeightSetting;
import com.comet.keyboard.settings.KeyboardPaddingBottomSetting;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.theme.KeyboardTheme;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class BaseKeyboard extends Keyboard {

	protected Context mContext;
	protected Key mActionKey;
	protected Key mEmojiKey;
	protected Key mArrowsKey;
	protected Key mVoiceKey;
	protected Key mTranslateKey;
	protected Key mCurrencyKey;
	protected Key mUrlKey;
	protected Key mModeKey;
	protected Key mAnyKey;
	static public final int KEYCODE_SPACE = 32;
	static public final int KEYCODE_SHIFT = -1;
	static public final int KEYCODE_MODE = -2;
	static public final int KEYCODE_ACTION = KEYCODE_DONE;
	static public final int KEYCODE_DELETE = -5;
	static public final int KEYCODE_ANY_KEY = -100;
	static public final int KEYCODE_EMOJI = -101;
	static public final int KEYCODE_ARROWS = -102;
	static public final int KEYCODE_VOICE = -103;
	static public final int KEYCODE_TRANSLATE = -104;
	static public final int KEYCODE_SETTINGS = -105;
	static public final int KEYCODE_URL = -106;
	static public final int KEYCODE_ARROW_LEFT = -107;
	static public final int KEYCODE_ARROW_RIGHT = -108;
	static public final int KEYCODE_ARROW_UP = -109;
	static public final int KEYCODE_ARROW_DOWN = -110;
	static public final int KEYCODE_ARROW_HOME = -111;
	static public final int KEYCODE_ARROW_END = -112;
	static public final int KEYCODE_LOCALE = -113;
	static public final int KEYCODE_SYM_MENU = -114;
	static public final int KEYCODE_CUT = -115;
	static public final int KEYCODE_COPY = -116;
	static public final int KEYCODE_PASTE = -117;
	static public final int KEYCODE_SELECT = -118;
	static public final int KEYCODE_SELECT_ALL = -119;
	static public final int KEYCODE_ARROW_BACK = -120;
	static public final int KEYCODE_ARROW_NEXT = -121;
	static public final int KEYCODE_DEL_FORWARD = -122;

	static public final int KEY_ACTION_SAVE = -1000;
	static public final int KEY_ACTION_PREV = -1001;

	private int mTotalHeight;
	private int mXMLLayoutID;
	private KeyScale mKeyScale;
	private KeyHeight mKeyHeights;

	private int mEditorAction = EditorInfo.IME_ACTION_NONE;
	private boolean mNoEnterAction = false;

	private CharSequence mCharacters;
	private int mColumns;
	private int mHorizontalPadding;

	private boolean isCreatedByXML = false;

	private int[] mBottomGap;

	public BaseKeyboard(Context context, int xmlLayoutResId) {
		super(context, xmlLayoutResId);
		this.mContext = context;

		mXMLLayoutID = xmlLayoutResId;

		isCreatedByXML = true;

		// Get keyboard height
		KeyHeight keyHeight = KeyHeightSetting.getKeyHeightPreference(context);
		mBottomGap = KeyboardPaddingBottomSetting
				.getKeyboardPaddingBottomPreference(context);
		setKeyboardHeight(context, keyHeight);

		int keyPaddingHeight = KeyPaddingHeightSetting
				.getKeyPaddingHeightPreference(context); // portrait height
		setKeyboardPaddingHeight(context, keyPaddingHeight);
	}

	public BaseKeyboard(Context context, int layoutTemplateResId,
			CharSequence characters, int columns, int horizontalPadding) {
		super(context, layoutTemplateResId, characters, columns,
				horizontalPadding);

		mCharacters = characters;
		mColumns = columns;
		mHorizontalPadding = horizontalPadding;

		isCreatedByXML = false;

		this.mContext = context;

		// Get keyboard height
		KeyHeight keyHeight = KeyHeightSetting.getKeyHeightPreference(context);
		mBottomGap = KeyboardPaddingBottomSetting
				.getKeyboardPaddingBottomPreference(context);
		setKeyboardHeight(context, keyHeight);

		int keyPaddingHeight = KeyPaddingHeightSetting
				.getKeyPaddingHeightPreference(context); // portrait height
		setKeyboardPaddingHeight(context, keyPaddingHeight);
	}

	@Override
	protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
			XmlResourceParser parser) {
		Key key = new LatinKey(res, parent, x, y, parser);
		assignSpecialKey(key);

		return key;
	}

	public int getXMLResID() {
		return mXMLLayoutID;
	}

	/**
	 * Set complicated property of key heights
	 * 
	 * @param height
	 */
	public void setKeyHeights(KeyHeight height) {
		mKeyHeights = height;
	}

	// Set keyboard height
	public void setKeyboardHeight(Context context, KeyHeight height) {
		setKeyHeight(height.getRowDefault());

		setKeyHeights(height);

		mKeyScale = new KeyScale(height, context.getResources()
				.getDimensionPixelSize(R.dimen.key_height));

		if (mKeyScale.getRowDefault() > 1.0f)
			mKeyScale.setRowDefault(1.0f);

		if (mKeyScale.getRowBottom() > 1.0f)
			mKeyScale.setRowBottom(1.0f);

		// Reset keyboard height
		if (isCreatedByXML)
			resetKeyboardHeight(context,
					context.getResources().getXml(mXMLLayoutID));
		else
			resetKeyboardHeight(mCharacters, mColumns, mHorizontalPadding);
	}

	public void setKeyboardPaddingHeight(Context context, int height) {
		setVerticalGap(height);

		// Reset keyboard height
		if (isCreatedByXML)
			resetKeyboardHeight(context,
					context.getResources().getXml(mXMLLayoutID));
		else
			resetKeyboardHeight(mCharacters, mColumns, mHorizontalPadding);
	}

	public int getHeight() {
		if (isCreatedByXML)
			return mTotalHeight;
		else
			return super.getKeyHeight();
	}

	/**
	 * Gets key heights
	 */
	protected KeyHeight getKeyHeights() {
		return mKeyHeights;
	}

	private static final String TAG_ROW = "Row";
	private static final String TAG_KEY = "Key";
	private static final String TAG_GAP = "Gap";

	protected void resetKeyboardHeight(Context context, XmlResourceParser parser) {
		boolean inKey = false;
		boolean inRow = false;
		int y = 0;
		Key key = null;
		Row currentRow = null;
		Resources res = context.getResources();
		mTotalHeight = 0;

		// NOTE #1 : Setting verticalGap only works below the row.
		// #2 It also works for last drawing object, not for the actually set
		// object.
		// So, we should issue setting command when we set the first time for
		// the right above row from the bottom.
		// This is weird thing, but Android provides that way.
		try {
			int event;
			int index = 0;
			// Calculate the Row count;
			// Get the raw and draw it.
			while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
				if (event == XmlResourceParser.START_TAG) {
					String tag = parser.getName();
					if (TAG_ROW.equals(tag)) {
						inRow = true;
						currentRow = createRowFromXml(res, parser);

						if (currentRow.mode != 0) {
							skipToEndOfRow(parser);
							inRow = false;
						}
					} else if (TAG_KEY.equals(tag)) {
						inKey = true;
						key = getKeys().get(index++);
						if (currentRow.rowEdgeFlags == EDGE_BOTTOM) {
							key.height = getKeyHeights().countRowBottom();
						} else {
							key.height = getKeyHeight();
						}						
						// needs to fix bug with any key flags							
						key.edgeFlags |= currentRow.rowEdgeFlags;
						key.y = y;
					} else if (TAG_GAP.equals(tag)) {
						inRow = false;
						int paddingGap = getVerticalGap() + getBottomGap(0);
						currentRow.verticalGap += paddingGap;
						if (currentRow.rowEdgeFlags == EDGE_BOTTOM)
							y += (paddingGap + getKeyHeights().countRowBottom());
						else
							y += (paddingGap + getKeyHeight());
					}
				} else if (event == XmlResourceParser.END_TAG) {
					if (inKey) {
						inKey = false;
					} else if (inRow) {
						inRow = false;
						// ugly
						currentRow.verticalGap += getVerticalGap();
						y += currentRow.verticalGap;
						if (currentRow.rowEdgeFlags == EDGE_BOTTOM) {
							y += getKeyHeights().countRowBottom();
						} else {
							y += getKeyHeight();
						}
					} else {
						// TODO: error or extend?
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentRow.verticalGap += getBottomGap(1);
		y += getBottomGap(1);
		mTotalHeight = y - getVerticalGap();
	}

	protected void resetKeyboardHeight(CharSequence characters, int columns,
			int horizontalPadding) {
		int x = 0;
		int y = 0;
		int column = 0;

		int mDisplayWidth;
		int mTotalWidth;
		int index = 0;

		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		mDisplayWidth = dm.widthPixels;
		// Log.v(TAG, "keyboard's display metrics:" + dm);

		int mDefaultHorizontalGap = 0;
		int mDefaultWidth = mDisplayWidth / 10;
		int mDefaultVerticalGap = 0;
		int mDefaultHeight = mDefaultWidth;

		mTotalWidth = 0;

		final int maxColumns = columns == -1 ? Integer.MAX_VALUE : columns;
		for (int i = 0; i < characters.length(); i++) {
			if (column >= maxColumns
					|| x + mDefaultWidth + horizontalPadding > mDisplayWidth) {
				x = 0;
				y += mDefaultVerticalGap + mDefaultHeight;
				column = 0;
			}

			Key key = getKeys().get(index++);
			key.height = getKeyHeight();
			key.y = y;			

			column++;
			x += mDefaultWidth + mDefaultHorizontalGap;
			if (x > mTotalWidth) {
				mTotalWidth = x;
			}
		}
		mTotalHeight = y + mDefaultHeight;
	}

	private void skipToEndOfRow(XmlResourceParser parser)
			throws XmlPullParserException, IOException {
		int event;
		while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
			if (event == XmlResourceParser.END_TAG
					&& parser.getName().equals(TAG_ROW)) {
				break;
			}
		}
	}

	/**
	 * Keep track of certain keys
	 * 
	 * @param key
	 *            A key to check
	 */
	protected void assignSpecialKey(Key key) {
		assignSpecialKey(key, key.codes[0]);
	}

	/**
	 * Keep track of certain keys by primary code
	 * 
	 * @param key
	 *            A key to check
	 */
	protected void assignSpecialKey(Key key, final int primaryCode) {
		if (key.codes[0] == KEYCODE_ANY_KEY)
			mAnyKey = key;

		if (primaryCode == KEYCODE_ACTION)
			mActionKey = key;
		else if (primaryCode == KEYCODE_EMOJI)
			mEmojiKey = key;
		else if (primaryCode == KEYCODE_ARROWS)
			mArrowsKey = key;
		else if (primaryCode == KEYCODE_VOICE)
			mVoiceKey = key;
		else if (primaryCode == KEYCODE_TRANSLATE)
			mTranslateKey = key;
		else if (primaryCode == KEYCODE_URL)
			mUrlKey = key;
		else if (primaryCode == KEYCODE_MODE)
			mModeKey = key;
		else if ((key.label != null && key.label.equals("$"))
				|| (key.popupCharacters != null && key.popupCharacters
				.equals("$")))
			mCurrencyKey = key;

		checkForNulls("assignSpecialKey()", key);
	}



	/**
	 * This looks at the ime options given by the current editor, to set the
	 * appropriate label on the keyboard's enter key (if it has one).
	 */
	void setImeOptions(Resources res, EditorInfo optionInfo) {
		if (mActionKey == null) {
			return;
		}

		int options = optionInfo.imeOptions;
		int actionId = optionInfo.actionId;
		CharSequence actionLabel = optionInfo.actionLabel;

		if (actionId == KEY_ACTION_PREV) {
			mActionKey.icon = null;
			mActionKey.iconPreview = null;
			mActionKey.label = actionLabel;
			checkForNulls("setImeOptions():1", mActionKey);
			return;
		}

		// Reset action key values.
		mActionKey.icon = null;
		mActionKey.iconPreview = null;
		mActionKey.label = null;
		mActionKey.text = null;
		mActionKey.popupCharacters = null;
		mActionKey.repeatable = false;
		mEditorAction = options
				& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
		mNoEnterAction = false;
		switch (mEditorAction) {
		case EditorInfo.IME_ACTION_DONE :
		case EditorInfo.IME_ACTION_UNSPECIFIED :
			mActionKey.label = res.getText(R.string.key_label_done);
			break;
		case EditorInfo.IME_ACTION_GO :
			mActionKey.label = res.getText(R.string.key_label_go);
			break;
		case EditorInfo.IME_ACTION_NEXT :
			mActionKey.label = res.getText(R.string.key_label_next);
			break;
		case EditorInfo.IME_ACTION_SEARCH :
			mActionKey.icon = KeyboardThemeManager.getCurrentTheme()
			.getDrawable(KeyboardTheme.KEY_SYM_SEARCH);
			mActionKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
					.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SEARCH);
			break;
		case EditorInfo.IME_ACTION_SEND :
			mActionKey.label = res.getText(R.string.key_label_send);
			break;
		default :
			// Display the return key
			mActionKey.icon = KeyboardThemeManager.getCurrentTheme()
			.getDrawable(KeyboardTheme.KEY_SYM_RETURN);
			mActionKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
					.getDrawable(KeyboardTheme.KEY_SYM_POPUP_RETURN);
			mActionKey.repeatable = false;

			if ((options & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
				Log.v(KeyboardApp.LOG_TAG, "flag no enter");
				// Set a long press action
				//mEditorAction = options & EditorInfo.IME_MASK_ACTION;
				mNoEnterAction = true;
				/*					switch (mEditorAction) {
						case EditorInfo.IME_ACTION_GO :
							mActionKey.popupCharacters = "{"
									+ res.getText(R.string.key_label_go) + "}";
							mActionKey.repeatable = false;
							break;
						case EditorInfo.IME_ACTION_NEXT :
							mActionKey.popupCharacters = "{"
									+ res.getText(R.string.key_label_next)
									+ "}";
							mActionKey.repeatable = false;
							break;
						case EditorInfo.IME_ACTION_SEND :
							mActionKey.popupCharacters = "{"
									+ res.getText(R.string.key_label_send)
									+ "}";
							mActionKey.repeatable = false;
							break;
						default :
							// Other action types are ignored
							mEditorAction = EditorInfo.IME_ACTION_NONE;
					}*/

			}

			break;
		}

		checkForNulls("setImeOptions():2", mActionKey);

		updateKeyBounds(mActionKey);
	}

	/**
	 * Returns the default editor action for the action key
	 * 
	 * @param onLongPress
	 *            If true, returns the editor action on long press of the action
	 *            key
	 * @return An IME_ACTION_* constant defined in EditorInfo
	 */
	public int getEditorAction(boolean onLongPress) {
		return onLongPress ? (mNoEnterAction
				? mEditorAction
						: EditorInfo.IME_ACTION_NONE) : mEditorAction;
	}

	public static void updateKeyBounds(Key key) {
		if (key.icon != null)
			key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(),
					key.icon.getIntrinsicHeight());

		if (key.iconPreview != null)
			key.iconPreview.setBounds(0, 0,
					key.iconPreview.getIntrinsicWidth(),
					key.iconPreview.getIntrinsicHeight());
	}

	public Key getActionKey() {
		return mActionKey;
	}

	public Key getAnyKey() {
		return mAnyKey;
	}

	public Key getEmojiKey() {
		return mEmojiKey;
	}

	public Key getArrowsKey() {
		return mArrowsKey;
	}

	public Key getVoiceKey() {
		return mVoiceKey;
	}

	public Key getTranslateKey() {
		return mTranslateKey;
	}

	public Key getModeKey() {
		return mModeKey;
	}

	protected KeyScale getKeyScale() {
		return mKeyScale;
	}



	static public class LatinKey extends Keyboard.Key {

		public LatinKey(Resources res, Keyboard.Row parent, int x, int y,
				XmlResourceParser parser) {
			super(res, parent, x, y, parser);
			assignIcon();

			BaseKeyboard.checkForNulls("LatinKey()", this);
		}

		/**
		 * Overriding this method so that we can reduce the target area for the
		 * key that closes the keyboard.
		 */
		@Override
		public boolean isInside(int x, int y) {
			return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
		}



		private void assignIcon() {
			KeyboardTheme currentTheme = KeyboardThemeManager.getCurrentTheme();

			switch(codes[0]) {

			case KEYCODE_ANY_KEY:
				Context context = KeyboardApp.getApp();
				SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
				String anyKeyState = sharedPrefs.getString("any_key_action", context.getString(R.string.any_key_action_id_default));
				icon = getAnyKeyIcon(context, anyKeyState, false);
				break;

			case KEYCODE_SHIFT:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SHIFT_OFF);
				break;

			case KEYCODE_ARROW_LEFT:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_LEFT);
				break;
			case KEYCODE_ARROW_RIGHT:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_RIGHT);
				break;
			case KEYCODE_ARROW_UP:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_UP);
				break;
			case KEYCODE_ARROW_DOWN:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_DOWN);
				break;
			case KEYCODE_ARROW_BACK:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_BACK);
				break;
			case KEYCODE_ARROW_NEXT:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_NEXT);
				break;
			case KEYCODE_DELETE:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_DELETE);
				break;
			case KEYCODE_SPACE:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SPACE);
				break;
			case KEYCODE_ARROWS:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROWS);
				break;
			case KEYCODE_VOICE:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_MIC);
				break;
			case KEYCODE_SETTINGS:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SETTINGS);
				break;
			case KEYCODE_TRANSLATE:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_TRANSLATE);
				break;
			case KEYCODE_LOCALE:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_LOCALE);
				break;
			case KEYCODE_EMOJI:
				icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_EMOJI);
				break;
			}
		}



		/**
		 * Get icon for short or long state action
		 * 
		 * @param state
		 * @param superEnabled
		 * @return
		 */
		protected Drawable getAnyKeyIcon(Context context, String state, boolean superEnabled){
			Drawable icon = null;

			KeyboardTheme theme = KeyboardThemeManager.getCurrentTheme(); 

			if (state.equals(context.getString(R.string.any_key_action_id_emoji_menu))) {
				// Emoji
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_EMOJI : KeyboardTheme.KEY_SYM_EMOJI);
			} else if (state.equals(context.getString(R.string.any_key_action_id_arrow_keypad))) {
				// Arrow keypad
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_ARROWS : KeyboardTheme.KEY_SYM_ARROWS);
			} else if (state.equals(context.getString(R.string.any_key_action_id_voice_input))) {
				// Voice input
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_MIC : KeyboardTheme.KEY_SYM_MIC);
			} else if (state.equals(context.getString(R.string.any_key_action_id_translator))) {
				// Translator
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_TRANSLATE : KeyboardTheme.KEY_SYM_TRANSLATE);
			} else if (state.equals(context.getString(R.string.any_key_action_id_locale))) {
				// Language
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_LOCALE : KeyboardTheme.KEY_SYM_LOCALE);
			} else if (state.equals(context.getString(R.string.any_key_action_id_settings))) {
				// Settings
				icon = theme.getDrawable(superEnabled ? KeyboardTheme.KEY_SYM_SUPER_SETTINGS : KeyboardTheme.KEY_SYM_SETTINGS);
			}  

			return icon;

		}

	}

	public String toString() {
		return toString("");
	}

	public String toString(String name) {
		StringBuilder buffer = new StringBuilder();
		Utils.appendLine(buffer, name, getClass().getName());
		Utils.appendLine(buffer, name, "{");

		String subName = name + "\t";

		try {
			// Retrieve all variables.
			Utils.appendLine(buffer, subName, "mTotalHeight" + " = " + mTotalHeight);
			Utils.appendLine(buffer, subName, "mXMLLayoutID" + " = " + mXMLLayoutID);
			Utils.appendLine(buffer, subName, "mColumns" + " = " + mColumns);
			Utils.appendLine(buffer, subName, "mHorizontalPadding" + " = " + mHorizontalPadding);

			// Add all native fields
			Utils.appendLine(buffer, subName, Utils.getClassString(this, subName));
		} catch (Exception e) {
			Utils.appendLine(buffer, subName, "exception = " + e.getMessage());
		}
		Utils.appendLine(buffer, name, "}");

		return buffer.toString();
	}

	public int[] getBottomGap() {
		return mBottomGap;
	}
	public int getBottomGap(int index) {
		return mBottomGap[index];
	}

	public void setBottomGap(int[] bottomGap) {
		mBottomGap = bottomGap;
	}


	protected static int getAlphaKeyboardLayout(Context context, String keyboardLayoutID) {
		if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwerty_sp))) {
			return R.xml.qwerty_es;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwerty_intl))) {
			return R.xml.qwerty_intl;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_azerty_fr))) {
			return R.xml.azerty_fr;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwerty_sl))) {
			return R.xml.qwerty_sl;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwerty_sv))) {
			return R.xml.qwerty_sv;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_azerty_be))) {
			return R.xml.azerty_be;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwertz_de))) {
			return R.xml.qwertz_de;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_qwertz_sl))) {
			return R.xml.qwertz_sl;
		} else if (keyboardLayoutID.equals(context.getString(R.string.kb_id_t9))) {
			return R.xml.t9;
		} else {
			return R.xml.qwerty_en;
		}
	}


	// <DEBUG>
//	private static int mErrorSent = 0; // Send error report max 3 times, for performance reasons.
	public static void checkForNulls(String location, Key key) {
		//		if(key.label == null && key.icon == null && mErrorSent++ < 3) {
		//			// Send error report
		//			String theme = "null_theme";
		//			if(KeyboardThemeManager.getCurrentTheme() != null)
		//				theme = KeyboardThemeManager.getCurrentTheme().getName();
		//			String details = "location=" + location + ",key.label=" + key.label + ",key.codes=" + Arrays.toString(key.codes) + ",theme=" + theme + ",keyboard=" + KeyboardLayout.getLayoutId();
		//			ErrorReport.reportShortError(KeyboardApp.getKeyboardApp(), "null_key", details);
		//		}
	}
	// </DEBUG>
}
