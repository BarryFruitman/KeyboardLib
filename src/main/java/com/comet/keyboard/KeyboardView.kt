package com.comet.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.comet.keyboard.layouts.KeyboardLayout
import com.comet.keyboard.settings.LongPressDurationSetting
import com.comet.keyboard.settings.Settings
import com.comet.keyboard.settings.SwipeSensitivitySetting
import com.comet.keyboard.settings.WallpaperPhoto
import com.comet.keyboard.theme.KeyboardTheme
import com.comet.keyboard.theme.KeyboardThemeManager
import com.comet.keyboard.util.ErrorReport
import com.comet.keyboard.util.Utils
import junit.framework.Assert
import java.util.Arrays
import java.util.Locale

@Suppress("DEPRECATION")
open class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = R.attr.keyboardViewStyle
) : View(context, attrs, defStyle) {
    /**
     * Listener for virtual keyboard events.
     */
    interface OnKeyboardActionListener {
        /**
         * Called when the user presses a key. This is sent before the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
         * key, the value will be zero.
         */
        fun onPress(primaryCode: Int)

        /**
         * Called when the user releases a key. This is sent after the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the code of the key that was released
         */
        fun onRelease(primaryCode: Int)

        /**
         * Send a key press to the listener.
         * @param primaryCode this is the key that was pressed
         * @param keyCodes the codes for all the possible alternative keys
         * with the primary code being the first. If the primary key code is
         * a single character such as an alphabet or number or symbol, the alternatives
         * will include other characters that may be on the same key or adjacent keys.
         * These codes are useful to correct for accidental presses of a key adjacent to
         * the intended key.
         */
        fun onKey(primaryCode: Int, keyCodes: IntArray?)

        /**
         * Sends a sequence of characters to the listener.
         * @param text the sequence of characters to be displayed.
         */
        fun onText(text: CharSequence?)

        /**
         * Called when the user quickly moves the finger from right to left.
         */
        fun swipeLeft()

        /**
         * Called when the user quickly moves the finger from left to right.
         */
        fun swipeRight()

        /**
         * Called when the user quickly moves the finger from up to down.
         */
        fun swipeDown()

        /**
         * Called when the user quickly moves the finger from down to up.
         */
        fun swipeUp()
    }

    private var mKeyboard: BaseKeyboard? = null
    protected var mCurrentKeyIndex = NOT_A_KEY
    private var mKeyTextSize = 0
    private val mWindowOffsetCoordinates = IntArray(2)
    private val mScreenOffsetCoordinates = IntArray(2)
    private var mPreviewText: TextView? = null
    private val mPreviewPopup: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewOffset = 0
    private var mPreviewHeight = 0
    private var mPopupParent: View
    private var mPopupKeyboardOffsetX = 0
    private var mPopupKeyboardOffsetY = 0
    private var mKeys: Array<Keyboard.Key> = emptyArray()
    /**
     * Returns the [OnKeyboardActionListener] object.
     * @return the listener attached to this keyboard
     */
    /** Listener for [OnKeyboardActionListener].  */
    protected var onKeyboardActionListener: OnKeyboardActionListener? = null
        set
    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0
    private val mPreviewCentered = false
    var isPreviewEnabled = true
    private var mPopupPreviewX = 0
    private var mPopupPreviewY = 0
    private var mPopupPreviewWidthPrev = 0
    private var mPopupPreviewHeightPrev = 0
    private var mLastX = 0
    private var mLastY = 0
    private var isProximityCorrectionEnabled = false
    private val mPaint: Paint
    private var mDownTime: Long = 0
    private var mLastMoveTime: Long = 0
    private var mLastKey = 0
    private var mLastCodeX = 0
    private var mLastCodeY = 0
    private var mCurrentKey = NOT_A_KEY
    private var mLastKeyTime: Long = 0
    private var mCurrentKeyTime: Long = 0
    private val mKeyIndices = IntArray(12)
    private var mGestureDetector: GestureDetector? = null
    private var mRepeatKeyIndex = NOT_A_KEY
    private var mPopupLayout = 0
    private var mInvalidatedKey: Keyboard.Key? = null
    private var mPossiblePoly = false
    private val mSwipeTracker = SwipeTracker()

    // Variables for dealing with multiple pointers
    private var mOldPointerCount = 1
    private var mOldPointerX = 0f
    private var mOldPointerY = 0f
    private val mDistances = IntArray(MAX_NEARBY_KEYS)
    private var mSwipeThreshold = 0

    // For multi-tap
    private var mLastSentIndex = 0
    private var mTapCount = 0
    private var mLastTapTime: Long = 0
    private var mInMultiTap = false
    private val mPreviewLabel = StringBuilder(1)

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted.  */
    private var mDrawPending = false

    /** The dirty region in the keyboard bitmap  */
    private val mDirtyRect = Rect()

    /** The keyboard bitmap for faster updates  */
    private var mBuffer: Bitmap? = null

    /** The canvas for the above mutable keyboard bitmap  */
    private var mCanvas: Canvas? = null
    protected var mPhotoBackground: Drawable? = null

    /*
	 * Key states
	 */
    // any key action for simple press
    var anyKeyAction = getContext().getString(R.string.any_key_action_id_default)
        private set

    // any key action for long press
    private var mAnyKeyLongAction: String? = null

    // return key action for long press
    private var mReturnKeyLongAction: String? = null
    private var mLongPress = false
    private var mIME: KeyboardService? = null
    @JvmField
    var mPopupKeyboardWindow: PopupWindow? = null
    var mPopupKeyboardRect: Rect? = null
    private var mPopupKeyboardLayout: PopupKeyboardLayout? = null
    private var mPopupKeyboardView: PopupKeyboardView? = null
    private var mSymMenuWindow: PopupWindow? = null
    private var mSymMenuLayout: SymMenuLayout? = null
    private var mSymQuickMenuWindow: PopupWindow? = null
    private var mTranslatorWindow: PopupWindow? = null
    @JvmField
    var mTranslatorView: TranslatorView? = null
    private var mMainMenuView: FrameLayout? = null
    private var mMainMenuWindow: PopupWindow? = null

    /**
     * Get the currently displayed PopupWindow.
     *
     * @return    The current PopupWindow.
     */
    var lastPopupWindow: PopupWindow? = null
        private set
    var capsLock = false
    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SHOW_PREVIEW -> showPreview(msg.arg1)
                MSG_REMOVE_PREVIEW -> mPreviewText!!.visibility = INVISIBLE
                MSG_REPEAT -> if (repeatKey()) {
                    val repeat = Message.obtain(this, MSG_REPEAT)
                    sendMessageDelayed(repeat, REPEAT_INTERVAL.toLong())
                }

                MSG_LONGPRESS -> openPopupIfRequired()
            }
        }
    }

    /**
     * Called by constructors to initialize the object.
     *
     * @param context    A context (a KeyboardService).
     */
    private fun init(context: Context) {
        mIME = context as KeyboardService
        createPopups(mIME)
        loadWallpaper(context)
    }

    /**
     * Create the various popup windows used by a primary keyboards.
     *
     * @param context    An instance of KeyboardService.
     */
    private fun createPopups(context: KeyboardService?) {
        mPopupKeyboardWindow = PopupWindow(context)
        mPopupKeyboardWindow!!.setBackgroundDrawable(null)
        mSymMenuWindow = PopupWindow(context)
        mSymMenuWindow!!.setBackgroundDrawable(null)
        mSymQuickMenuWindow = PopupWindow(context)
        mSymQuickMenuWindow!!.setBackgroundDrawable(null)
        mTranslatorWindow = PopupWindow(context)
        mTranslatorWindow!!.setBackgroundDrawable(null)
        mMainMenuWindow = PopupWindow(context)
        mMainMenuWindow!!.setBackgroundDrawable(null)
    }

    /**
     * Sets swipe threshold
     * @param val
     */
    fun setSwipeThreshold(`val`: Int) {
        mSwipeThreshold = (`val` * resources.displayMetrics.density).toInt()
    }

    private fun initGestureDetector() {
        mGestureDetector = SwipeGestureDetector(context, object : SimpleOnGestureListener() {
            override fun onScroll(
                me1: MotionEvent?, me2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                if (me1 == null) return false
                if (mPossiblePoly) return false
                val deltaX = me2.x - me1.x
                val deltaY = me2.y - me1.y
                val travelX = width / 3 // One-third the keyboard width
                val travelY = height / 3 // One-third the keyboard height
                mSwipeTracker.computeCurrentVelocity(1000)
                val velocityX: Float = mSwipeTracker.xVelocity
                val velocityY: Float = mSwipeTracker.yVelocity
                if (velocityX > mSwipeThreshold && deltaX > travelX) {
                    swipeRight()
                    return true
                } else if (velocityX < -mSwipeThreshold && deltaX < -travelX) {
                    swipeLeft()
                    return true
                } else if (velocityY < -mSwipeThreshold && deltaY < -travelY) {
                    swipeUp()
                    return true
                } else if (velocityY > mSwipeThreshold && deltaY > travelY) {
                    swipeDown()
                    return true
                }
                return false
            }
        }).apply {
            setIsLongpressEnabled(false)
        }
    }

    private inner class SwipeGestureDetector(context: Context?, listener: OnGestureListener?) :
        GestureDetector(context, listener!!) {
        private var mFlinging = false
        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (mFlinging) {
                if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                    mFlinging = false
                }
                return true
            }

            // This method will return true if the user trigged a scroll
            mFlinging = super.onTouchEvent(ev)
            return mFlinging
        }
    }

    var keyboard: BaseKeyboard?
        /**
         * Returns the current keyboard being displayed by this view.
         * @return the currently attached keyboard
         * @see .setKeyboard
         */
        get() = mKeyboard
        /**
         * Attaches a keyboard to this view. The keyboard can be switched at any time and the
         * view will re-layout itself to accommodate the keyboard.
         * @see Keyboard
         *
         * @see .getKeyboard
         * @param keyboard the keyboard to display in this view
         */
        set(keyboard) {
            if (mKeyboard != null) {
                updatePreview(NOT_A_KEY)
            }
            // Remove any pending messages
            removeMessages()
            mKeyboard = keyboard
            val keys = mKeyboard!!.keys
            mKeys = keys.toTypedArray<Keyboard.Key>()
            requestLayout()
            // Hint to reallocate the buffer if the size changed
            invalidateAllKeys()
            computeProximityThreshold(keyboard)
            updateAnyKeySym()
        }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView.isShifted
     */
    fun setShifted(shifted: Boolean): Boolean {
        if (mKeyboard != null) {
            if (mKeyboard!!.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys()
                return true
            }
        }
        setCapsLockIcon(false)
        return isShifted
    }

    val isShifted: Boolean
        /**
         * Returns the state of the shift key of the keyboard, if any.
         * @return true if the shift is in a pressed state, false otherwise. If there is
         * no shift key on the keyboard or there is no keyboard attached, it returns false.
         * @see KeyboardView.setShifted
         */
        get() = if (mKeyboard != null) {
            mKeyboard!!.isShifted
        } else false

    fun setPopupParent(v: View) {
        mPopupParent = v
    }

    fun setPopupOffset(x: Int, y: Int) {
        mPopupKeyboardOffsetX = x
        mPopupKeyboardOffsetY = y
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
    }

    private fun adjustCase(label: CharSequence): CharSequence {
        return if (mKeyboard!!.isShifted && label.length < 3 && Character.isLowerCase(label[0])) {
            label.toString().uppercase(Locale.getDefault())
        } else {
            label
        }
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(paddingLeft + paddingRight, paddingTop + paddingBottom)
        } else {
            var width = mKeyboard!!.minWidth + paddingLeft + paddingRight
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec)
            }
            setMeasuredDimension(width, mKeyboard!!.height + paddingTop + paddingBottom)
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: Keyboard?) {
        if (keyboard == null) return
        val keys = mKeys
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += Math.min(key.width, key.height) + key.gap
        }
        if (dimensionSum < 0 || length == 0) return
        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold // Square it
    }

    /**
     * Called by the framework when the view size changes. Resizes the popup
     * windows to match.
     */
    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        currentFullPopupWindow?.update(w, totalHeight)

//		mBuffer = null;
    }

    /**
     * Sets any key action for short and long press
     *
     * @param anyKeyAction        simple click
     * @param anyKeyLongAction    long click
     */
    fun setAnyKeyAction(
        anyKeyAction: String,
        anyKeyLongAction: String?,
        returnKeyLongAction: String?
    ) {
        this.anyKeyAction = anyKeyAction
        mAnyKeyLongAction = anyKeyLongAction
        mReturnKeyLongAction = returnKeyLongAction
        updateAnyKeySym()
    }

    /**
     * Set shift key state based on shift and caps lock states
     *
     * @param caps
     * @param capsLock
     * @return
     */
    fun setShifted(caps: Boolean, capsLock: Boolean): Boolean {
        setShifted(caps || capsLock) // Call the superclass to avoid a
        // recusive loop
        setCapsLockIcon(capsLock)
        return isShifted
    }

    /**
     * Set the icon on the shift key to either caps-lock or not
     *
     * @param capsLock    Set true for caps lock.
     * @return            Returns the new shift state.
     */
    protected fun setCapsLockIcon(capsLock: Boolean): Boolean {
        return setShiftKeyIcon(capsLock, true)
    }

    /**
     * Update the shift key icon.
     *
     * @param capsLock    Set true if caps lock is enabled.
     * @param redraw    Set true to force a redraw.
     * @return            Returns the shift key state.
     */
    protected fun setShiftKeyIcon(capsLock: Boolean, redraw: Boolean): Boolean {
        // Find our custom shift key
        val keyboard = keyboard
        val iShiftKey = keyboard!!.shiftKeyIndex
        if (iShiftKey != -1) {
            val shiftKey = keyboard.keys[iShiftKey]

            // Update shift key icon
            if (capsLock == true) {
                shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_SHIFT_LOCKED)
                shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT_LOCKED)
            } else if (keyboard.isShifted) {
                shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_SHIFT_ON)
                shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT)
            } else {
                shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_SHIFT_OFF)
                shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT)
            }
            BaseKeyboard.checkForNulls("setShiftKeyIcon()", shiftKey)
            BaseKeyboard.updateKeyBounds(shiftKey)
            if (redraw) {
                // Redraw the shift key
                invalidateKey(iShiftKey)
            }
        }
        return isShifted
    }
    /*
	 * Methods to open the various PopupWindows
	 */
    /**
     * Opens the symbols quick menu. The quick menu is a one-touch menu with the
     * most common symbols, and a key to open the main symbols menu.
     *
     * @param key    The key that was pressed to open this menu.
     */
    protected fun openSymQuickMenu(key: Keyboard.Key) {
        if (isPopupShowing) dismissAllPopupWindows()

        // Create a new keyboard
        val keyboard = PopupKeyboard(context, R.xml.sym_quick_menu)
        mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, key)
        mPopupKeyboardView!!.isOneTouch = true
        showPopupWindow(
            mPopupKeyboardWindow,
            this,
            Gravity.NO_GRAVITY,
            mPopupKeyboardRect!!.left,
            mPopupKeyboardRect!!.top
        )
    }

    fun openSymMenu() {
        if (isPopupShowing) dismissAllPopupWindows()
        if (mSymMenuLayout == null) {
            // Inflate the translator view
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            mSymMenuLayout = layoutInflater.inflate(R.layout.sym_menu_layout, null) as SymMenuLayout
            mSymMenuLayout!!.createKeyboards(this, onKeyboardActionListener)
            mSymMenuWindow!!.contentView = mSymMenuLayout
        }
        mSymMenuWindow!!.width = width
        mSymMenuWindow!!.height = totalHeight
        showPopupWindow(mSymMenuWindow, this, Gravity.BOTTOM, 0, 0)
    }

    fun openTranslator() {
        if (isPopupShowing) dismissAllPopupWindows()
        if (mTranslatorView == null) {
            // Inflate the translator view
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            mTranslatorView = layoutInflater.inflate(R.layout.translator, null) as TranslatorView
            mTranslatorView!!.onCreate()
            mTranslatorWindow!!.contentView = mTranslatorView
        }
        mTranslatorWindow!!.width = width
        mTranslatorWindow!!.height = totalHeight
        showPopupWindow(mTranslatorWindow, this, Gravity.BOTTOM, 0, 0)
    }

    /**
     * Creates and displays the popup keyboard. Called by onLongPress() when the
     * user long-presses a key with more than one alternate character.
     */
    fun openMainMenu() {
        if (isPopupShowing) dismissAllPopupWindows()
        if (mMainMenuView == null) {
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            mMainMenuView = layoutInflater.inflate(R.layout.main_menu, null) as FrameLayout
            mMainMenuWindow!!.contentView = mMainMenuView
        }
        mMainMenuWindow!!.width = width
        mMainMenuWindow!!.height = totalHeight
        showPopupWindow(mMainMenuWindow, this, Gravity.BOTTOM, 0, 0)
        invalidateAllKeys()
    }
    /*
	 * Methods to open the various popup keyboards
	 */
    /**
     * Opens the emoji one-touch menu above the emoji key.
     */
    fun openEmojiKeyboard() {
        if (isPopupShowing) dismissAllPopupWindows()

        // Create a new keyboard
        val keyboard = PopupKeyboard(context, R.xml.emoji_menu)
        mPopupKeyboardRect = assemblePopupKeyboard(
            R.layout.popup_keyboard_layout,
            keyboard,
            this.keyboard!!.emojiKey
        )
        mPopupKeyboardView!!.isOneTouch = true
        showPopupWindow(
            mPopupKeyboardWindow,
            this,
            Gravity.NO_GRAVITY,
            mPopupKeyboardRect!!.left,
            mPopupKeyboardRect!!.top
        )
    }

    fun openUrlKeyboard() {
        if (isPopupShowing) dismissAllPopupWindows()

        // Create a new keyboard
        val keyboard = PopupKeyboard(context, R.xml.url_menu)
        mPopupKeyboardRect =
            assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, this.keyboard!!.anyKey)
        mPopupKeyboardView!!.isOneTouch = true
        mPopupKeyboardView!!.setNearestKey(-201)
        showPopupWindow(
            mPopupKeyboardWindow,
            this,
            Gravity.NO_GRAVITY,
            mPopupKeyboardRect!!.left,
            mPopupKeyboardRect!!.top
        )
        invalidateAllKeys()
    }
    /*
	 * PopupWindow methods for menus and keyboards
	 */
    /**
     * Show a popup window.
     *
     * @param popWin    The PopupWindow to show.
     * @param parent    The view to assign as popWin's parent.
     * @param gravity    The gravity to apply to popWin.
     * @param x            The x location for popWin.
     * @param y            The y location for popWin.
     */
    fun showPopupWindow(popWin: PopupWindow?, parent: View?, gravity: Int, x: Int, y: Int) {
        popWin!!.showAtLocation(parent, gravity, x, y)
        lastPopupWindow = popWin
    }

    /**
     * Opens a popup keyboard that contains only characters.
     *
     * @param key    The key that triggered the keyboard
     */
    // Convenience method
    protected fun openPopupCharacterKeyboard(key: Keyboard.Key, oneTouch: Boolean = true) {
        // Create a new keyboard
        val popupCharacters = key.popupCharacters
        var keyboard = PopupKeyboard(
            context,
            key.popupResId, popupCharacters, -1, paddingLeft
                + paddingRight
        )

        // Assemble the keyboard view
        mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, key)

        // Check if popup keyboard fits above-right key
        if (mPopupKeyboardRect!!.right <= key.x) {
            // Popup is located to left of key. Reverse the key order.
            val reversePopupCharacters =
                StringBuilder(popupCharacters.subSequence(0, popupCharacters.length))
            reversePopupCharacters.reverse()
            keyboard = PopupKeyboard(
                context,
                key.popupResId,
                reversePopupCharacters,
                -1,
                paddingLeft + paddingRight
            )
            mPopupKeyboardView!!.keyboard = keyboard
        }

        // Set default key. This key is highlighted first.
        mPopupKeyboardView!!.isOneTouch = oneTouch
        mPopupKeyboardView!!.setNearestKey(popupCharacters[0].code)

        // Show the keyboard
        showPopupWindow(
            mPopupKeyboardWindow,
            this,
            Gravity.NO_GRAVITY,
            mPopupKeyboardRect!!.left,
            mPopupKeyboardRect!!.top
        )
    }

    /**
     * Fills a PopupWindow with a keyboard, positions it relative to a key, and
     * returns the resulting bounds.
     *
     * @param layoutViewId    The id of view that holds the keyboard.
     * @param keyboard        The keyboard to pop.
     * @param key            The key that triggered the keyboard.
     * @return                A Rect containing the bounds of the PopupWindow.
     */
    protected fun assemblePopupKeyboard(
        layoutViewId: Int,
        keyboard: BaseKeyboard?,
        key: Keyboard.Key
    ): Rect {
        if (mPopupKeyboardWindow!!.isShowing) dismissAllPopupWindows()

        // Inflate the popup keyboard container
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mPopupKeyboardLayout = layoutInflater.inflate(layoutViewId, null) as PopupKeyboardLayout
        mPopupKeyboardView = mPopupKeyboardLayout!!.keyboardView as PopupKeyboardView

        // Assign the keyboard
        mPopupKeyboardView!!.keyboard = keyboard
        mPopupKeyboardView!!.setPopupParent(this)
        mPopupKeyboardView!!.onKeyboardActionListener = onKeyboardActionListener
        mPopupKeyboardView!!.setShifted(isShifted)

        // Fill in the popup keyboard window
        var popupRect = assemblePopupWindow(mPopupKeyboardWindow, mPopupKeyboardLayout)
        popupRect = positionWindowAboveKey(popupRect, key)
        mPopupKeyboardView!!.setPopupOffset(0, mIME!!.candidateHeight + popupRect.top)

        // Return the bounds of the window
        return popupRect
    }

    /**
     * Fills a PopupWindow with content, resizes it to fit and returns the
     * resulting bounds.
     *
     * @param window        The PopupWindow to fill.
     * @param contentView    The content View to fill it with.
     * @return                A Rect containing the bounds of the PopupWindow.
     */
    protected fun assemblePopupWindow(window: PopupWindow?, contentView: View?): Rect {
        // Size and lay out all views
        contentView!!.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
        )
        val layoutMeasuredWidth = contentView.measuredWidth
        val layoutMeasuredHeight = contentView.measuredHeight
        // Display popup just above and to the right of the key
        window!!.contentView = contentView
        window.width = layoutMeasuredWidth
        window.height = layoutMeasuredHeight

        // Return the bounds of mPopupKeyboardWindow
        return Rect(0, 0, layoutMeasuredWidth, layoutMeasuredHeight)
    }

    /**
     * Finds the best place to position a popup keyboard above a key.
     *
     * @param windowRect    The initial bounds of the PopupWindow.
     * @param key            The key to position it above.
     * @return                The new bounds of the PopupWindow.
     */
    private fun positionWindowAboveKey(windowRect: Rect, key: Keyboard.Key): Rect {
        val newRect = Rect()
        val width = windowRect.right - windowRect.left
        val height = windowRect.bottom - windowRect.top

        // Convert key coordinates from relative to absolute
        val windowOffset = IntArray(2)
        getLocationInWindow(windowOffset)
        val x = key.x + windowOffset[0]
        val y = key.y + windowOffset[1]

        // Position window just above key
        newRect.bottom = y - key.height / 4
        newRect.top = newRect.bottom - height

        // Choose horizontal position
        val displayWidth =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.width
        if (width < displayWidth - x) {
            // Position window to right of key
            newRect.left = x
            newRect.right = newRect.left + width
        } else {
            // Position window to left of key
            newRect.right = x
            newRect.left = newRect.right - width
            if (newRect.left < 0) {
                // Window doesn't fit on left either. Left align it with screen instead.
                newRect.right -= newRect.left
                newRect.left = 0
            }
        }
        return newRect
    }

    val isPopupShowing: Boolean
        /**
         * Checks if any PopupWindows are showing.
         *
         * @return    true if any PopupWindow is showing.
         */
        get() = (mPopupKeyboardWindow!!.isShowing
            || mSymMenuWindow!!.isShowing
            || mSymQuickMenuWindow!!.isShowing
            || mTranslatorWindow!!.isShowing
            || mMainMenuWindow!!.isShowing)
    private val currentFullPopupWindow: PopupWindow?
        /**
         * Gets the currently showing PopupWindow. Assumes only one popup is visible
         * at a time.
         *
         * @return    The currently showing PopupWindow.
         */
        get() {
            if (mTranslatorWindow!!.isShowing) return mTranslatorWindow
            return if (mMainMenuWindow!!.isShowing) mMainMenuWindow else null
        }

    /**
     * Close all open popup windows
     */
    protected fun dismissAllPopupWindows() {
        dismissAllPopupWindows(false)
    }

    fun dismissAllPopupWindows(rememberLastWindow: Boolean) {
        if (mPopupKeyboardWindow!!.isShowing) mPopupKeyboardWindow!!.dismiss()
        if (mSymMenuWindow!!.isShowing) mSymMenuWindow!!.dismiss()
        if (mSymQuickMenuWindow!!.isShowing) mSymQuickMenuWindow!!.dismiss()
        if (mTranslatorWindow!!.isShowing) mTranslatorWindow!!.dismiss()
        if (mMainMenuWindow!!.isShowing) mMainMenuWindow!!.dismiss()
        if (!rememberLastWindow) lastPopupWindow = null
    }

    /**
     * Gets any key code
     *
     * @param state
     * @return
     */
    private fun getFnKeyCode(state: String?): Int {
        return when (state) {
            context.getString(R.string.any_key_action_id_emoji_menu), context.getString(
                R.string.any_key_action_id_smiley_menu_deprecated) -> {
                BaseKeyboard.KEYCODE_EMOJI
            }
            context.getString(R.string.any_key_action_id_arrow_keypad) -> {
                BaseKeyboard.KEYCODE_ARROWS
            }
            context.getString(R.string.any_key_action_id_voice_input
            ) -> {
                BaseKeyboard.KEYCODE_VOICE
            }
            context.getString(R.string.any_key_action_id_translator) -> {
                BaseKeyboard.KEYCODE_TRANSLATE
            }
            context.getString(R.string.any_key_action_id_locale) -> {
                BaseKeyboard.KEYCODE_LOCALE
            }
            context.getString(R.string.any_key_action_id_settings) -> {
                BaseKeyboard.KEYCODE_SETTINGS
            }
            else -> {
                throw IllegalArgumentException("I don't recognize state '$state'")
            }
        }
    }
    /*
	 * Keyboard drawing methods. Many of these methods override KeyboardView
	 * because we use a highly customized keyboard look. As a result some
	 * methods are copied exactly from Android source.
	 */
    /**
     * Called by the framework to re-draw the keyboard.
     */
    public override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        if (mDrawPending || mBuffer == null) {
            if (mBuffer == null || mBuffer!!.width != width || mBuffer!!.height != height) {
                val width = width
                val height = height
                if (width <= 0 || height <= 0) // KeyboardView is not ready
                    return

                // Create a new buffer if anything changed
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBuffer!!)
                setPhotoWallpaperBounds()
                invalidateAllKeys()
            }
            drawKeyboard(mCanvas)
            mDirtyRect.setEmpty()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    /**
     * Draws the keyboard to a buffer.
     */
    protected fun drawKeyboard(canvas: Canvas?) {
        if (keyboard == null) return  // Oops!

        // Set default text alignment
        mPaint.textAlign = Align.CENTER

        // Set clip region and padding
        val clipRegion = Rect(0, 0, 0, 0)
        val kbPadding = Rect(
            paddingLeft, paddingTop,
            paddingRight, paddingBottom
        )
        val keys: Array<Any> = keyboard!!.keys.toTypedArray()
        val invalidKey = mInvalidatedKey
        var drawSingleKey = false
        if (invalidKey != null && canvas!!.getClipBounds(clipRegion)) {
            // Is clipRegion completely contained within the invalidated key?
            if (invalidKey.x + kbPadding.left - 1 <= clipRegion.left && invalidKey.y + kbPadding.top - 1 <= clipRegion.top && invalidKey.x + invalidKey.width + kbPadding.left + 1 >= clipRegion.right && invalidKey.y + invalidKey.height + kbPadding.top + 1 >= clipRegion.bottom) {
                drawSingleKey = true
            }
        }

        // Draw the keyboard background
        drawBackground(canvas)

        // Draw the keys
        val keyCount = keys.size
        val keyPadding = Rect()
        for (i in 0 until keyCount) {
            val key = keys[i] as Keyboard.Key
            if (drawSingleKey && invalidKey !== key) {
                continue
            }

            // Draw the key background
            drawKeyBackground(key, canvas, kbPadding, keyPadding)

            // Draw superscript label
            if (mIME!!.isDrawSuperLabel(key)) drawSuperLabel(key, keyPadding, canvas)
            if (key.label != null) {
                // Draw a label
                drawLabel(key, canvas, kbPadding, keyPadding)
            } else {
                updateKeyIcon(key)
                // Paint an icon
                if (key.icon != null) {
                    drawIcon(key, canvas, kbPadding, keyPadding)
                    if (key === keyboard!!.anyKey && mAnyKeyLongAction != null) {
                        // Draw super icon for any key long press action
                        val icon = getAnyKeyIcon(mAnyKeyLongAction!!, true)
                        if (!mIME!!.mURL && icon != null) drawSuperIcon(
                            key,
                            canvas,
                            kbPadding,
                            keyPadding,
                            icon
                        )
                    }
                }
            }
            if (key === keyboard!!.actionKey && mReturnKeyLongAction != null) {
                // Draw super icon for return key long press action
                val icon = getAnyKeyIcon(mReturnKeyLongAction!!, true)
                icon?.let { drawSuperIcon(key, canvas, kbPadding, keyPadding, it) }
            }
        }
        mInvalidatedKey = null
        /*
		 * // Overlay a dark rectangle to dim the keyboard if
		 * (mPopupKeyboardWindow.isShowing()) { paint.setColor((int)
		 * (mBackgroundDimAmount * 0xFF) << 24); canvas.drawRect(0, 0,
		 * getWidth(), getHeight(), paint); }
		 */mDrawPending = false
    }

    /**
     * Get icon for short or long state action
     *
     * @param state
     * @param superEnabled
     * @return
     */
    private fun getAnyKeyIcon(state: String, superEnabled: Boolean): Drawable? {
        val theme = KeyboardThemeManager.getCurrentTheme()

        return when (state) {
            context.getString(R.string.any_key_action_id_emoji_menu), context.getString(R.string.any_key_action_id_smiley_menu_deprecated) -> {
                // Emoji
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_EMOJI else KeyboardTheme.KEY_SYM_EMOJI)
            }
            context.getString(R.string.any_key_action_id_arrow_keypad) -> {
                // Arrow keypad
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_ARROWS else KeyboardTheme.KEY_SYM_ARROWS)
            }
            context.getString(R.string.any_key_action_id_voice_input) -> {
                // Voice input
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_MIC else KeyboardTheme.KEY_SYM_MIC)
            }
            context.getString(R.string.any_key_action_id_translator) -> {
                // Translator
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_TRANSLATE else KeyboardTheme.KEY_SYM_TRANSLATE)
            }
            context.getString(R.string.any_key_action_id_locale) -> {
                // Language
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_LOCALE else KeyboardTheme.KEY_SYM_LOCALE)
            }
            context.getString(R.string.any_key_action_id_settings) -> {
                // Settings
                theme.getDrawable(if (superEnabled) KeyboardTheme.KEY_SYM_SUPER_SETTINGS else KeyboardTheme.KEY_SYM_SETTINGS)
            }

            else -> null
        }
    }

    /**
     * Gets any super icon
     *
     * @param state
     * @return
     */
    protected fun getAnyKeySuperIcon(state: String): Drawable? {
        return getAnyKeyIcon(state, true)
    }

    /*
	 * Keyboard background drawing methods.
	 */

    /**
     * Draw the keyboard background. This could be a photo drawable or a theme
     * background (drawable or a solid color).
     *
     * @param canvas    The canvas to draw on.
     */
    protected open fun drawBackground(canvas: Canvas?) {
        if (mPhotoBackground == null) {
            val background = KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KB_BG)
            if (background != null) {
                // Theme backgrounds are always cropped
                mBackgroundFit = false
                val bounds = getWallpaperBounds(background)
                background.bounds = bounds
                background.draw(canvas!!)
            } else  // No background image. Use solid color.
                canvas!!.drawColor(KeyboardThemeManager.getCurrentTheme().bgColor)
        } else {
            mPhotoBackground!!.draw(canvas!!)
        }
    }

    /**
     * Set the bounds of a photo wallpaper.
     */
    protected open fun setPhotoWallpaperBounds() {
        if (mPhotoBackground != null) mPhotoBackground!!.bounds = photoWallpaperBounds
    }

    protected val photoWallpaperBounds: Rect
        get() = getWallpaperBounds(mPhotoBackground)

    /**
     * Compute the bounds rect for the photo wallpaper, based on keyboard size
     * and stretch/fit flag. If fit == true, the photo is resized to fix
     * exactly, which will change the aspect. If not, the photo will be resized
     * in only one dimension, and cropped. This is usually the preferred way.
     *
     * @return    A Rect containing the bounds of the wallpaper drawable
     */
    private val mWallpaperRect = Rect()
    protected fun getWallpaperBounds(wallpaper: Drawable?): Rect {
        if (mBackgroundFit) {
            mWallpaperRect.left = 0
            mWallpaperRect.top = 0
            mWallpaperRect.right = width
            mWallpaperRect.bottom = height
            return mWallpaperRect
        }

        // Get the various dimensions and ratios.
        val kbWidth = width.toFloat()
        val kbHeight = height.toFloat()
        val kbRatio = kbWidth / kbHeight
        val wpRatio = (wallpaper!!.intrinsicWidth.toFloat()
            / wallpaper.intrinsicHeight.toFloat())
        if (wpRatio > kbRatio) {
            // Background has a wider aspect ratio than content area. crop to
            // width, fit to height.
            val sizeRatio = (kbHeight
                / wallpaper.intrinsicHeight.toFloat())
            val wpWidth = (wallpaper.intrinsicWidth
                * sizeRatio)
            mWallpaperRect.left = ((kbWidth - wpWidth) / 2).toInt()
            mWallpaperRect.top = 0
            mWallpaperRect.right = (kbWidth + (wpWidth - kbWidth) / 2).toInt()
            mWallpaperRect.bottom = kbHeight.toInt()
        } else {
            // Background has a narrower aspect ratio than content area. Crop to
            // height, fit to
            // width.
            val sizeRatio = (kbWidth
                / wallpaper.intrinsicWidth.toFloat())
            var wpHeight = wallpaper.intrinsicHeight.toFloat()
            wpHeight = wpHeight * sizeRatio
            mWallpaperRect.left = 0
            mWallpaperRect.top = ((kbHeight - wpHeight) / 2).toInt()
            mWallpaperRect.right = kbWidth.toInt()
            mWallpaperRect.bottom = (kbHeight + (wpHeight - kbHeight) / 2).toInt()
        }
        return mWallpaperRect
    }
    /*
	 * Key background drawing methods.
	 */
    /**
     * Draw the background on a key
     */
    private val mKeyBounds = Rect()
    private fun drawKeyBackground(
        key: Keyboard.Key,
        canvas: Canvas?,
        kbPadding: Rect,
        keyPadding: Rect?
    ) {
        val background = getKeyBackground(key)
        val drawableState = key.currentDrawableState
        background!!.setState(drawableState)
        background.getPadding(keyPadding!!)
        mKeyBounds.left = kbPadding.left + key.x
        mKeyBounds.top = kbPadding.top + key.y
        mKeyBounds.right = kbPadding.left + key.x + key.width
        mKeyBounds.bottom = kbPadding.top + key.y + key.height
        background.bounds = mKeyBounds
        background.draw(canvas!!)
    }

    /**
     * Get the drawable for a key background.
     *
     * @param key    A key.
     * @return        The background drawable to use for key.
     */
    protected open fun getKeyBackground(key: Keyboard.Key): Drawable? {
        val background: Drawable?
        var details: String
        if (key.codes[0] < 0) {
            // Special background for function keys
            background = getFnKeyBackground(key)
            details = "getFnKeyBackground(key.codes[0] < 0)"
        } else {
            if (KeyboardThemeManager.getCurrentTheme().themeName == "ICS" && (key.codes[0] == 44 || key.codes[0] == 46 || key.codes[0] == 32)) {
                // ICS THEME HACK
                background = getFnKeyBackground(key)
                details = "getFnKeyBackground(ICS)"
            } else {
                // Return the default key background.
                background = KeyboardThemeManager.getCurrentTheme()
                    .getDrawable(KeyboardTheme.KEY_NORMAL)
                details = "getFnKeyBackground()"
            }
        }

        // Send error report
        if (background == null) {
            details += " with key codes " + Arrays.toString(key.codes) + " key == getAnyKey() " + (key === keyboard!!.anyKey)
            ErrorReport.reportShortError(mIME, "getKeyBackground", details)
        }
        Assert.assertTrue(background != null)

        // Set alpha
        if (mPhotoBackground != null) // Use custom alpha
            background!!.alpha = mPhotoKeyBGAlpha else  // Use theme alpha
            background!!.alpha = KeyboardThemeManager.getCurrentTheme().bgAlpha
        return background
    }

    /**
     * Get the drawable for a function key background.
     *
     * @param key
     * A function key.
     * @return        The background drawable to use for key.
     */
    protected fun getFnKeyBackground(key: Keyboard.Key): Drawable {
        return if (key === keyboard!!.anyKey) {
            // The Any Key uses a special background.
            val background =
                KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KEY_ANY)
            background.alpha = mPhotoKeyBGAlpha
            background
        } else  // Return the default function key background.
            KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KEY_FN_NORMAL)
    }
    /*
	 * Key label drawing methods.
	 */
    /**
     * Draws a label on a key.
     *
     * @param key            The key to draw a label on
     * @param canvas        The canvas to paint on
     * @param kbPadding        Keyboard padding
     * @param keyPadding    Key padding
     */
    private fun drawLabel(key: Keyboard.Key, canvas: Canvas?, kbPadding: Rect, keyPadding: Rect) {
        // Switch the character to uppercase if shift is pressed
        val label = getLabel(key)
        var textSize = (resources.getDimension(R.dimen.key_text_size)
            * keyboard!!.keyScale.rowDefault)
        var labelSize = (resources.getDimension(R.dimen.label_text_size)
            * keyboard!!.keyScale.rowDefault)
        if (key.edgeFlags == Keyboard.EDGE_BOTTOM) {
            textSize = (resources.getDimension(R.dimen.key_text_size)
                * keyboard!!.keyScale.rowBottom)
            labelSize = (resources.getDimension(R.dimen.label_text_size)
                * keyboard!!.keyScale.rowBottom)
        }
        val color = KeyboardThemeManager.getCurrentTheme().keyFGColor
        mPaint.color = color
        mPaint.textAlign = Align.CENTER
        // For characters, use large font. For labels like "Done", use small font.
        if (label!!.length > 1 && key.codes.size < 2) {
            mPaint.textSize = labelSize
            mPaint.setTypeface(Typeface.DEFAULT_BOLD)
        } else {
            mPaint.textSize = textSize
            //			mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        // Draw the text
        val labelX = getLabelX(key, kbPadding, keyPadding)
        val labelY = getLabelY(key, kbPadding, keyPadding)
        canvas!!.drawText(label, labelX, labelY, mPaint)
        // Turn off drop shadow
        mPaint.setShadowLayer(0f, 0f, 0f, 0)
    }

    /**
     * Get the label for a key.
     *
     * @param key    A key.
     * @return        The label for key.
     */
    protected fun getLabel(key: Keyboard.Key): String? {
        if (key.label != null && keyboard!!.isShifted && key.codes[0] > 0) return key.label.toString()
            .uppercase(
                Locale.getDefault()
            ) else if (key.label != null) return key.label.toString()
        return null
    }

    /**
     * Get the x-position for a centered key label, relative to the keyboard.
     *
     * @param key            A key.
     * @param kbPadding        The keyboard padding.
     * @param keyPadding    The key padding.
     * @return                The x-position, relative to the keyboard.
     */
    protected fun getLabelX(key: Keyboard.Key, kbPadding: Rect, keyPadding: Rect): Float {
        return (key.x + kbPadding.left + (key.width - keyPadding.left - keyPadding.right) / 2 + keyPadding.left).toFloat()
    }

    protected open fun getLabelY(key: Keyboard.Key, kbPadding: Rect?, keyPadding: Rect): Float {
        return getLabelBottomY(key, keyPadding)
    }

    /**
     * Get the y-position for a centered key label, relative to the keyboard.
     *
     * @param key            A key.
     * @param kbPadding        The keyboard padding.
     * @param keyPadding    The key padding.
     * @return                The x-position, relative to the keyboard.
     */
    protected fun getLabelCenterY(key: Keyboard.Key, kbPadding: Rect, keyPadding: Rect): Float {
        return key.y + kbPadding.top + (key.height - keyPadding.top - keyPadding.bottom) / 2 + (mPaint.textSize - mPaint.descent()) / 2 + keyPadding.top
    }

    private fun getLabelBottomY(key: Keyboard.Key, keyPadding: Rect): Float {
        return key.y + key.height - keyPadding.bottom - mPaint.descent()
    }

    /**
     * Draw a superscript label above the main label on a key.
     *
     * @param key        The key to draw on
     * @param canvas    The canvas to paint on
     */
    private fun drawSuperLabel(key: Keyboard.Key, keyPadding: Rect, canvas: Canvas?) {
        val superLabel = getSuperLabel(key)
        if (superLabel.length == 0) return
        var size = (resources.getDimension(R.dimen.key_super_label_text_size)
            * keyboard!!.keyScale.rowDefault)
        if (key.edgeFlags == Keyboard.EDGE_BOTTOM) size =
            (resources.getDimension(R.dimen.key_super_label_text_size)
                * keyboard!!.keyScale.rowBottom)
        var color = KeyboardThemeManager.getCurrentTheme().keySuperFGColor
        var horizOffset = KeyboardThemeManager.getCurrentTheme().superHorizOffset.toFloat()
        mPaint.setTypeface(Typeface.DEFAULT)

        // Calculate vertical location of label
        var verticalGap = (resources.getDimension(R.dimen.kb_gap_vertical)
            * keyboard!!.keyScale.rowDefault)
        if (key.edgeFlags == Keyboard.EDGE_BOTTOM) {
            verticalGap = (resources.getDimension(R.dimen.kb_gap_vertical)
                * keyboard!!.keyScale.rowBottom)
        }
        val vertOffset = verticalGap + size

        // Calculate horizontal location of label
        if (horizOffset == 0f) {
            // Center
            mPaint.textAlign = Align.CENTER
            horizOffset = (key.x + key.width / 2).toFloat()
        } else if (horizOffset > 0) {
            // Right-align
            mPaint.textAlign = Align.RIGHT
            horizOffset = key.x + key.width - keyPadding.right - horizOffset
        } else if (horizOffset < 0) {
            // Left-align
            mPaint.textAlign = Align.LEFT
            horizOffset = key.x + keyPadding.left - horizOffset
        }
        mPaint.setTypeface(Typeface.DEFAULT_BOLD)
        mPaint.isFakeBoldText = true
        if (KeyboardThemeManager.getCurrentTheme().themeName == "ICS") {
            // ICS THEME HACK
            if (key.codes[0] < 0 || key.codes[0] == 44 || key.codes[0] == 46 || key.codes[0] == 32) color =
                KeyboardTheme.icsFnSuperColor
        }
        mPaint.textSize = size
        mPaint.color = color
        canvas!!.drawText(superLabel, horizOffset, key.y + vertOffset, mPaint)
    }

    /**
     * Returns the superscript label to draw at the top of this key
     *
     * @param key        The key to check
     * @return            The superscript label
     */
    protected fun getSuperLabel(key: Keyboard.Key): String {
        var superLabel = ""
        if (key === keyboard!!.anyKey) {
            if (mIME!!.mURL) // User is editing an URL field.
                superLabel = "URL"
        } else if (key.popupCharacters != null && key.popupCharacters.length > 2 && key.popupCharacters[0] == '{' && key.popupCharacters[key.popupCharacters.length - 1] == '}') {
            // popupCharacters wrapped in {} are actually labels
            superLabel = key.popupCharacters.toString().substring(1, key.popupCharacters.length - 1)
        } else if (key.popupCharacters != null && key.popupCharacters.length > 0 && showSuperChar(
                key.popupCharacters[0]
            )
        ) {
            // Use the first non-letter popup key as the superscript label
            for (iChar in 0 until key.popupCharacters.length) {
                val c = key.popupCharacters[iChar]
                if (!Character.isLetter(c) || c == 'ß') {
                    superLabel = Character.toString(c)
                    break
                }
            }
        }

        // Set shift state
        if (isShifted) superLabel = superLabel.uppercase(Locale.getDefault())
        return superLabel
    }

    /**
     * Checks if a character should be displayed as a super-label.
     *
     * @param c        The character to check.
     * @return        true if c is a valid super-label.
     */
    protected fun showSuperChar(c: Char): Boolean {
        return if ((Character.isLetter(c) || c == 'ß')
            && !KeyboardLayout.getCurrentLayout().showSuperLetters()
        ) false else true
    }
    /*
	 * Key icon drawing methods.
	 */

    private val mIconRect = Rect()

    /**
     * Draw an icon on a key.
     *
     * @param key            The key to draw a label on
     * @param canvas        The canvas to paint on
     * @param kbPadding        Keyboard padding
     * @param keyPadding    Key padding
     */
    private fun drawIcon(key: Keyboard.Key, canvas: Canvas?, kbPadding: Rect, keyPadding: Rect) {
        var size = (resources.getDimension(R.dimen.key_text_size)
            * keyboard!!.keyScale.rowDefault).toInt()
        if (key.edgeFlags == Keyboard.EDGE_BOTTOM) size =
            (resources.getDimension(R.dimen.key_text_size)
                * keyboard!!.keyScale.rowBottom).toInt()
        if (hasSuper(key)) {
            // Bottom align icon to content area.
            mIconRect.left = kbPadding.left + key.x + (key.width - size) / 2
            mIconRect.right = mIconRect.left + size
            mIconRect.bottom = kbPadding.top + key.y + key.height - keyPadding.bottom
            mIconRect.top = mIconRect.bottom - size
        } else {
            // Center icon to key.
            mIconRect.left = kbPadding.left + key.x + (key.width - size) / 2
            mIconRect.right = mIconRect.left + size
            mIconRect.top = kbPadding.top + key.y + (key.height - size) / 2
            mIconRect.bottom = mIconRect.top + size
        }
        key.icon.bounds = mIconRect
        key.icon.draw(canvas!!)
    }

    protected fun hasSuper(key: Keyboard.Key): Boolean {
        // Check for super-label.
        if (getSuperLabel(key).length > 0) return true

        // Any Key as a super icon.
        return if (key === keyboard!!.anyKey || key === keyboard!!.actionKey) true else false
    }

    private val mSuperIconRect = Rect()

    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.KeyboardView, defStyle, 0
        )
        val inflate = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var previewLayout = 0
        val keyTextSize = 0
        val n = a.indexCount
        for (i in 0 until n) {
            val attr = a.getIndex(i)
            when (attr) {
                R.styleable.KeyboardView_verticalCorrection -> {
                    mVerticalCorrection = a.getDimensionPixelOffset(attr, 0)
                }
                R.styleable.KeyboardView_keyPreviewLayout -> {
                    previewLayout = a.getResourceId(attr, 0)
                }
                R.styleable.KeyboardView_keyPreviewOffset -> {
                    mPreviewOffset = a.getDimensionPixelOffset(attr, 0)
                }
                R.styleable.KeyboardView_keyPreviewHeight -> {
                    mPreviewHeight = a.getDimensionPixelSize(attr, 80)
                }
                R.styleable.KeyboardView_keyTextSize -> {
                    mKeyTextSize = a.getDimensionPixelSize(attr, 18)
                }
                R.styleable.KeyboardView_popupLayout -> {
                    mPopupLayout = a.getResourceId(attr, 0)
                }
            }
        }
        a.recycle()
        LONG_PRESS_TIMEOUT = LongPressDurationSetting.getLongPressDurationPreference(getContext())
        mPreviewPopup = PopupWindow(context)
        if (previewLayout != 0) {
            mPreviewText = inflate.inflate(R.layout.keyboard_key_preview, null) as TextView
            mPreviewTextSizeLarge = mPreviewText!!.textSize.toInt()
            mPreviewPopup.contentView = mPreviewText
            mPreviewPopup.setBackgroundDrawable(null)
        } else {
            isPreviewEnabled = false
        }
        mPreviewPopup.isClippingEnabled = false
        mPreviewPopup.animationStyle = 0
        mPreviewPopup.isTouchable = false
        mPopupParent = this
        //mPredicting = true;
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Align.CENTER
        mPaint.alpha = 255
        setSwipeThreshold(SwipeSensitivitySetting.getSwipeSensitivityPreference(getContext()))
        resetMultiTap()
        initGestureDetector()
        init(context)
    }

    protected fun drawSuperIcon(
        key: Keyboard.Key,
        canvas: Canvas?,
        kbPadding: Rect,
        keyPadding: Rect,
        icon: Drawable
    ) {
        val theme = KeyboardThemeManager.getCurrentTheme()
        val size = (resources.getDimension(R.dimen.key_super_label_text_size)
            * keyboard!!.keyScale.rowDefault).toInt()
        val horizOffset = theme.superHorizOffset
        // Calculate horizontal location of label
        if (horizOffset == 0) {
            // Center
            mSuperIconRect.right = kbPadding.left + key.x + (key.width + size) / 2
            mSuperIconRect.left = mSuperIconRect.right - size
        } else if (horizOffset > 0) {
            // Right-align
            mSuperIconRect.right =
                kbPadding.left + key.x + key.width - keyPadding.right - horizOffset
            mSuperIconRect.left = mSuperIconRect.right - size
        } else {
            // Left-align
            mSuperIconRect.left = kbPadding.left + key.x + keyPadding.left - horizOffset
            mSuperIconRect.right = mSuperIconRect.left + size
        }
        mSuperIconRect.top = kbPadding.top + key.y + keyPadding.top
        mSuperIconRect.bottom = mSuperIconRect.top + size
        icon.bounds = mSuperIconRect
        icon.draw(canvas!!)
    }

    /**
     * Called to update all key icons, in case the theme changed.
     *
     * @param key    The key to update
     */
    protected fun updateKeyIcon(key: Keyboard.Key) {
        // TODO: This is probably very inefficient. :(
        if (key === keyboard!!.anyKey) {
            updateAnyKeySym()
        } else if (key === keyboard!!.actionKey) {
            // The action key icon is handled by BaseKeyboard.setImeOptions()
            return
        } else {
            _updateKeyIcon(
                key, R.integer.keycode_arrow_left,
                KeyboardTheme.KEY_SYM_ARROW_LEFT
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrow_right,
                KeyboardTheme.KEY_SYM_ARROW_RIGHT
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrow_down,
                KeyboardTheme.KEY_SYM_ARROW_DOWN
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrow_up,
                KeyboardTheme.KEY_SYM_ARROW_UP
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrow_back,
                KeyboardTheme.KEY_SYM_ARROW_BACK
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrow_next,
                KeyboardTheme.KEY_SYM_ARROW_NEXT
            )
            setShiftKeyIcon(capsLock, false)
            _updateKeyIcon(
                key, R.integer.keycode_delete,
                KeyboardTheme.KEY_SYM_DELETE
            )
            _updateKeyIcon(
                key, R.integer.keycode_space,
                KeyboardTheme.KEY_SYM_SPACE
            )
            _updateKeyIcon(
                key, R.integer.keycode_arrows,
                KeyboardTheme.KEY_SYM_ARROWS
            )
            _updateKeyIcon(
                key, R.integer.keycode_voice,
                KeyboardTheme.KEY_SYM_MIC
            )
            _updateKeyIcon(
                key, R.integer.keycode_settings,
                KeyboardTheme.KEY_SYM_SETTINGS
            )
            _updateKeyIcon(
                key, R.integer.keycode_translate,
                KeyboardTheme.KEY_SYM_TRANSLATE
            )
            _updateKeyIcon(
                key, R.integer.keycode_locale,
                KeyboardTheme.KEY_SYM_LOCALE
            )
            _updateKeyIcon(
                key, R.integer.keycode_emoji,
                KeyboardTheme.KEY_SYM_EMOJI
            )
        }
        BaseKeyboard.checkForNulls("updateKeyIcon()", key)
    }

    /**
     * Updates the Any Key symbol, based on user preference.
     */
    protected fun updateAnyKeySym() {
        val key = keyboard!!.anyKey
        val kbTheme = KeyboardThemeManager.getCurrentTheme()
        if (key == null) return

        // Override default label/icon for customizable Any Key
        when (anyKeyAction) {
            context.getString(R.string.any_key_action_id_emoji_menu), context.getString(R.string.any_key_action_id_smiley_menu_deprecated) -> {
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_EMOJI)
                key.codes[0] = BaseKeyboard.KEYCODE_EMOJI
            }
            context.getString(R.string.any_key_action_id_arrow_keypad) -> {
                // Arrow keypad
                key.label = null
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_ARROWS)
                key.codes[0] = BaseKeyboard.KEYCODE_ARROWS
            }
            context.getString(R.string.any_key_action_id_voice_input) -> {
                // Voice input
                key.label = null
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_MIC)
                key.codes[0] = BaseKeyboard.KEYCODE_VOICE
            }
            context.getString(R.string.any_key_action_id_translator) -> {
                // Translator
                key.label = null
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_TRANSLATE)
                key.codes[0] = BaseKeyboard.KEYCODE_TRANSLATE
            }
            context.getString(R.string.any_key_action_id_locale) -> {
                // Language
                key.label = null
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_LOCALE)
                key.codes[0] = BaseKeyboard.KEYCODE_LOCALE
            }
            context.getString(R.string.any_key_action_id_settings) -> {
                // Settings
                key.label = null
                key.icon = getAnyKeyIcon(anyKeyAction, false)
                key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SETTINGS)
                key.codes[0] = BaseKeyboard.KEYCODE_SETTINGS
            }
            else -> Assert.assertTrue(false)
        }
        BaseKeyboard.checkForNulls("updateAnyKeySym()", key)
        BaseKeyboard.updateKeyBounds(key)

        // Assign the any key as one of the special keys
        keyboard!!.assignSpecialKey(key)
        if (mAnyKeyLongAction != null) keyboard!!.assignSpecialKey(
            key,
            getFnKeyCode(mAnyKeyLongAction)
        )
        if (mReturnKeyLongAction != null) keyboard!!.assignSpecialKey(
            key,
            getFnKeyCode(mReturnKeyLongAction)
        )
    }

    /**
     * Updates the icon for a particular key.
     *
     * @param key                The key to update.
     * @param keyCodeResID        An integer resource for the key code.
     * @param drawableID        The theme drawable id.
     * @return                    Returns 0 if the key already had the requested drawable, 1 if it
     * is updated, and -1 if key doesn't match the key code.
     */
    private fun _updateKeyIcon(key: Keyboard.Key, keyCodeResID: Int, drawableID: Int): Int {
        val res = context.resources
        val drawable: Drawable
        if (key.codes[0] == res.getInteger(keyCodeResID)) {
            drawable = KeyboardThemeManager.getCurrentTheme().getDrawable(drawableID)
            return if (key.icon === drawable) {
                0
            } else {
                key.icon = drawable
                1
            }
        }
        return -1
    }
    /*
	 * Photo wallpaper methods.
	 */
    /**
     * Load the photo wallpaper drawable from shared prefs.
     *
     * @param context    A context.
     */
    protected open fun loadWallpaper(context: Context) {
        if (mPhotoBackground != null) // Don't bother reloading
            return

        // Get alpha and fit from preferences
        val preference = context.getSharedPreferences(
            Settings.SETTINGS_FILE, Context.MODE_PRIVATE
        )
        mPhotoKeyBGAlpha = preference.getInt("wallpaper_alpha", 255)
        mBackgroundFit = preference.getBoolean("wallpaper_fit", true)
        mPhotoBackground = WallpaperPhoto.loadWallpaper(context)
        setPhotoWallpaperBounds()
    }

    /**
     * Apply a photo wallpaper to the background. Used by WallpaperPhoto for
     * preview purposes only.
     *
     * @param wallpaper
     * @param alpha
     * @param fit
     */
    fun applyWallpaper(wallpaper: Drawable?, alpha: Int, fit: Boolean) {
        mPhotoBackground = wallpaper
        mPhotoKeyBGAlpha = alpha
        mBackgroundFit = fit
        setPhotoWallpaperBounds()
        invalidateAllKeys()
    }

    /**
     * Reloads the wallpaper. Called by WallpaperPhoto to restore the original
     * wallpaper if user cancels the preview.
     */
    fun reloadWallpaper() {
        mPhotoBackground = null
        loadWallpaper(context)
    }

    /**
     * Reloads the current theme.
     */
    fun reloadTheme() {
        invalidateAllKeys()
    }

    protected fun getKeyIndices(x: Int, y: Int, allKeys: IntArray?): Int {
        val keys = mKeys
        var primaryIndex = NOT_A_KEY
        var closestKey = NOT_A_KEY
        var closestKeyDist = mProximityThreshold + 1
        Arrays.fill(mDistances, Int.MAX_VALUE)
        val nearestKeyIndices = mKeyboard!!.getNearestKeys(x, y)
        val keyCount = nearestKeyIndices.size
        for (i in 0 until keyCount) {
            val key = keys[nearestKeyIndices[i]]
            var dist = 0
            val isInside = key.isInside(x, y)
            if (isInside) {
                primaryIndex = nearestKeyIndices[i]
            }
            if (((isProximityCorrectionEnabled
                    && key.squaredDistanceFrom(x, y).also { dist = it } < mProximityThreshold)
                    || isInside)
                && key.codes[0] > 32
            ) {
                // Find insertion point
                val nCodes = key.codes.size
                if (dist < closestKeyDist) {
                    closestKeyDist = dist
                    closestKey = nearestKeyIndices[i]
                }
                if (allKeys == null) continue
                for (j in mDistances.indices) {
                    if (mDistances[j] > dist) {
                        // Make space for nCodes codes
                        System.arraycopy(
                            mDistances, j, mDistances, j + nCodes,
                            mDistances.size - j - nCodes
                        )
                        System.arraycopy(
                            allKeys, j, allKeys, j + nCodes,
                            allKeys.size - j - nCodes
                        )
                        for (c in 0 until nCodes) {
                            allKeys[j + c] = key.codes[c]
                            mDistances[j + c] = dist
                        }
                        break
                    }
                }
            }
        }
        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey
        }
        return primaryIndex
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        if (index != NOT_A_KEY && index < mKeys.size) {
            val key = mKeys[index]
            if (key.text != null) {
                onKeyboardActionListener!!.onText(key.text)
                onKeyboardActionListener!!.onRelease(NOT_A_KEY)
            } else {
                var code = key.codes[0]
                //TextEntryState.keyPressedAt(key, x, y);
                val codes = IntArray(MAX_NEARBY_KEYS)
                Arrays.fill(codes, NOT_A_KEY)
                getKeyIndices(x, y, codes)
                // Multi-tap
                if (mInMultiTap) {
                    if (mTapCount != -1) {
                        onKeyboardActionListener!!.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE)
                    } else {
                        mTapCount = 0
                    }
                    code = key.codes[mTapCount]
                }
                onKeyboardActionListener!!.onKey(code, codes)
                onKeyboardActionListener!!.onRelease(code)
            }
            mLastSentIndex = index
            mLastTapTime = eventTime
        }
    }

    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    private fun getPreviewText(key: Keyboard.Key): CharSequence {
        return if (mInMultiTap) {
            // Multi-tap
            mPreviewLabel.setLength(0)
            mPreviewLabel.append(key.codes[if (mTapCount < 0) 0 else mTapCount].toChar())
            adjustCase(mPreviewLabel)
        } else {
            if (mLongPress && key.popupCharacters != null) adjustCase(key.popupCharacters) else adjustCase(
                key.label
            )
        }
    }

    protected fun updatePreview(key: Keyboard.Key) {
        for (iKey in mKeys.indices) if (mKeys[iKey] === key) {
            updatePreview(iKey)
            break
        }
    }

    protected fun updatePreview(keyIndex: Int) {
        val oldKeyIndex = mCurrentKeyIndex
        mCurrentKeyIndex = keyIndex
        // Release the old key and press the new key
        val keys = mKeys
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (oldKeyIndex != NOT_A_KEY && keys.size > oldKeyIndex) {
                keys[oldKeyIndex].onReleased(mCurrentKeyIndex == NOT_A_KEY)
                invalidateKey(oldKeyIndex)
            }
            if (mCurrentKeyIndex != NOT_A_KEY && keys.size > mCurrentKeyIndex) {
                keys[mCurrentKeyIndex].onPressed()
                invalidateKey(mCurrentKeyIndex)
            }
        }
        // If key changed or superscript was activated, and preview is on ...
        if ((oldKeyIndex != mCurrentKeyIndex || mLongPress) && isPreviewEnabled) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW)
            if (mPreviewPopup.isShowing) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler.sendMessageDelayed(
                        mHandler
                            .obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW.toLong()
                    )
                }
            }
            if (keyIndex != NOT_A_KEY) {
                if (mPreviewPopup.isShowing && mPreviewText!!.visibility == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showPreview(keyIndex)
                } else {
                    mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0),
                        DELAY_BEFORE_PREVIEW.toLong()
                    )
                }
            }
        }
    }

    private fun showPreview(keyIndex: Int) {
        if (keyIndex < 0 || keyIndex >= mKeys.size) return
        showPreview(mKeys[keyIndex])
    }

    private fun showPreview(key: Keyboard.Key) {
        if (key.label == null && key.icon == null) return
        if (key.icon != null) {
            mPreviewText!!.setCompoundDrawables(
                null, null, null,
                if (key.iconPreview != null) key.iconPreview else key.icon
            )
            mPreviewText!!.setText(null)
        } else {
            val previewText = getPreviewText(key)
            mPreviewText!!.setCompoundDrawables(null, null, null, null)
            mPreviewText!!.text = previewText
            if (previewText.length > 1 && key.codes.size < 2) {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize.toFloat())
                mPreviewText!!.setTypeface(Typeface.DEFAULT_BOLD)
            } else {
                mPreviewText!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    mPreviewTextSizeLarge.toFloat()
                )
                mPreviewText!!.setTypeface(Typeface.DEFAULT)
            }
        }
        mPreviewText!!.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = Math.max(
            mPreviewText!!.measuredWidth,
            key.width + mPreviewText!!.paddingLeft + mPreviewText!!.paddingRight
        )
        val popupHeight = key.height + mPreviewText!!.paddingTop + mPreviewText!!.paddingBottom
        val lp = mPreviewText!!.layoutParams
        if (lp != null) {
            lp.width = popupWidth
            lp.height = popupHeight
        }
        if (!mPreviewCentered) {
            mPopupPreviewX = key.x - mPreviewText!!.paddingLeft + paddingLeft
            mPopupPreviewY = key.y - popupHeight + mPreviewOffset
        } else {
            // TODO: Fix this if centering is brought back
            mPopupPreviewX = 160 - mPreviewText!!.measuredWidth / 2
            mPopupPreviewY = -mPreviewText!!.measuredHeight
        }
        mHandler.removeMessages(MSG_REMOVE_PREVIEW)
        getLocationInWindow(mWindowOffsetCoordinates)
        mWindowOffsetCoordinates[0] += mPopupKeyboardOffsetX // Offset may be zero
        mWindowOffsetCoordinates[1] += mPopupKeyboardOffsetY // Offset may be zero
        getLocationOnScreen(mScreenOffsetCoordinates)

        // Set the preview background state
        mPreviewText!!.background.setState(
            if (key.popupResId != 0) LONG_PRESSABLE_STATE_SET else EMPTY_STATE_SET
        )
        if (KeyboardService.getIME() != null
            && !KeyboardService.getIME().isFullscreenMode
        ) {
            mPopupPreviewX += mWindowOffsetCoordinates[0]
            mPopupPreviewY += mWindowOffsetCoordinates[1]
        }
        mPopupPreviewX += mScreenOffsetCoordinates[0]
        if (KeyboardService.getIME() != null
            && !KeyboardService.getIME().isFullscreenMode
        ) {
            if (mScreenOffsetCoordinates[1] == 0) postDelayed({ showPreview(key) }, 100)
        }
        if (!mPreviewPopup.isShowing) {
            mPreviewPopup.width = popupWidth
            mPreviewPopup.height = popupHeight
            mPreviewPopup.showAtLocation(
                mPopupParent,
                Gravity.NO_GRAVITY,
                mPopupPreviewX,
                mPopupPreviewY
            )
            Log.d("PREVIEWPOPUP", "showAtLocation(" + mPreviewPopup.animationStyle + ")")
        } else {
            mPreviewPopup.update(mPopupPreviewX, mPopupPreviewY, -1, -1)
            Log.d("PREVIEWPOPUP", "update($mPopupPreviewY)")
        }
        mPopupPreviewWidthPrev = popupWidth
        mPopupPreviewHeightPrev = popupHeight
        mPreviewText!!.visibility = VISIBLE
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is
     * not sufficient because the keyboard renders the keys to an off-screen
     * buffer and an invalidate() only draws the cached buffer.
     *
     * @see .invalidateKey
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use
     * this method if only one key is changing it's content. Any changes that
     * affect the position or size of the key may not be honored.
     *
     * @param keyIndex        the index of the key in the attached [Keyboard].
     * @see .invalidateAllKeys
     */
    private fun invalidateKey(keyIndex: Int) {
        val keys: Array<Any> = keyboard!!.keys.toTypedArray()
        if (keyIndex < 0 || keyIndex >= keys.size) {
            return
        }
        val key = keys[keyIndex] as Keyboard.Key
        mInvalidatedKey = key
        mDrawPending = true
        mDirtyRect.union(
            key.x + paddingLeft,
            key.y + paddingTop,
            key.x + key.width + paddingLeft,
            key.y + key.height + paddingTop
        )
        invalidate(
            key.x + paddingLeft,
            key.y + paddingTop,
            key.x + key.width + paddingLeft,
            key.y + key.height + paddingTop
        )
    }

    private fun openPopupIfRequired(): Boolean {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false
        }
        if (mCurrentKey < 0 || mCurrentKey >= mKeys.size) {
            return false
        }
        val popupKey = mKeys[mCurrentKey]
        //        if (result) {
////            mAbortKey = true;
//            showPreview(NOT_A_KEY);
//        }
        return onLongPress(popupKey)
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param key the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected fun onLongPress(key: Keyboard.Key): Boolean {
        KeyboardService.writeDebug("KeyboardView.onKey(key=" + key.codes[0] + ")")
        if (mIME!!.inPreviewMode()) return true
        updatePreview(NOT_A_KEY)
        mLongPress = true

        // Send keystroke feedback
        mIME!!.keyFeedback(key.codes[0])
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            onKeyboardActionListener!!.onKey(BaseKeyboard.KEYCODE_SETTINGS, null)
            return true
        } else if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
            // Enable caps lock
            capsLock = !capsLock
            setShifted(capsLock, capsLock)
            return true
        } else if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
            // Open main menu
            mIME!!.keyFeedback(key.codes[0])
            mIME!!.onMainMenuKey()
            return true
        } else if (key.codes[0] == '.'.code) {
            // Open symbols quick menu
            openSymQuickMenu(key)
            return true
        } else if (key.codes[0] == BaseKeyboard.KEYCODE_ACTION) {
            val keycode = getFnKeyCode(mReturnKeyLongAction)
            onKeyboardActionListener!!.onKey(keycode, intArrayOf(keycode))
            return true
        } else if (key === keyboard!!.actionKey) {
            if (keyboard!!.getEditorAction(true) != EditorInfo.IME_ACTION_NONE) {
                // Perform the default editor action
                mIME!!.keyFeedback(key.codes[0])
                mIME!!.performEditorAction()
                return true
            }
        } else if (key === keyboard!!.anyKey) {
            return if (mIME!!.mURL) {
                // Open the URL menu	
                mIME!!.keyFeedback(key.codes[0])
                openUrlKeyboard()
                true
            } else {
                val keycode = getFnKeyCode(mAnyKeyLongAction)
                onKeyboardActionListener!!.onKey(keycode, intArrayOf(keycode))
                true
            }
        } else if (key.popupCharacters != null && key.popupCharacters.length > 0) {
            // Open a popup menu
            if (key.popupCharacters.length == 1) {
                // There is only one alternate character.
                return if (!mIME!!.isDrawSuperLabel(key) && !Character.isDigit(key.popupCharacters[0])) {
                    // disabled long press
                    onKeyboardActionListener!!.onKey(key.codes[0], key.codes)
                    true
                } else {
                    // Change preview from key label to superlabel
                    updatePreview(key)
                    false
                }
            } else if (key.popupCharacters.length > 1 && key.popupResId != 0) {
                // There is more than one alternate character. Show a popup keyboard.
                val clearSymbols =
                    !mIME!!.isSuperLabelEnabled && key.edgeFlags != BaseKeyboard.EDGE_BOTTOM
                var popupChars: CharSequence? = null
                if (clearSymbols) {
                    popupChars = key.popupCharacters
                    // remove symbols					
                    val chars = StringBuilder(key.popupCharacters)
                    var i = 0
                    while (i < chars.length) {
                        val c = chars[i]
                        if (!(Character.isLetter(c) || Character.isDigit(c))) {
                            chars.deleteCharAt(i)
                            i--
                        }
                        i++
                    }
                    key.popupCharacters = chars
                }
                openPopupCharacterKeyboard(key)
                if (clearSymbols) key.popupCharacters = popupChars
                return true
            }
            return false
        }
        return false
    }

    protected fun onLongPressRelease(key: Keyboard.Key): Boolean {
        if (key.popupCharacters != null && key.popupCharacters.length == 1) {
            // There is only one alternate character.
//			if (!mIME.isDrawSuperLabel(key) && !Character.isDigit(key.popupCharacters.charAt(0))) {
            // disabled long press
            val keycode = key.popupCharacters[0].code
            onKeyboardActionListener!!.onKey(keycode, intArrayOf(keycode))
            return true
            //			}
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(me: MotionEvent): Boolean {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two-thumb typing
        val pointerCount = me.pointerCount
        val action = me.action
        var result: Boolean
        val now = me.eventTime
        if (pointerCount != mOldPointerCount) {
            if (pointerCount == 1) {
                // Send a down event for the latest pointer
                val down = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_DOWN,
                    me.x, me.y, me.metaState
                )
                result = onModifiedTouchEvent(down, false)
                down.recycle()
                // If it's an up action, then deliver the up as well.
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(me, true)
                }
            } else {
                // Send an up event for the last pointer
                val up = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_UP,
                    mOldPointerX, mOldPointerY, me.metaState
                )
                result = onModifiedTouchEvent(up, true)
                up.recycle()
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(me, false)
                mOldPointerX = me.x
                mOldPointerY = me.y
            } else {
                // Don't do anything when 2 pointers are down and moving.
                result = true
            }
        }
        mOldPointerCount = pointerCount
        return result
    }

    protected open fun onModifiedTouchEvent(me: MotionEvent, possiblePoly: Boolean): Boolean {
        var touchX = me.x.toInt() - paddingLeft
        var touchY = me.y.toInt() + mVerticalCorrection - paddingTop
        val action = me.action
        val eventTime = me.eventTime
        val keyIndex = getKeyIndices(touchX, touchY, null)
        mPossiblePoly = possiblePoly
        if (mPopupKeyboardWindow != null && mPopupKeyboardWindow!!.isShowing && mPopupKeyboardView != null && mPopupKeyboardView!!.isOneTouch) {
            me.offsetLocation(
                -mPopupKeyboardRect!!.left.toFloat(),
                (mIME!!.candidateHeight - mPopupKeyboardRect!!.top).toFloat()
            )
            return mPopupKeyboardView!!.dispatchTouchEvent(me)
        }


        // Track the last few movements to look for spurious swipes.
        if (action == MotionEvent.ACTION_DOWN) mSwipeTracker.clear()
        mSwipeTracker.addMovement(me)

        // Ignore all motion events until a DOWN.
//        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
//            return true;
//        }
        if (mGestureDetector!!.onTouchEvent(me)) {
            updatePreview(NOT_A_KEY)
            mHandler.removeMessages(MSG_REPEAT)
            mHandler.removeMessages(MSG_LONGPRESS)
            return true
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                //        	mAbortKey = false;
                mLongPress = false
                mLastCodeX = touchX
                mLastCodeY = touchY
                mLastKeyTime = 0
                mCurrentKeyTime = 0
                mLastKey = NOT_A_KEY
                mCurrentKey = keyIndex
                mDownTime = me.eventTime
                mLastMoveTime = mDownTime
                checkMultiTap(eventTime, keyIndex)
                onKeyboardActionListener!!.onPress(if (keyIndex != NOT_A_KEY) mKeys[keyIndex].codes[0] else 0)
                if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey
                    val msg = mHandler.obtainMessage(MSG_REPEAT)
                    mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY.toLong())
                    repeatKey()
                    // Delivering the key could have caused an abort
//        		if (mAbortKey) {
//        			mRepeatKeyIndex = NOT_A_KEY;
//        			break;
//        		}
                }
                if (mCurrentKey != NOT_A_KEY) {
                    val msg = mHandler.obtainMessage(MSG_LONGPRESS, me)
                    mHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT.toLong())
                }
                updatePreview(keyIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex
                        mCurrentKeyTime = eventTime - mDownTime
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime
                            continueLongPress = true
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            resetMultiTap()
                            mLastKey = mCurrentKey
                            mLastCodeX = mLastX
                            mLastCodeY = mLastY
                            mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                            mCurrentKey = keyIndex
                            mCurrentKeyTime = 0
                        }
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    mHandler.removeMessages(MSG_LONGPRESS)
                    // Start new longpress if key has changed
                    if (keyIndex != NOT_A_KEY) {
                        val msg = mHandler.obtainMessage(MSG_LONGPRESS, me)
                        mHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT.toLong())
                    }
                }
                updatePreview(mCurrentKey)
                mLastMoveTime = eventTime
            }

            MotionEvent.ACTION_UP -> {
                removeMessages()
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime
                } else {
                    resetMultiTap()
                    mLastKey = mCurrentKey
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                    mCurrentKey = keyIndex
                    mCurrentKeyTime = 0
                }
                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey
                    touchX = mLastCodeX
                    touchY = mLastCodeY
                }
                updatePreview(NOT_A_KEY)
                Arrays.fill(mKeyIndices, NOT_A_KEY)
                if (mLongPress && keyIndex != NOT_A_KEY) {
                    onLongPressRelease(mKeys[keyIndex])
                } else if (mRepeatKeyIndex == NOT_A_KEY /*&& !mAbortKey*/) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                }
                invalidateKey(keyIndex)
                mRepeatKeyIndex = NOT_A_KEY
                mLongPress = false
            }

            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
                mLongPress = false
                updatePreview(NOT_A_KEY)
                invalidateKey(mCurrentKey)
            }
        }
        mLastX = touchX
        mLastY = touchY
        return true
    }

    // Overriding this method may provide some performance improvements (see View docs)
    override fun isOpaque(): Boolean {
        return true
    }

    private fun repeatKey(): Boolean {
        val key = mKeys[mRepeatKeyIndex]
        detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime)
        return true
    }

    protected fun swipeRight() {
        onKeyboardActionListener!!.swipeRight()
    }

    protected fun swipeLeft() {
        onKeyboardActionListener!!.swipeLeft()
    }

    protected fun swipeUp() {
        onKeyboardActionListener!!.swipeUp()
    }

    protected fun swipeDown() {
        onKeyboardActionListener!!.swipeDown()
    }

    fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()
        dismissAllPopupWindows(true)
        mBuffer = null
        mCanvas = null
    }

    private fun removeMessages() {
        mHandler.removeMessages(MSG_REPEAT)
        mHandler.removeMessages(MSG_LONGPRESS)
        mHandler.removeMessages(MSG_SHOW_PREVIEW)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun resetMultiTap() {
        mLastSentIndex = NOT_A_KEY
        mTapCount = 0
        mLastTapTime = -1
        mInMultiTap = false
    }

    private fun checkMultiTap(eventTime: Long, keyIndex: Int) {
        if (keyIndex == NOT_A_KEY) return
        val key = mKeys[keyIndex]
        if (key.codes.size > 1) {
            mInMultiTap = true
            if (eventTime < mLastTapTime + MULTITAP_INTERVAL
                && keyIndex == mLastSentIndex
            ) {
                mTapCount = (mTapCount + 1) % key.codes.size
                return
            } else {
                mTapCount = -1
                return
            }
        }
        if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
            resetMultiTap()
        }
    }

    val totalHeight: Int
        get() = height + mIME!!.candidateHeight

    private class SwipeTracker {
        val mPastX = FloatArray(NUM_PAST)
        val mPastY = FloatArray(NUM_PAST)
        val mPastTime = LongArray(NUM_PAST)
        var yVelocity = 0f
        var xVelocity = 0f
        fun clear() {
            mPastTime[0] = 0
        }

        fun addMovement(ev: MotionEvent) {
            val time = ev.eventTime
            val N = ev.historySize
            for (i in 0 until N) {
                addPoint(
                    ev.getHistoricalX(i), ev.getHistoricalY(i),
                    ev.getHistoricalEventTime(i)
                )
            }
            addPoint(ev.x, ev.y, time)
        }

        private fun addPoint(x: Float, y: Float, time: Long) {
            var drop = -1
            var i: Int
            val pastTime = mPastTime
            i = 0
            while (i < NUM_PAST) {
                if (pastTime[i] == 0L) {
                    break
                } else if (pastTime[i] < time - LONGEST_PAST_TIME) {
                    drop = i
                }
                i++
            }
            if (i == NUM_PAST && drop < 0) {
                drop = 0
            }
            if (drop == i) drop--
            val pastX = mPastX
            val pastY = mPastY
            if (drop >= 0) {
                val start = drop + 1
                val count = NUM_PAST - drop - 1
                System.arraycopy(pastX, start, pastX, 0, count)
                System.arraycopy(pastY, start, pastY, 0, count)
                System.arraycopy(pastTime, start, pastTime, 0, count)
                i -= drop + 1
            }
            pastX[i] = x
            pastY[i] = y
            pastTime[i] = time
            i++
            if (i < NUM_PAST) {
                pastTime[i] = 0
            }
        }

        @JvmOverloads
        fun computeCurrentVelocity(units: Int, maxVelocity: Float = Float.MAX_VALUE) {
            val pastX = mPastX
            val pastY = mPastY
            val pastTime = mPastTime
            val oldestX = pastX[0]
            val oldestY = pastY[0]
            val oldestTime = pastTime[0]
            var accumX = 0f
            var accumY = 0f
            var N = 0
            while (N < NUM_PAST) {
                if (pastTime[N] == 0L) {
                    break
                }
                N++
            }
            for (i in 1 until N) {
                val dur = (pastTime[i] - oldestTime).toInt()
                if (dur == 0) continue
                var dist = pastX[i] - oldestX
                var vel = dist / dur * units // pixels/frame.
                accumX = if (accumX == 0f) vel else (accumX + vel) * .5f
                dist = pastY[i] - oldestY
                vel = dist / dur * units // pixels/frame.
                accumY = if (accumY == 0f) vel else (accumY + vel) * .5f
            }
            xVelocity =
                if (accumX < 0.0f) Math.max(accumX, -maxVelocity) else Math.min(accumX, maxVelocity)
            yVelocity =
                if (accumY < 0.0f) Math.max(accumY, -maxVelocity) else Math.min(accumY, maxVelocity)
        }

        companion object {
            const val NUM_PAST = 4
            const val LONGEST_PAST_TIME = 200
        }
    }

    /*
	 * Methods to save and restore PopupWindows in case the view is
	 * re-initialized
	 */
    val currPopupLayoutID: Int
        /**
         * Returns the layout id of contents of the currently-open PopupWindow
         *
         * @return    A layout id
         */
        get() = if (lastPopupWindow == null) -1 else lastPopupWindow!!.contentView.id

    val currPopupKeyboardID: Int
        /**
         * Returns the keyboard id of the currently-open popup keyboard
         *
         * @return    A keyboard id
         */
        get() {
            if (lastPopupWindow == null) return -1
            val keyboardLayout = lastPopupWindow!!.contentView as ViewGroup
            if (keyboardLayout is PopupKeyboardLayout) {
                val popupKeyboardLayout =
                    lastPopupWindow!!.contentView as PopupKeyboardLayout
                return popupKeyboardLayout.keyboardView.keyboard?.xmlResID ?: 0
            }
            return -1
        }

    /**
     * Restores a PopupWindow after the view is re-initialized
     *
     * @param popupLayoutID        The layout ID of the popup keyboard.
     * @param popupKeyboardID    The ID of the keyboard XML file.
     * @param parm                ??? The Any Key ???
     */
    fun restorePopupWindow(popupLayoutID: Int, popupKeyboardID: Int, parm: Any?) {
        if (popupLayoutID == -1) return
        if (popupLayoutID == R.id.popupKeyboardLayout) {
            if (popupKeyboardID == R.xml.emoji_menu) openEmojiKeyboard() else if (popupKeyboardID == R.xml.url_menu) openUrlKeyboard()
        } else if (popupLayoutID == R.id.sym_menu_layout) openSymMenu() else if (popupLayoutID == R.id.translating_keyboard) openTranslator() else if (popupLayoutID == R.id.popupKeyboardLayout) if (popupKeyboardID == R.xml.url_menu) openUrlKeyboard() else if (popupLayoutID == R.id.main_menu) {
            Assert.assertTrue(parm != null)
            openMainMenu()
        }
    }

    fun toString(name: String): String {
        val buffer = StringBuilder()
        Utils.appendLine(buffer, name, javaClass.name)
        Utils.appendLine(buffer, name, "{")
        val subName = name + "\t"
        Utils.appendLine(buffer, subName, "mDrawPending = $mDrawPending")
        Utils.appendLine(buffer, subName, "mBuffer = $mBuffer")
        Utils.appendLine(buffer, subName, "mCanvas = $mCanvas")
        Utils.appendLine(buffer, subName, "mInvalidatedKey = $mInvalidatedKey")
        Utils.appendLine(buffer, subName, "mPhotoBackground = $mPhotoBackground")
        Utils.appendLine(buffer, subName, "mKeyBGAlpha" + " = " + mPhotoKeyBGAlpha)
        Utils.appendLine(buffer, subName, "mBackgroundFit" + " = " + mBackgroundFit)
        Utils.appendLine(buffer, subName, "mPopupKeyboardWindow = $mPopupKeyboardWindow")
        Utils.appendLine(buffer, subName, "mPopupKeyboardLayout = $mPopupKeyboardLayout")
        Utils.appendLine(buffer, subName, "mPopupKeyboardView = $mPopupKeyboardView")
        Utils.appendLine(buffer, subName, "mSymMenuWindow = $mSymMenuWindow")
        Utils.appendLine(buffer, subName, "mSymMenuLayout = $mSymMenuLayout")
        Utils.appendLine(buffer, subName, "mSymQuickMenuWindow = $mSymQuickMenuWindow")
        Utils.appendLine(buffer, subName, "mTranslatorWindow = $mTranslatorWindow")
        Utils.appendLine(buffer, subName, "mTranslatorView = $mTranslatorView")

        // Add all native fields
        Utils.appendLine(buffer, subName, "")
        Utils.appendLine(
            buffer, subName,
            Utils.getClassString(this, subName)
        )
        Utils.appendLine(buffer, name, "}")
        return buffer.toString()
    }

    companion object {
        private val KEY_DELETE = intArrayOf(Keyboard.KEYCODE_DELETE)
        private val LONG_PRESSABLE_STATE_SET = intArrayOf(R.attr.state_long_pressable)
        protected const val NOT_A_KEY = -1
        private const val MSG_SHOW_PREVIEW = 1
        private const val MSG_REMOVE_PREVIEW = 2
        private const val MSG_REPEAT = 3
        private const val MSG_LONGPRESS = 4
        private const val DELAY_BEFORE_PREVIEW = 100
        private const val DELAY_AFTER_PREVIEW = 70
        private const val DEBOUNCE_TIME = 70
        private const val REPEAT_INTERVAL = 50 // ~20 keys per second
        private const val REPEAT_START_DELAY = 400
        @JvmField
        var LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
        private const val MAX_NEARBY_KEYS = 12
        private const val MULTITAP_INTERVAL = 800 // milliseconds
        private var mPhotoKeyBGAlpha = 255
        private var mBackgroundFit = true
    }
}