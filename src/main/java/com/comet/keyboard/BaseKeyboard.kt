@file:Suppress("DEPRECATION")

/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.keyboard

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.util.Log
import android.view.inputmethod.EditorInfo
import com.comet.keyboard.models.KeyHeight
import com.comet.keyboard.models.KeyScale
import com.comet.keyboard.settings.KeyHeightSetting
import com.comet.keyboard.settings.KeyPaddingHeightSetting
import com.comet.keyboard.settings.KeyboardPaddingBottomSetting
import com.comet.keyboard.settings.Settings
import com.comet.keyboard.theme.KeyboardTheme
import com.comet.keyboard.theme.KeyboardThemeManager
import com.comet.keyboard.util.Utils
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

open class BaseKeyboard : Keyboard {
    protected var mContext: Context
    var actionKey: Key? = null
        protected set
    var emojiKey: Key? = null
        protected set
    var arrowsKey: Key? = null
        protected set
    var voiceKey: Key? = null
        protected set
    var translateKey: Key? = null
        protected set
    var mCurrencyKey: Key? = null
    var mUrlKey: Key? = null
    var modeKey: Key? = null
        protected set
    var anyKey: Key? = null
        protected set
    private var mTotalHeight = 0
    var xmlResID = 0
        private set
    var keyScale: KeyScale? = null
        private set
    /**
     * Gets key heights
     */
    protected var keyHeights: KeyHeight? = null
    private var mEditorAction = EditorInfo.IME_ACTION_NONE
    private var mNoEnterAction = false
    private var mCharacters: CharSequence? = null
    private var mColumns = 0
    private var mHorizontalPadding = 0
    private var isCreatedByXML = false
    var bottomGap: IntArray

    constructor(context: Context, xmlLayoutResId: Int) : super(context, xmlLayoutResId) {
        mContext = context
        xmlResID = xmlLayoutResId
        isCreatedByXML = true

        // Get keyboard height
        val keyHeight = KeyHeightSetting.getKeyHeightPreference(context)
        bottomGap = KeyboardPaddingBottomSetting
            .getKeyboardPaddingBottomPreference(context)
        setKeyboardHeight(context, keyHeight)
        val keyPaddingHeight = KeyPaddingHeightSetting
            .getKeyPaddingHeightPreference(context) // portrait height
        setKeyboardPaddingHeight(context, keyPaddingHeight)
    }

    constructor(
        context: Context, layoutTemplateResId: Int,
        characters: CharSequence?, columns: Int, horizontalPadding: Int
    ) : super(
        context, layoutTemplateResId, characters, columns,
        horizontalPadding
    ) {
        mCharacters = characters
        mColumns = columns
        mHorizontalPadding = horizontalPadding
        isCreatedByXML = false
        mContext = context

        // Get keyboard height
        val keyHeight = KeyHeightSetting.getKeyHeightPreference(context)
        bottomGap = KeyboardPaddingBottomSetting
            .getKeyboardPaddingBottomPreference(context)
        setKeyboardHeight(context, keyHeight)
        val keyPaddingHeight = KeyPaddingHeightSetting
            .getKeyPaddingHeightPreference(context) // portrait height
        setKeyboardPaddingHeight(context, keyPaddingHeight)
    }

    @Deprecated("Deprecated in Java")
    override fun createKeyFromXml(
        res: Resources, parent: Row, x: Int, y: Int,
        parser: XmlResourceParser
    ): Key {
        val key: Key = LatinKey(res, parent, x, y, parser)
        assignSpecialKey(key)
        return key
    }

    // Set keyboard height
    fun setKeyboardHeight(context: Context, height: KeyHeight) {
        keyHeight = height.rowDefault
        keyHeights = height
        keyScale = KeyScale(
            height, context.resources
                .getDimensionPixelSize(R.dimen.key_height)
        )
        if (keyScale!!.rowDefault > 1.0f) keyScale!!.rowDefault = 1.0f
        if (keyScale!!.rowBottom > 1.0f) keyScale!!.rowBottom = 1.0f

        // Reset keyboard height
        if (isCreatedByXML) resetKeyboardHeight(
            context,
            context.resources.getXml(xmlResID)
        ) else resetKeyboardHeight(mCharacters, mColumns, mHorizontalPadding)
    }

    fun setKeyboardPaddingHeight(context: Context, height: Int) {
        verticalGap = height

        // Reset keyboard height
        if (isCreatedByXML) resetKeyboardHeight(
            context,
            context.resources.getXml(xmlResID)
        ) else resetKeyboardHeight(mCharacters, mColumns, mHorizontalPadding)
    }

    @Deprecated("Deprecated in Java")
    override fun getHeight(): Int {
        return if (isCreatedByXML) mTotalHeight else super.getKeyHeight()
    }

    protected fun resetKeyboardHeight(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        var y = 0
        var key: Key?
        var currentRow: Row? = null
        val res = context.resources
        mTotalHeight = 0

        // NOTE #1 : Setting verticalGap only works below the row.
        // #2 It also works for last drawing object, not for the actually set
        // object.
        // So, we should issue setting command when we set the first time for
        // the right above row from the bottom.
        // This is weird thing, but Android provides that way.
        try {
            var event: Int
            var index = 0
            // Calculate the Row count;
            // Get the raw and draw it.
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_ROW == tag) {
                        inRow = true
                        currentRow = createRowFromXml(res, parser)
                        if (currentRow.mode != 0) {
                            skipToEndOfRow(parser)
                            inRow = false
                        }
                    } else if (TAG_KEY == tag) {
                        inKey = true
                        key = keys[index++]
                        if (currentRow!!.rowEdgeFlags == EDGE_BOTTOM) {
                            key.height = keyHeights!!.countRowBottom()
                        } else {
                            key.height = keyHeight
                        }
                        // needs to fix bug with any key flags							
                        key.edgeFlags = key.edgeFlags or currentRow.rowEdgeFlags
                        key.y = y
                    } else if (TAG_GAP == tag) {
                        inRow = false
                        val paddingGap = verticalGap + getBottomGap(0)
                        currentRow!!.verticalGap += paddingGap
                        y += if (currentRow!!.rowEdgeFlags == EDGE_BOTTOM) paddingGap + keyHeights!!.countRowBottom() else paddingGap + keyHeight
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                    } else if (inRow) {
                        inRow = false
                        // ugly
                        currentRow!!.verticalGap += verticalGap
                        y += currentRow!!.verticalGap
                        y += if (currentRow.rowEdgeFlags == EDGE_BOTTOM) {
                            keyHeights!!.countRowBottom()
                        } else {
                            keyHeight
                        }
                    } else {
                        // TODO: error or extend?
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentRow!!.verticalGap += getBottomGap(1)
        y += getBottomGap(1)
        mTotalHeight = y - verticalGap
    }

    protected fun resetKeyboardHeight(
        characters: CharSequence?, columns: Int,
        horizontalPadding: Int
    ) {
        var x = 0
        var y = 0
        var column = 0
        val mDisplayWidth: Int
        var mTotalWidth: Int
        var index = 0
        val dm = mContext.resources.displayMetrics
        mDisplayWidth = dm.widthPixels
        // Log.v(TAG, "keyboard's display metrics:" + dm);
        val mDefaultHorizontalGap = 0
        val mDefaultWidth = mDisplayWidth / 10
        val mDefaultVerticalGap = 0
        mTotalWidth = 0
        val maxColumns = if (columns == -1) Int.MAX_VALUE else columns
        for (i in 0 until characters!!.length) {
            if (column >= maxColumns
                || x + mDefaultWidth + horizontalPadding > mDisplayWidth
            ) {
                x = 0
                y += mDefaultVerticalGap + mDefaultWidth
                column = 0
            }
            val key = keys[index++]
            key.height = keyHeight
            key.y = y
            column++
            x += mDefaultWidth + mDefaultHorizontalGap
            if (x > mTotalWidth) {
                mTotalWidth = x
            }
        }
        mTotalHeight = y + mDefaultWidth
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipToEndOfRow(parser: XmlResourceParser) {
        var event: Int
        while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG && parser.name == TAG_ROW) {
                break
            }
        }
    }
    /**
     * Keep track of certain keys by primary code
     *
     * @param key
     * A key to check
     */
    fun assignSpecialKey(key: Key, primaryCode: Int = key.codes[0]) {
        if (key.codes[0] == KEYCODE_ANY_KEY) anyKey = key
        if (primaryCode == KEYCODE_ACTION) actionKey =
            key else if (primaryCode == KEYCODE_EMOJI) emojiKey =
            key else if (primaryCode == KEYCODE_ARROWS) arrowsKey =
            key else if (primaryCode == KEYCODE_VOICE) voiceKey =
            key else if (primaryCode == KEYCODE_TRANSLATE) translateKey =
            key else if (primaryCode == KEYCODE_URL) mUrlKey =
            key else if (primaryCode == KEYCODE_MODE) modeKey =
            key else if (key.label != null && key.label == "$" || key.popupCharacters != null && (key.popupCharacters
                == "$")
        ) mCurrencyKey = key
        checkForNulls("assignSpecialKey()", key)
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    fun setImeOptions(res: Resources, optionInfo: EditorInfo) {
        if (actionKey == null) {
            return
        }
        val options = optionInfo.imeOptions
        val actionId = optionInfo.actionId
        val actionLabel = optionInfo.actionLabel
        if (actionId == KEY_ACTION_PREV) {
            actionKey!!.icon = null
            actionKey!!.iconPreview = null
            actionKey!!.label = actionLabel
            checkForNulls("setImeOptions():1", actionKey)
            return
        }

        // Reset action key values.
        actionKey!!.icon = null
        actionKey!!.iconPreview = null
        actionKey!!.label = null
        actionKey!!.text = null
        actionKey!!.popupCharacters = null
        actionKey!!.repeatable = false
        mEditorAction = (options
            and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION))
        mNoEnterAction = false
        when (mEditorAction) {
            EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_UNSPECIFIED -> actionKey!!.label =
                res.getText(R.string.key_label_done)

            EditorInfo.IME_ACTION_GO -> actionKey!!.label = res.getText(R.string.key_label_go)
            EditorInfo.IME_ACTION_NEXT -> actionKey!!.label = res.getText(R.string.key_label_next)
            EditorInfo.IME_ACTION_SEARCH -> {
                actionKey!!.icon = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_SEARCH)
                actionKey!!.iconPreview = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_POPUP_SEARCH)
            }

            EditorInfo.IME_ACTION_SEND -> actionKey!!.label = res.getText(R.string.key_label_send)
            else -> {
                // Display the return key
                actionKey!!.icon = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_RETURN)
                actionKey!!.iconPreview = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_POPUP_RETURN)
                actionKey!!.repeatable = false
                if (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) {
                    Log.v(KeyboardApp.LOG_TAG, "flag no enter")
                    // Set a long press action
                    //mEditorAction = options & EditorInfo.IME_MASK_ACTION;
                    mNoEnterAction = true
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
            }
        }
        checkForNulls("setImeOptions():2", actionKey)
        updateKeyBounds(actionKey!!)
    }

    /**
     * Returns the default editor action for the action key
     *
     * @param onLongPress
     * If true, returns the editor action on long press of the action
     * key
     * @return An IME_ACTION_* constant defined in EditorInfo
     */
    fun getEditorAction(onLongPress: Boolean): Int {
        return if (onLongPress) (if (mNoEnterAction) mEditorAction else EditorInfo.IME_ACTION_NONE) else mEditorAction
    }

    class LatinKey(
        res: Resources?, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ) : Key(res, parent, x, y, parser) {
        init {
            assignIcon()
            checkForNulls("LatinKey()", this)
        }

        /**
         * Overriding this method so that we can reduce the target area for the
         * key that closes the keyboard.
         */
        @Deprecated("Deprecated in Java")
        override fun isInside(x: Int, y: Int): Boolean {
            return super.isInside(x, if (codes[0] == KEYCODE_CANCEL) y - 10 else y)
        }

        private fun assignIcon() {
            val currentTheme = KeyboardThemeManager.getCurrentTheme()
            when (codes[0]) {
                KEYCODE_ANY_KEY -> {
                    val context: Context = KeyboardApp.getApp()
                    val sharedPrefs =
                        context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE)
                    val anyKeyState = sharedPrefs.getString(
                        "any_key_action",
                        context.getString(R.string.any_key_action_id_default)
                    )
                    icon = getAnyKeyIcon(context, anyKeyState, false)
                }

                KEYCODE_SHIFT -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SHIFT_OFF)
                KEYCODE_ARROW_LEFT -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_LEFT)

                KEYCODE_ARROW_RIGHT -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_RIGHT)

                KEYCODE_ARROW_UP -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_UP)
                KEYCODE_ARROW_DOWN -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_DOWN)

                KEYCODE_ARROW_BACK -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_BACK)

                KEYCODE_ARROW_NEXT -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROW_NEXT)

                KEYCODE_DELETE -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_DELETE)
                KEYCODE_SPACE -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SPACE)
                KEYCODE_ARROWS -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_ARROWS)
                KEYCODE_VOICE -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_MIC)
                KEYCODE_SETTINGS -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_SETTINGS)
                KEYCODE_TRANSLATE -> icon =
                    currentTheme.getDrawable(KeyboardTheme.KEY_SYM_TRANSLATE)

                KEYCODE_LOCALE -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_LOCALE)
                KEYCODE_EMOJI -> icon = currentTheme.getDrawable(KeyboardTheme.KEY_SYM_EMOJI)
            }
        }

        /**
         * Get icon for short or long state action
         *
         * @param state
         * @param superEnabled
         * @return
         */
        private fun getAnyKeyIcon(
            context: Context,
            state: String?,
            @Suppress("SameParameterValue") superEnabled: Boolean
        ): Drawable? {
            var icon: Drawable? = null
            val theme = KeyboardThemeManager.getCurrentTheme()
            when (state) {
                context.getString(R.string.any_key_action_id_emoji_menu), context.getString(R.string.any_key_action_id_smiley_menu_deprecated) -> {
                    // Emoji
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_EMOJI else KeyboardTheme.KEY_SYM_EMOJI)
                }
                context.getString(R.string.any_key_action_id_arrow_keypad) -> {
                    // Arrow keypad
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_ARROWS else KeyboardTheme.KEY_SYM_ARROWS)
                }
                context.getString(R.string.any_key_action_id_voice_input) -> {
                    // Voice input
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_MIC else KeyboardTheme.KEY_SYM_MIC)
                }
                context.getString(R.string.any_key_action_id_translator) -> {
                    // Translator
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_TRANSLATE else KeyboardTheme.KEY_SYM_TRANSLATE)
                }
                context.getString(R.string.any_key_action_id_locale) -> {
                    // Language
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_LOCALE else KeyboardTheme.KEY_SYM_LOCALE)
                }
                context.getString(R.string.any_key_action_id_settings) -> {
                    // Settings
                    icon = theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_SETTINGS else KeyboardTheme.KEY_SYM_SETTINGS)
                }
            }
            return icon
        }
    }

    override fun toString(): String {
        return toString("")
    }

    fun toString(name: String): String {
        val buffer = StringBuilder()
        Utils.appendLine(buffer, name, javaClass.name)
        Utils.appendLine(buffer, name, "{")
        val subName = name + "\t"
        try {
            // Retrieve all variables.
            Utils.appendLine(buffer, subName, "mTotalHeight = $mTotalHeight")
            Utils.appendLine(buffer, subName, "mXMLLayoutID" + " = " + xmlResID)
            Utils.appendLine(buffer, subName, "mColumns = $mColumns")
            Utils.appendLine(buffer, subName, "mHorizontalPadding = $mHorizontalPadding")

            // Add all native fields
            Utils.appendLine(buffer, subName, Utils.getClassString(this, subName))
        } catch (e: Exception) {
            Utils.appendLine(buffer, subName, "exception = " + e.message)
        }
        Utils.appendLine(buffer, name, "}")
        return buffer.toString()
    }

    fun getBottomGap(index: Int): Int {
        return bottomGap[index]
    }

    companion object {
        const val KEYCODE_SPACE = 32
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE = -2
        const val KEYCODE_ACTION = KEYCODE_DONE
        const val KEYCODE_DELETE = -5
        const val KEYCODE_ANY_KEY = -100
        const val KEYCODE_EMOJI = -101
        const val KEYCODE_ARROWS = -102
        const val KEYCODE_VOICE = -103
        const val KEYCODE_TRANSLATE = -104
        const val KEYCODE_SETTINGS = -105
        const val KEYCODE_URL = -106
        const val KEYCODE_ARROW_LEFT = -107
        const val KEYCODE_ARROW_RIGHT = -108
        const val KEYCODE_ARROW_UP = -109
        const val KEYCODE_ARROW_DOWN = -110
        const val KEYCODE_ARROW_HOME = -111
        const val KEYCODE_ARROW_END = -112
        const val KEYCODE_LOCALE = -113
        const val KEYCODE_SYM_MENU = -114
        const val KEYCODE_CUT = -115
        const val KEYCODE_COPY = -116
        const val KEYCODE_PASTE = -117
        const val KEYCODE_SELECT = -118
        const val KEYCODE_SELECT_ALL = -119
        const val KEYCODE_ARROW_BACK = -120
        const val KEYCODE_ARROW_NEXT = -121
        const val KEYCODE_DEL_FORWARD = -122
        const val KEY_ACTION_PREV = -1001
        const val KEYCODE_MODE_CHANGE = -2
        const val EDGE_BOTTOM = 0x08
        private const val TAG_ROW = "Row"
        private const val TAG_KEY = "Key"
        private const val TAG_GAP = "Gap"
        fun updateKeyBounds(key: Key) {
            if (key.icon != null) key.icon.setBounds(
                0, 0, key.icon.intrinsicWidth,
                key.icon.intrinsicHeight
            )
            if (key.iconPreview != null) key.iconPreview.setBounds(
                0, 0,
                key.iconPreview.intrinsicWidth,
                key.iconPreview.intrinsicHeight
            )
        }

        // <DEBUG>
        //	private static int mErrorSent = 0; // Send error report max 3 times, for performance reasons.
        fun checkForNulls(location: String?, key: Key?) {
            //		if(key.label == null && key.icon == null && mErrorSent++ < 3) {
            //			// Send error report
            //			String theme = "null_theme";
            //			if(KeyboardThemeManager.getCurrentTheme() != null)
            //				theme = KeyboardThemeManager.getCurrentTheme().getName();
            //			String details = "location=" + location + ",key.label=" + key.label + ",key.codes=" + Arrays.toString(key.codes) + ",theme=" + theme + ",keyboard=" + KeyboardLayout.getLayoutId();
            //			ErrorReport.reportShortError(KeyboardApp.getKeyboardApp(), "null_key", details);
            //		}
        } // </DEBUG>
    }
}
