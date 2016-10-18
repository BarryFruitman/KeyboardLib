package com.comet.keyboard;

import com.comet.keyboard.layouts.KeyboardLayout;
import com.comet.keyboard.settings.LongPressDurationSetting;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.settings.SwipeSensitivitySetting;
import com.comet.keyboard.settings.WallpaperPhoto;
import com.comet.keyboard.theme.KeyboardTheme;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;
import com.comet.keyboard.voice.dragon.DragonVoiceView;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

public class KeyboardView extends View {

    /**
     * Listener for virtual keyboard events.
     */
    public interface OnKeyboardActionListener {
        
        /**
         * Called when the user presses a key. This is sent before the {@link #onKey} is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
         * key, the value will be zero.
         */
        void onPress(int primaryCode);
        
        /**
         * Called when the user releases a key. This is sent after the {@link #onKey} is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the code of the key that was released
         */
        void onRelease(int primaryCode);

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
        void onKey(int primaryCode, int[] keyCodes);

        /**
         * Sends a sequence of characters to the listener.
         * @param text the sequence of characters to be displayed.
         */
        void onText(CharSequence text);
        
        /**
         * Called when the user quickly moves the finger from right to left.
         */
        void swipeLeft();
        
        /**
         * Called when the user quickly moves the finger from left to right.
         */
        void swipeRight();
        
        /**
         * Called when the user quickly moves the finger from up to down.
         */
        void swipeDown();
        
        /**
         * Called when the user quickly moves the finger from down to up.
         */
        void swipeUp();
    }

    private static final int[] KEY_DELETE = { Keyboard.KEYCODE_DELETE };
    private static final int[] LONG_PRESSABLE_STATE_SET = { R.attr.state_long_pressable };

    protected static final int NOT_A_KEY = -1;

    private BaseKeyboard mKeyboard;
    protected int mCurrentKeyIndex = NOT_A_KEY;
    private int mKeyTextSize;
    private int[] mWindowOffsetCoordinates = new int[2];
    private int[] mScreenOffsetCoordinates = new int[2];
    
    private TextView mPreviewText;
    private PopupWindow mPreviewPopup;
    private int mPreviewTextSizeLarge;
    private int mPreviewOffset;
    private int mPreviewHeight;

    private View mPopupParent;
    private int mPopupKeyboardOffsetX;
    private int mPopupKeyboardOffsetY;
    private Key[] mKeys;

    /** Listener for {@link OnKeyboardActionListener}. */
    private OnKeyboardActionListener mKeyboardActionListener;
    
    private static final int MSG_SHOW_PREVIEW = 1;
    private static final int MSG_REMOVE_PREVIEW = 2;
    private static final int MSG_REPEAT = 3;
    private static final int MSG_LONGPRESS = 4;

    private static final int DELAY_BEFORE_PREVIEW = 50;
    private static final int DELAY_AFTER_PREVIEW = 70;
    private static final int DEBOUNCE_TIME = 70;
    
    private int mVerticalCorrection;
    private int mProximityThreshold;

    private boolean mPreviewCentered = false;
    private boolean mShowPreview = true;
    private int mPopupPreviewX;
    private int mPopupPreviewY;
	private int mPopupPreviewWidthPrev;
	private int mPopupPreviewHeightPrev;

    private int mLastX;
    private int mLastY;

    private boolean mProximityCorrectOn;
    
    private Paint mPaint;
    
    private long mDownTime;
    private long mLastMoveTime;
    private int mLastKey;
    private int mLastCodeX;
    private int mLastCodeY;
    private int mCurrentKey = NOT_A_KEY;
    private long mLastKeyTime;
    private long mCurrentKeyTime;
    private int[] mKeyIndices = new int[12];
    private GestureDetector mGestureDetector;
    private int mRepeatKeyIndex = NOT_A_KEY;
    private int mPopupLayout;
    private Key mInvalidatedKey;
    private boolean mPossiblePoly;
    private SwipeTracker mSwipeTracker = new SwipeTracker();

    // Variables for dealing with multiple pointers
    private int mOldPointerCount = 1;
    private float mOldPointerX;
    private float mOldPointerY;

    private static final int REPEAT_INTERVAL = 50; // ~20 keys per second
    private static final int REPEAT_START_DELAY = 400;
    public static int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();    

    private static int MAX_NEARBY_KEYS = 12;
    private int[] mDistances = new int[MAX_NEARBY_KEYS];
    private int mSwipeThreshold;

    // For multi-tap
    private int mLastSentIndex;
    private int mTapCount;
    private long mLastTapTime;
    private boolean mInMultiTap;
    private static final int MULTITAP_INTERVAL = 800; // milliseconds
    private StringBuilder mPreviewLabel = new StringBuilder(1);

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted. **/
    private boolean mDrawPending;
    /** The dirty region in the keyboard bitmap */
    private Rect mDirtyRect = new Rect();
    /** The keyboard bitmap for faster updates */
    private Bitmap mBuffer;
    /** The canvas for the above mutable keyboard bitmap */
    private Canvas mCanvas;

    
    
    
    
    
    
	protected Drawable mPhotoBackground;
	private static int mPhotoKeyBGAlpha = 255;
	private static boolean mBackgroundFit = true;
	
	// any key action for simple press
	private String mAnyKeyAction = getContext().getString(R.string.any_key_action_id_default);
	// any key action for long press
	private String mAnyKeyLongAction;
	// return key action for long press
	private String mReturnKeyLongAction;
	
	private boolean mLongPress = false;

	private KeyboardService mIME;
	PopupWindow mPopupKeyboardWindow;
	Rect mPopupKeyboardRect;
	private PopupKeyboardLayout mPopupKeyboardLayout;
	private PopupKeyboardView mPopupKeyboardView;
	
	private PopupWindow mSymMenuWindow;
	Rect mSymQuickMenuRect;
	private SymMenuLayout mSymMenuLayout;
	private PopupWindow mSymQuickMenuWindow;
	
	private PopupWindow mTranslatorWindow;
	TranslatorView mTranslatorView;
	
	private PopupWindow mDragonVoiceWindow;
	private DragonVoiceView mDragonVoiceView;
	private FrameLayout mMainMenuView;
	private PopupWindow mMainMenuWindow;
	private PopupWindow mLastPopupWindow;
    
	private boolean mCapsLock;
    
    
    
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_PREVIEW:
               		showPreview(msg.arg1);
                    break;
                case MSG_REMOVE_PREVIEW:
                    mPreviewText.setVisibility(INVISIBLE);
                    break;
                case MSG_REPEAT:
                    if (repeatKey()) {
                        Message repeat = Message.obtain(this, MSG_REPEAT);
                        sendMessageDelayed(repeat, REPEAT_INTERVAL);                        
                    }
                    break;
                case MSG_LONGPRESS:
                    openPopupIfRequired((MotionEvent) msg.obj);
                    break;
            }
        }
    };

    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.keyboardViewStyle);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a =
            context.obtainStyledAttributes(
                attrs, R.styleable.KeyboardView, defStyle, 0);
        
        LayoutInflater inflate =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int previewLayout = 0;
        int keyTextSize = 0;

        int n = a.getIndexCount();
        
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.KeyboardView_verticalCorrection) {
                mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.KeyboardView_keyPreviewLayout) {
                previewLayout = a.getResourceId(attr, 0);
            } else if (attr == R.styleable.KeyboardView_keyPreviewOffset) {
                mPreviewOffset = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.KeyboardView_keyPreviewHeight) {
                mPreviewHeight = a.getDimensionPixelSize(attr, 80);
            } else if (attr == R.styleable.KeyboardView_keyTextSize) {
                mKeyTextSize = a.getDimensionPixelSize(attr, 18);
            } else if (attr == R.styleable.KeyboardView_popupLayout) {
                mPopupLayout = a.getResourceId(attr, 0);
            }
        }

        a.recycle();
        
        LONG_PRESS_TIMEOUT = LongPressDurationSetting.getLongPressDurationPreference(getContext());
        
        mPreviewPopup = new PopupWindow(context);
        if (previewLayout != 0) {
            mPreviewText = (TextView) inflate.inflate(R.layout.keyboard_key_preview, null);
            mPreviewTextSizeLarge = (int) mPreviewText.getTextSize();
            mPreviewPopup.setContentView(mPreviewText);
            mPreviewPopup.setBackgroundDrawable(null);
        } else {
            mShowPreview = false;
        }

        mPreviewPopup.setClippingEnabled(false);
        mPreviewPopup.setAnimationStyle(0);
        mPreviewPopup.setTouchable(false);

        mPopupParent = this;
        //mPredicting = true;
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(keyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        setSwipeThreshold(SwipeSensitivitySetting.getSwipeSensitivityPreference(getContext()));

        resetMultiTap();
        initGestureDetector();
        
        init(context);
    }
    
    
    
	/**
	 * Called by constructors to initialize the object.
	 * 
	 * @param context	A context (a KeyboardService).
	 */
	private void init(Context context) {
		mIME = (KeyboardService) context;

		createPopups(mIME);
		loadWallpaper(context);
	}


	/**
	 * Create the various popup windows used by a primary keyboards.
	 * 
	 * @param context	An instance of KeyboardService.
	 */
	private void createPopups(KeyboardService context) {
		mPopupKeyboardWindow = new PopupWindow(context);
		mPopupKeyboardWindow.setBackgroundDrawable(null);
		mSymMenuWindow = new PopupWindow(context);
		mSymMenuWindow.setBackgroundDrawable(null);
		mSymQuickMenuWindow = new PopupWindow(context);
		mSymQuickMenuWindow.setBackgroundDrawable(null);
		mTranslatorWindow = new PopupWindow(context);
		mTranslatorWindow.setBackgroundDrawable(null);
		mDragonVoiceWindow = new PopupWindow(context);
		mDragonVoiceWindow.setBackgroundDrawable(null);
		mMainMenuWindow = new PopupWindow(context);
		mMainMenuWindow.setBackgroundDrawable(null);
	}

    
    
    /**
     * Sets swipe threshold
     * @param val
     */
    public void setSwipeThreshold(int val){
    	mSwipeThreshold = (int) (val * getResources().getDisplayMetrics().density);
    }

    private void initGestureDetector() {
        mGestureDetector = new SwipeGestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        	
            @Override
            public boolean onScroll(MotionEvent me1, MotionEvent me2, 
                    float distanceX, float distanceY) {
            	
            	if(me1 == null || me2 == null)
            		return false;

                if (mPossiblePoly) return false;
                float deltaX = me2.getX() - me1.getX();
                float deltaY = me2.getY() - me1.getY();
                int travelX = getWidth() / 3; // One-third the keyboard width
                int travelY = getHeight() / 3; // One-third the keyboard height
                mSwipeTracker.computeCurrentVelocity(1000);
                final float velocityX = mSwipeTracker.getXVelocity();
                final float velocityY = mSwipeTracker.getYVelocity();

                if (velocityX > mSwipeThreshold && deltaX > travelX) {
                	swipeRight();
                	return true;
                } else if (velocityX < -mSwipeThreshold && deltaX < -travelX) {
                	swipeLeft();
                	return true;
                } else if (velocityY < -mSwipeThreshold && deltaY < -travelY) {
                	swipeUp();
                	return true;
                } else if (velocityY > mSwipeThreshold && deltaY > travelY) {
                	swipeDown();
                	return true;
                }
                
                return false;
            }
        });

        mGestureDetector.setIsLongpressEnabled(false);
    }
    
    
    private class SwipeGestureDetector extends GestureDetector {
    	
    	private boolean mFlinging = false;

		public SwipeGestureDetector(Context context, OnGestureListener listener) {
			super(context, listener);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev) {

			if(mFlinging) {
				if(ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
					mFlinging = false;
				}
				return true;
			}

			// This method will return true if the user trigged a scroll
			mFlinging = super.onTouchEvent(ev);
			return mFlinging;
		}
    }
    
    

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        mKeyboardActionListener = listener;
    }

    /**
     * Returns the {@link OnKeyboardActionListener} object.
     * @return the listener attached to this keyboard
     */
    protected OnKeyboardActionListener getOnKeyboardActionListener() {
        return mKeyboardActionListener;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard the keyboard to display in this view
     */
    public void setKeyboard(BaseKeyboard keyboard) {
        if (mKeyboard != null) {
            updatePreview(NOT_A_KEY);
        }
        // Remove any pending messages
        removeMessages();
        mKeyboard = keyboard;
        List<Key> keys = mKeyboard.getKeys();
        mKeys = keys.toArray(new Key[keys.size()]);
        requestLayout();
        // Hint to reallocate the buffer if the size changed
        invalidateAllKeys();
        computeProximityThreshold(keyboard);
        
		updateAnyKeySym();
    }

    /**
     * Returns the current keyboard being displayed by this view.
     * @return the currently attached keyboard
     * @see #setKeyboard(Keyboard)
     */
    public BaseKeyboard getKeyboard() {
        return mKeyboard;
    }
    
    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView#isShifted()
     */
    public boolean setShifted(boolean shifted) {
        if (mKeyboard != null) {
            if (mKeyboard.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys();
                return true;
            }
        }
        
		setCapsLockIcon(false);
		return isShifted();
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise. If there is
     * no shift key on the keyboard or there is no keyboard attached, it returns false.
     * @see KeyboardView#setShifted(boolean)
     */
    public boolean isShifted() {
        if (mKeyboard != null) {
            return mKeyboard.isShifted();
        }
        return false;
    }
    
    
    
	public boolean getCapsLock() {
		return mCapsLock;
	}

	public void setCapsLock(boolean state) {
		mCapsLock = state;
	}

    

    /**
     * Enables or disables the key feedback popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled. 
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see #isPreviewEnabled()
     */
    public void setPreviewEnabled(boolean previewEnabled) {
        mShowPreview = previewEnabled;
    }

    /**
     * Returns the enabled state of the key feedback popup.
     * @return whether or not the key feedback popup is enabled
     * @see #setPreviewEnabled(boolean)
     */
    public boolean isPreviewEnabled() {
        return mShowPreview;
    }
    
    public void setVerticalCorrection(int verticalOffset) {
        
    }
    public void setPopupParent(View v) {
        mPopupParent = v;
    }
    
    public void setPopupOffset(int x, int y) {
    	  mPopupKeyboardOffsetX = x;
    	  mPopupKeyboardOffsetY = y;
    	  if (mPreviewPopup.isShowing()) {
            mPreviewPopup.dismiss();
        }
    }

    /**
     * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include key
     * codes for adjacent keys.  When disabled, only the primary key code will be
     * reported.
     * @param enabled whether or not the proximity correction is enabled
     */
    public void setProximityCorrectionEnabled(boolean enabled) {
        mProximityCorrectOn = enabled;
    }

    /**
     * Returns true if proximity correction is enabled.
     */
    public boolean isProximityCorrectionEnabled() {
        return mProximityCorrectOn;
    }


    private CharSequence adjustCase(CharSequence label) {
        if (mKeyboard.isShifted() && label != null && label.length() < 3
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase();
        }
        return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
            int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec);
            }
            setMeasuredDimension(width, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private void computeProximityThreshold(Keyboard keyboard) {
        if (keyboard == null) return;
        final Key[] keys = mKeys;
        if (keys == null) return;
        int length = keys.length;
        int dimensionSum = 0;
        for (int i = 0; i < length; i++) {
            Key key = keys[i];
            dimensionSum += Math.min(key.width, key.height) + key.gap;
        }
        if (dimensionSum < 0 || length == 0) return;
        mProximityThreshold = (int) (dimensionSum * 1.4f / length);
        mProximityThreshold *= mProximityThreshold; // Square it
    }

	/**
	 * Called by the framework when the view size changes. Resizes the popup
	 * windows to match.
	 */
	@Override
	public void onSizeChanged (int w, int h, int oldw, int oldh) {
		PopupWindow popupWindow = getCurrentFullPopupWindow();
		int height = 0;

		if(popupWindow != null) {
			height = getTotalHeight();
			popupWindow.update(w, height);
		}

//		mBuffer = null;
	}


    
	
	
	/*
	 * Key states
	 */

	public String getAnyKeyAction() {
		return mAnyKeyAction;
	}


	/**
	 * Sets any key action for short and long press
	 * 
	 * @param anyKeyAction		simple click
	 * @param anyKeyLongAction	long click
	 */
	public void setAnyKeyAction(String anyKeyAction, String anyKeyLongAction, String returnKeyLongAction) {
		mAnyKeyAction = anyKeyAction;
		mAnyKeyLongAction = anyKeyLongAction;
		mReturnKeyLongAction = returnKeyLongAction;
		updateAnyKeySym();
	}





	/**
	 * Set shift key state based on shift and caps lock states
	 * 
	 * @param caps
	 * @param capsLock
	 * @return
	 */
	public boolean setShifted(boolean caps, boolean capsLock) {
		setShifted(caps || capsLock); // Call the superclass to avoid a
											// recusive loop
		setCapsLockIcon(capsLock);
		return isShifted();
	}



	/**
	 * Set the icon on the shift key to either caps-lock or not
	 * 
	 * @param capsLock	Set true for caps lock.
	 * @return			Returns the new shift state.
	 */
	protected boolean setCapsLockIcon(boolean capsLock) {
		return setShiftKeyIcon(capsLock, true);
	}



	/**
	 * Update the shift key icon.
	 * 
	 * @param capsLock	Set true if caps lock is enabled.
	 * @param redraw	Set true to force a redraw.
	 * @return			Returns the shift key state.
	 */
	protected boolean setShiftKeyIcon(boolean capsLock, boolean redraw) {
		// Find our custom shift key
		BaseKeyboard keyboard = getKeyboard();
		int iShiftKey = keyboard.getShiftKeyIndex();
		if(iShiftKey != -1) {
			Key shiftKey = keyboard.getKeys().get(iShiftKey);

			// Update shift key icon
			if(capsLock == true) {
				shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_SHIFT_LOCKED);
				shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT_LOCKED);
			} else if(keyboard.isShifted()) {
				shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_SHIFT_ON);
				shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT);
			} else {
				shiftKey.icon = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_SHIFT_OFF);
				shiftKey.iconPreview = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SHIFT);
			}

			BaseKeyboard.checkForNulls("setShiftKeyIcon()", shiftKey);

			BaseKeyboard.updateKeyBounds(shiftKey);

			if (redraw) {
				// Redraw the shift key
				invalidateKey(iShiftKey);
			}
		}
		
		return isShifted();
	}




	/*
	 * Methods to open the various PopupWindows
	 */
	
	/**
	 * Opens the symbols quick menu. The quick menu is a one-touch menu with the
	 * most common symbols, and a key to open the main symbols menu.
	 * 
	 * @param key	The key that was pressed to open this menu.
	 */
	protected void openSymQuickMenu(Key key) {
		if(isPopupShowing())
			dismissAllPopupWindows();

		// Create a new keyboard
		PopupKeyboard keyboard = new PopupKeyboard(getContext(), R.xml.sym_quick_menu);
		mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, key);
		mPopupKeyboardView.setOneTouch(true);
		showPopupWindow(mPopupKeyboardWindow, this, Gravity.NO_GRAVITY, mPopupKeyboardRect.left, mPopupKeyboardRect.top);
	}



	protected void openSymMenu() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		if(mSymMenuLayout == null) {
			// Inflate the translator view
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mSymMenuLayout = (SymMenuLayout) layoutInflater.inflate(R.layout.sym_menu_layout, null);
			mSymMenuLayout.createKeyboards(this, getOnKeyboardActionListener());

			mSymMenuWindow.setContentView(mSymMenuLayout);
		}

		mSymMenuWindow.setWidth(getWidth());
		mSymMenuWindow.setHeight(getTotalHeight());
		showPopupWindow(mSymMenuWindow, this, Gravity.BOTTOM, 0, 0);
	}



	protected void openTranslator() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		if(mTranslatorView == null) {
			// Inflate the translator view
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTranslatorView = (TranslatorView) layoutInflater.inflate(R.layout.translator, null);
			mTranslatorView.onCreate();
			mTranslatorWindow.setContentView(mTranslatorView);
		}

		mTranslatorWindow.setWidth(getWidth());
		mTranslatorWindow.setHeight(getTotalHeight());
		showPopupWindow(mTranslatorWindow, this, Gravity.BOTTOM, 0, 0);
	}



	protected void openDragonVoice() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		if(mDragonVoiceView == null) {
			// Inflate the voice input view
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mDragonVoiceView = (DragonVoiceView) layoutInflater.inflate(R.layout.voice_dialog, null);
			mDragonVoiceWindow.setContentView(mDragonVoiceView);
		}

		mDragonVoiceWindow.setWidth(getWidth());
		mDragonVoiceWindow.setHeight(getTotalHeight());
		showPopupWindow(mDragonVoiceWindow, this, Gravity.BOTTOM, 0, 0);

		mDragonVoiceView.startVoiceInput();
	}



	/**
	 * Creates and displays the popup keyboard. Called by onLongPress() when the
	 * user long-presses a key with more than one alternate character.
	 * 
	 * @param anyKey	The Any Key
	 */
	protected void openMainMenu() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		if(mMainMenuView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mMainMenuView = (FrameLayout) layoutInflater.inflate(R.layout.main_menu, null);
			mMainMenuWindow.setContentView(mMainMenuView);
		}
		mMainMenuWindow.setWidth(getWidth());
		mMainMenuWindow.setHeight(getTotalHeight());

		showPopupWindow(mMainMenuWindow, this, Gravity.BOTTOM, 0, 0);

		invalidateAllKeys();
	}




	/*
	 * Methods to open the various popup keyboards
	 */

	/**
	 * Opens the smiley one-touch menu above the smiley key.
	 */
	protected void openSmileyKeyboard() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		// Create a new keyboard
		PopupKeyboard keyboard = new PopupKeyboard(getContext(), R.xml.smiley_menu);
		mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, getKeyboard().getSmileyKey());
		mPopupKeyboardView.setOneTouch(true);
		showPopupWindow(mPopupKeyboardWindow, this, Gravity.NO_GRAVITY, mPopupKeyboardRect.left, mPopupKeyboardRect.top);
	}



	protected void openUrlKeyboard() {
		if(isPopupShowing())
			dismissAllPopupWindows();

		// Create a new keyboard
		PopupKeyboard keyboard = new PopupKeyboard(getContext(), R.xml.url_menu);
		mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, getKeyboard().getAnyKey());
		mPopupKeyboardView.setOneTouch(true);
		mPopupKeyboardView.setNearestKey(-201);
		showPopupWindow(mPopupKeyboardWindow, this, Gravity.NO_GRAVITY, mPopupKeyboardRect.left, mPopupKeyboardRect.top);

		invalidateAllKeys();
	}



	/*
	 * PopupWindow methods for menus and keyboards
	 */

	/**
	 * Show a popup window.
	 * 
	 * @param popWin	The PopupWindow to show.
	 * @param parent	The view to assign as popWin's parent.
	 * @param gravity	The gravity to apply to popWin.
	 * @param x			The x location for popWin.
	 * @param y			The y location for popWin.
	 */
	public void showPopupWindow(PopupWindow popWin, View parent, int gravity, int x, int y) {
		popWin.showAtLocation(parent, gravity, x, y);
		mLastPopupWindow = popWin;
	}


	/**
	 * Get the currently displayed PopupWindow.
	 * 
	 * @return	The current PopupWindow.
	 */
	public PopupWindow getLastPopupWindow() {
		return mLastPopupWindow;
	}



	/**
	 * Opens a popup keyboard that contains only characters.
	 * 
	 * @param key	The key that triggered the keyboard
	 */
	protected void openPopupCharacterKeyboard(Key key, boolean oneTouch) {
		// Create a new keyboard
		CharSequence popupCharacters = key.popupCharacters;
		PopupKeyboard keyboard = new PopupKeyboard(getContext(),
				key.popupResId, popupCharacters, -1, getPaddingLeft()
						+ getPaddingRight());

		// Assemble the keyboard view
		mPopupKeyboardRect = assemblePopupKeyboard(R.layout.popup_keyboard_layout, keyboard, key);

		// Check if popup keyboard fits above-right key
		if(mPopupKeyboardRect.right <= key.x) {
			// Popup is located to left of key. Reverse the key order.
			StringBuilder reversePopupCharacters = new StringBuilder( popupCharacters.subSequence(0, popupCharacters.length()));
			reversePopupCharacters.reverse();
			keyboard = new PopupKeyboard(getContext(), key.popupResId, reversePopupCharacters, -1, getPaddingLeft() + getPaddingRight());
			mPopupKeyboardView.setKeyboard(keyboard);
		}

		// Set default key. This key is highlighted first.
		mPopupKeyboardView.setOneTouch(oneTouch);
		mPopupKeyboardView.setNearestKey((int) popupCharacters.charAt(0));		

		// Show the keyboard
		showPopupWindow(mPopupKeyboardWindow, this, Gravity.NO_GRAVITY, mPopupKeyboardRect.left, mPopupKeyboardRect.top);
		
	}
	// Convenience method
	protected void openPopupCharacterKeyboard(Key key) {
		openPopupCharacterKeyboard(key, true);
	}


	/**
	 * Fills a PopupWindow with a keyboard, positions it relative to a key, and
	 * returns the resulting bounds.
	 * 
	 * @param layoutViewId	The id of view that holds the keyboard.
	 * @param keyboard		The keyboard to pop.
	 * @param key			The key that triggered the keyboard.
	 * @return				A Rect containing the bounds of the PopupWindow.
	 */
	protected Rect assemblePopupKeyboard(int layoutViewId, BaseKeyboard keyboard, Key key) {

		if(mPopupKeyboardWindow.isShowing())
			dismissAllPopupWindows();

		// Inflate the popup keyboard container
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPopupKeyboardLayout = (PopupKeyboardLayout) layoutInflater.inflate(layoutViewId, null);
		mPopupKeyboardView = (PopupKeyboardView) mPopupKeyboardLayout.getKeyboardView();

		// Assign the keyboard
		mPopupKeyboardView.setKeyboard(keyboard);
		mPopupKeyboardView.setPopupParent(this);
		mPopupKeyboardView.setOnKeyboardActionListener(getOnKeyboardActionListener());
		mPopupKeyboardView.setShifted(isShifted());

		// Fill in the popup keyboard window
		Rect popupRect = assemblePopupWindow(mPopupKeyboardWindow, mPopupKeyboardLayout);
		popupRect = positionWindowAboveKey(popupRect, key);

		mPopupKeyboardView.setPopupOffset(0, mIME.getCandidateHeight() + popupRect.top);

		// Return the bounds of the window
		return popupRect;
	}



	/**
	 * Fills a PopupWindow with content, resizes it to fit and returns the
	 * resulting bounds.
	 * 
	 * @param window		The PopupWindow to fill.
	 * @param contentView	The content View to fill it with.
	 * @param x				The absolute x location of the top-left corner of the window.
	 * @param y				The absolute y location of the top-left corner of the window.
	 * @return				A Rect containing the bounds of the PopupWindow.
	 */
	protected Rect assemblePopupWindow(PopupWindow window, View contentView) {
		// Size and lay out all views
		contentView.measure(
				MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
				MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
		int layoutMeasuredWidth = contentView.getMeasuredWidth();
		int layoutMeasuredHeight = contentView.getMeasuredHeight();
		// Display popup just above and to the right of the key
		window.setContentView(contentView);
		window.setWidth(layoutMeasuredWidth);
		window.setHeight(layoutMeasuredHeight);

		// Return the bounds of mPopupKeyboardWindow
		return new Rect(0, 0, layoutMeasuredWidth, layoutMeasuredHeight);
	}



	/**
	 * Convenience function for assemblePopupWindow() that takes a view id
	 * instead of a View.
	 * 
	 * @param window			The PopupWindow to fill.
	 * @param contentViewId		The view id to fill it with.
	 * @return					A Rect containing the bounds of the PopupWindow.
	 */
	protected Rect assemblePopupWindow(PopupWindow window, int contentViewId) {
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View contentView = layoutInflater.inflate(contentViewId, null);

		return assemblePopupWindow(window, contentView);
	}



	/**
	 * Finds the best place to position a popup keyboard above a key.
	 * 
	 * @param windowRect	The initial bounds of the PopupWindow.
	 * @param key			The key to position it above.
	 * @return				The new bounds of the PopupWindow.
	 */
	private Rect positionWindowAboveKey(Rect windowRect, Key key) {
		Rect newRect = new Rect();
		int width = windowRect.right - windowRect.left;
		int height = windowRect.bottom - windowRect.top;

		// Convert key coordinates from relative to absolute
		final int windowOffset[] = new int[2];
		getLocationInWindow(windowOffset);
		final int x = key.x + windowOffset[0];
		final int y = key.y + windowOffset[1];

		// Position window just above key
		newRect.bottom = y - key.height / 4;
		newRect.top = newRect.bottom - height;

		// Choose horizontal position
		int displayWidth = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		if(width < displayWidth - x) {
			// Position window to right of key
			newRect.left = x;
			newRect.right = newRect.left + width;
		} else {
			// Position window to left of key
			newRect.right = x;
			newRect.left = newRect.right - width;
			
			if(newRect.left < 0) {
				// Window doesn't fit on left either. Left align it with screen instead.
				newRect.right -= newRect.left;
				newRect.left = 0;
			}
		}

		return newRect;
	}



	/**
	 * Checks if any PopupWindows are showing.
	 * 
	 * @return	true if any PopupWindow is showing.
	 */
	protected boolean isPopupShowing() {
		return mPopupKeyboardWindow.isShowing()
				|| mDragonVoiceWindow.isShowing()
				|| mSymMenuWindow.isShowing()
				|| mSymQuickMenuWindow.isShowing()
				|| mTranslatorWindow.isShowing()
				|| mMainMenuWindow.isShowing();
	}


	/**
	 * Gets the currently showing PopupWindow. Assumes only one popup is visible
	 * at a time.
	 * 
	 * @return	The currently showing PopupWindow.
	 */
	protected PopupWindow getCurrentFullPopupWindow() {
		if (mTranslatorWindow.isShowing())
			return mTranslatorWindow;

		if (mMainMenuWindow.isShowing())
			return mMainMenuWindow;

		if (mDragonVoiceWindow.isShowing())
			return mDragonVoiceWindow;

		return null;
	}


	/**
	 * Close all open popup windows
	 */
	protected void dismissAllPopupWindows() {
		dismissAllPopupWindows(false);
	}
	protected void dismissAllPopupWindows(boolean rememberLastWindow) {
		if (mPopupKeyboardWindow.isShowing())
			mPopupKeyboardWindow.dismiss();
		if (mSymMenuWindow.isShowing())
			mSymMenuWindow.dismiss();
		if (mSymQuickMenuWindow.isShowing())
			mSymQuickMenuWindow.dismiss();
		if (mTranslatorWindow.isShowing())
			mTranslatorWindow.dismiss();
		if (mDragonVoiceWindow.isShowing())
			mDragonVoiceWindow.dismiss();
		if (mMainMenuWindow.isShowing())
			mMainMenuWindow.dismiss();

		if (!rememberLastWindow)
			mLastPopupWindow = null;
	}



	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Gets any key code
	 * 
	 * @param state
	 * @return
	 */
	protected int getFnKeyCode(String state) {
		int primaryCode = 0;
		if (state.equals(getContext().getString(
				R.string.any_key_action_id_smiley_menu))) {
			primaryCode = BaseKeyboard.KEYCODE_SMILEY;
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_arrow_keypad))) {
			primaryCode = BaseKeyboard.KEYCODE_ARROWS;
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_voice_input))) {
			primaryCode = BaseKeyboard.KEYCODE_VOICE;
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_translator))) {
			primaryCode = BaseKeyboard.KEYCODE_TRANSLATE;
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_locale))) {
			primaryCode = BaseKeyboard.KEYCODE_LOCALE;
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_settings))) {
			primaryCode = BaseKeyboard.KEYCODE_SETTINGS;
		} else {
			throw new IllegalArgumentException("Can not detect any key code");
		}    

		return primaryCode;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
    
    
    
    
    
    
    
    
    
	/*
	 * Keyboard drawing methods. Many of these methods override KeyboardView
	 * because we use a highly customized keyboard look. As a result some
	 * methods are copied exactly from Android source.
	 */

	/**
	 * Called by the framework to re-draw the keyboard.
	 */
	@Override
	public void onDraw(Canvas canvas) {
		if (getWidth() <= 0 || getHeight() <= 0)
			return;

		if (mDrawPending || mBuffer == null) {
			if (mBuffer == null || mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight()) {

				final int width = getWidth();
				final int height = getHeight();

				if(width <= 0 || height <= 0)
					// KeyboardView is not ready
					return;

				// Create a new buffer if anything changed
				mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				mCanvas = new Canvas(mBuffer);
				setPhotoWallpaperBounds();

				invalidateAllKeys();
			}

			mCanvas.clipRect(mDirtyRect, Op.REPLACE);
			drawKeyboard(mCanvas);
			mDirtyRect.setEmpty();
		}
		canvas.drawBitmap(mBuffer, 0, 0, null);
	}



	/**
	 * Draws the keyboard to a buffer.
	 */
	protected void drawKeyboard(Canvas canvas) {
		if (getKeyboard() == null)
			return; // Oops!

		// Set default text alignment
		mPaint.setTextAlign(Paint.Align.CENTER);

		// Set clip region and padding
		final Rect clipRegion = new Rect(0, 0, 0, 0);
		final Rect kbPadding = new Rect(getPaddingLeft(), getPaddingTop(),
				getPaddingRight(), getPaddingBottom());
		final Object[] keys = getKeyboard().getKeys().toArray();
		final Key invalidKey = mInvalidatedKey;

		boolean drawSingleKey = false;
		if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
			// Is clipRegion completely contained within the invalidated key?
			if (invalidKey.x + kbPadding.left - 1 <= clipRegion.left
					&& invalidKey.y + kbPadding.top - 1 <= clipRegion.top
					&& invalidKey.x + invalidKey.width + kbPadding.left + 1 >= clipRegion.right
					&& invalidKey.y + invalidKey.height + kbPadding.top + 1 >= clipRegion.bottom) {
				drawSingleKey = true;
			}
		}
		
		// Draw the keyboard background
		drawBackground(canvas);

		// Draw the keys
		final int keyCount = keys.length;
		final Rect keyPadding = new Rect(); 
		for (int i = 0; i < keyCount; i++) {
			final Key key = (Key) keys[i];
			if (drawSingleKey && invalidKey != key) {
				continue;
			}

			// Draw the key background
			drawKeyBackground(key, canvas, kbPadding, keyPadding);

			// Draw superscript label
			if(mIME.isDrawSuperLabel(key))
				drawSuperLabel(key, keyPadding, canvas);


			if (key.label != null) {
				// Draw a label
				drawLabel(key, canvas, kbPadding, keyPadding);
			} else {
				updateKeyIcon(key);
				// Paint an icon
				if (key.icon != null) {
					drawIcon(key, canvas, kbPadding, keyPadding);					

					if(key == getKeyboard().getAnyKey() && mAnyKeyLongAction != null){
						// Draw super icon for any key long press action
						Drawable icon = getAnyKeyIcon(mAnyKeyLongAction, true);

						if(!mIME.mURL && icon != null)
							drawSuperIcon(key, canvas, kbPadding, keyPadding, icon);
					}
				}	
			}
			
			if(key == getKeyboard().getActionKey() && mReturnKeyLongAction != null ){
				// Draw super icon for return key long press action
				Drawable icon = getAnyKeyIcon(mReturnKeyLongAction, true);

				if(icon != null)
					drawSuperIcon(key, canvas, kbPadding, keyPadding, icon);
			}	
		}
		mInvalidatedKey = null;
		/*
		 * // Overlay a dark rectangle to dim the keyboard if
		 * (mPopupKeyboardWindow.isShowing()) { paint.setColor((int)
		 * (mBackgroundDimAmount * 0xFF) << 24); canvas.drawRect(0, 0,
		 * getWidth(), getHeight(), paint); }
		 */
		mDrawPending = false;
	}
	
                  
	/**
	 * Get icon for short or long state action
	 * 
	 * @param state
	 * @param superEnabled
	 * @return
	 */
	protected Drawable getAnyKeyIcon(String state, boolean superEnabled){
		Drawable icon = null;

		KeyboardTheme theme = KeyboardThemeManager.getCurrentTheme(); 

		if (state.equals(getContext().getString(
				R.string.any_key_action_id_smiley_menu))) {
			// Smiley
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_SMILEY
					: KeyboardTheme.KEY_SYM_SMILEY);
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_arrow_keypad))) {
			// Arrow keypad
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_ARROWS
					: KeyboardTheme.KEY_SYM_ARROWS);
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_voice_input))) {
			// Voice input
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_MIC
					: KeyboardTheme.KEY_SYM_MIC);
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_translator))) {
			// Translator
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_TRANSLATE
					: KeyboardTheme.KEY_SYM_TRANSLATE);
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_locale))) {
			// Language
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_LOCALE
					: KeyboardTheme.KEY_SYM_LOCALE);
		} else if (state.equals(getContext().getString(
				R.string.any_key_action_id_settings))) {
			// Settings
			icon = theme.getDrawable(superEnabled
					? KeyboardTheme.KEY_SYM_SUPER_SETTINGS
					: KeyboardTheme.KEY_SYM_SETTINGS);
		}  

		return icon;

	}


	/**
	 * Gets any super icon
	 * 
	 * @param state
	 * @return
	 */
	protected Drawable getAnyKeySuperIcon(String state){
	  return getAnyKeyIcon(state, true);
	}


	/*
	 * Keyboard background drawing methods.
	 */

	/**
	 * Draw the keyboard background. This could be a photo drawable or a theme
	 * background (drawable or a solid color).
	 * 	
	 * @param canvas	The canvas to draw on.
	 */
	protected void drawBackground(Canvas canvas) {
		if(mPhotoBackground == null) {
			Drawable background = KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KB_BG);
			if(background != null) {
				// Theme backgrounds are always cropped
				mBackgroundFit = false;
				final Rect bounds = getWallpaperBounds(background);
				background.setBounds(bounds);
				background.draw(canvas);
			} else
				// No background image. Use solid color.
				canvas.drawColor(KeyboardThemeManager.getCurrentTheme().getBGColor());
		} else {
			mPhotoBackground.draw(canvas);
		}
	}



	/**
	 * Set the bounds of a photo wallpaper.
	 */
	protected void setPhotoWallpaperBounds() {
		if(mPhotoBackground != null)
			mPhotoBackground.setBounds(getPhotoWallpaperBounds());
	}

	
	
	
	protected Rect getPhotoWallpaperBounds() {
		return getWallpaperBounds(mPhotoBackground);
	}


	/**
	 * Compute the bounds rect for the photo wallpaper, based on keyboard size
	 * and stretch/fit flag. If fit == true, the photo is resized to fix
	 * exactly, which will change the aspect. If not, the photo will be resized
	 * in only one dimension, and cropped. This is usually the preferred way.
	 * 
	 * @return	A Rect containing the bounds of the wallpaper drawable 
	 */
	private final Rect mWallpaperRect = new Rect();
	protected Rect getWallpaperBounds(Drawable wallpaper) {
		if(mBackgroundFit) {
			mWallpaperRect.left = 0;
			mWallpaperRect.top = 0;
			mWallpaperRect.right = getWidth();
			mWallpaperRect.bottom = getHeight();
			return mWallpaperRect;
		}

		// Get the various dimensions and ratios.
		final float kbWidth = getWidth();
		final float kbHeight = getHeight();
		final float kbRatio = ((float) kbWidth) / ((float) kbHeight);
		final float wpRatio = ((float) wallpaper.getIntrinsicWidth())
				/ ((float) wallpaper.getIntrinsicHeight());

		if(wpRatio > kbRatio) {
			// Background has a wider aspect ratio than content area. crop to
			// width, fit to height.
			final float sizeRatio = kbHeight
					/ (float) wallpaper.getIntrinsicHeight();
			final float wpWidth = wallpaper.getIntrinsicWidth()
					* sizeRatio;
			mWallpaperRect.left = (int) ((kbWidth - wpWidth) / 2);
			mWallpaperRect.top = 0;
			mWallpaperRect.right = (int) (kbWidth + (wpWidth - kbWidth) / 2);
			mWallpaperRect.bottom = (int) kbHeight;
		} else {
			// Background has a narrower aspect ratio than content area. Crop to
			// height, fit to
			// width.
			final float sizeRatio = kbWidth
					/ (float) wallpaper.getIntrinsicWidth();
			float wpHeight = wallpaper.getIntrinsicHeight();
			wpHeight = wpHeight * sizeRatio;
			mWallpaperRect.left = 0;
			mWallpaperRect.top = (int) ((kbHeight - wpHeight) / 2);
			mWallpaperRect.right = (int) kbWidth;
			mWallpaperRect.bottom = (int) (kbHeight + (wpHeight - kbHeight) / 2);
		}

		return mWallpaperRect;
	}


	/*
	 * Key background drawing methods.
	 */

	/**
	 * Draw the background on a key
	 * 
	 * @param background	The background drawable to use
	 * @param bounds		The bounds to respect
	 * @param canvas		The canvas to paint on
	 */
	private final Rect mKeyBounds = new Rect();
	protected void drawKeyBackground(Key key, Canvas canvas, Rect kbPadding, Rect keyPadding) {
		Drawable background = getKeyBackground(key);
		int[] drawableState = key.getCurrentDrawableState();
		background.setState(drawableState);
		background.getPadding(keyPadding);
		
		mKeyBounds.left = kbPadding.left + key.x;
		mKeyBounds.top = kbPadding.top + key.y;
		mKeyBounds.right = kbPadding.left + key.x + key.width;
		mKeyBounds.bottom =  kbPadding.top + key.y + key.height;

		background.setBounds(mKeyBounds);
		background.draw(canvas);
	}



	/**
	 * Get the drawable for a key background.
	 * 
	 * @param key	A key.
	 * @return		The background drawable to use for key.
	 */
	protected Drawable getKeyBackground(Key key) {
		Drawable background = null;
		String details;

		if(key.codes[0] < 0) {
			// Special background for function keys
			background = getFnKeyBackground(key);
			details = "getFnKeyBackground(key.codes[0] < 0)"; 
		} else {
			if (KeyboardThemeManager.getCurrentTheme().getThemeName().equals("ICS")
					&& (key.codes[0] == 44 || key.codes[0] == 46 || key.codes[0] == 32)){
				// ICS THEME HACK
				background = getFnKeyBackground(key);
				details = "getFnKeyBackground(ICS)";
			} else {
				// Return the default key background.
				background = KeyboardThemeManager.getCurrentTheme()
						.getDrawable(KeyboardTheme.KEY_NORMAL);
				details = "getFnKeyBackground()";
			}
		}
		
		// Send error report
		if(background == null) {
			details +=  " with key codes " + Arrays.toString(key.codes) + " key == getAnyKey() " + (key == getKeyboard().getAnyKey());
			ErrorReport.reportShortError(mIME, "getKeyBackground", details);
		}
		Assert.assertTrue(background != null);

		// Set alpha
		if(mPhotoBackground != null)
			// Use custom alpha
			background.setAlpha(mPhotoKeyBGAlpha);
		else
			// Use theme alpha
			background.setAlpha(KeyboardThemeManager.getCurrentTheme().getBGAlpha());

		return background;
	}



	/**
	 * Get the drawable for a function key background.
	 * 
	 * @param key
	 *            A function key.
	 * @return		The background drawable to use for key.
	 */
	protected Drawable getFnKeyBackground(Key key) {
		if(key == getKeyboard().getAnyKey()) {
			// The Any Key uses a special background.
			Drawable background = KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KEY_ANY);
			background.setAlpha(mPhotoKeyBGAlpha);
			return background;
		}
		else
			// Return the default function key background.
			return KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.KEY_FN_NORMAL);
	}



	/*
	 * Key label drawing methods.
	 */

	/**
	 * Draws a label on a key.
	 * 
	 * @param key			The key to draw a label on
	 * @param textSize		Text size if it is a character (i.e. printing) key
	 * @param labelSize		Text size if it a label (i.e. non-printing) key
	 * @param color			Text color
	 * @param canvas		The canvas to paint on
	 * @param paint			The paintbrush to use
	 * @param kbPadding		Keyboard padding
	 * @param keyPadding	Key padding
	 */
	protected void drawLabel(Key key, Canvas canvas, Rect kbPadding, Rect keyPadding) {
		// Switch the character to uppercase if shift is pressed
		String label = getLabel(key);

		float textSize = getResources().getDimension(R.dimen.key_text_size)
				* getKeyboard().getKeyScale().getRowDefault();
		float labelSize = getResources().getDimension(R.dimen.label_text_size)
				* getKeyboard().getKeyScale().getRowDefault();

		if(key.edgeFlags == Keyboard.EDGE_BOTTOM){
			textSize = getResources().getDimension(R.dimen.key_text_size)
					* getKeyboard().getKeyScale().getRowBottom();
			labelSize = getResources().getDimension(R.dimen.label_text_size)
					* getKeyboard().getKeyScale().getRowBottom();
		}

		int color = KeyboardThemeManager.getCurrentTheme().getKeyFGColor();

		mPaint.setColor(color);
		mPaint.setTextAlign(Paint.Align.CENTER);
		// For characters, use large font. For labels like "Done", use small font.
		if (label.length() > 1 && key.codes.length < 2) {
			mPaint.setTextSize(labelSize);
			mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			mPaint.setTextSize(textSize);
//			mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		}

		// Draw the text
		final float labelX = getLabelX(key, kbPadding, keyPadding);
		final float labelY = getLabelY(key, kbPadding, keyPadding);
		canvas.drawText(label, labelX, labelY, mPaint);
		// Turn off drop shadow
		mPaint.setShadowLayer(0, 0, 0, 0);
	}



	/**
	 * Get the label for a key.
	 * 
	 * @param key	A key.
	 * @return		The label for key.
	 */
	protected String getLabel(Key key) {
		if(key.label != null && getKeyboard().isShifted() && key.codes[0] > 0)
			return key.label.toString().toUpperCase();
		else if(key.label != null)
			return key.label.toString();

		return null;
	}    

	/**
	 * Get the x-position for a centered key label, relative to the keyboard.
	 * 
	 * @param key			A key.
	 * @param kbPadding		The keyboard padding.
	 * @param keyPadding	The key padding.
	 * @return				The x-position, relative to the keyboard.
	 */
	protected float getLabelX(Key key, Rect kbPadding, Rect keyPadding) {
		return key.x + kbPadding.left
				+ (key.width - keyPadding.left - keyPadding.right) / 2
				+ keyPadding.left;
	}


	protected float getLabelY(Key key, Rect kbPadding, Rect keyPadding) {
		return getLabelBottomY(key, kbPadding, keyPadding);
	}


	/**
	 * Get the y-position for a centered key label, relative to the keyboard.
	 * 
	 * @param key			A key.
	 * @param kbPadding		The keyboard padding.
	 * @param keyPadding	The key padding.
	 * @param paint			The paint brush that will be used to paint the label.
	 * @return				The x-position, relative to the keyboard.
	 */
	protected float getLabelCenterY(Key key, Rect kbPadding, Rect keyPadding) {
		return key.y + kbPadding.top
				+ (key.height - keyPadding.top - keyPadding.bottom) / 2
				+ (mPaint.getTextSize() - mPaint.descent()) / 2
				+ keyPadding.top;
	}


	protected float getLabelBottomY(Key key, Rect kbPadding, Rect keyPadding) {
		return key.y + key.height - keyPadding.bottom - mPaint.descent();
	}


	/**
	 * Draw a superscript label above the main label on a key.
	 * 
	 * @param key		The key to draw on
	 * @param size		Text size
	 * @param color		Text color
	 * @param canvas	The canvas to paint on
	 * @param paint		The paintbrush to use
	 */
	protected void drawSuperLabel(Key key, Rect keyPadding, Canvas canvas) {
		String superLabel = getSuperLabel(key);
		if(superLabel.length() == 0)
			return;
		
		float size = getResources().getDimension(R.dimen.key_super_label_text_size)
				* getKeyboard().getKeyScale().getRowDefault();

		if(key.edgeFlags == Keyboard.EDGE_BOTTOM)
			size = getResources().getDimension(R.dimen.key_super_label_text_size)
				* getKeyboard().getKeyScale().getRowBottom();

		int color = KeyboardThemeManager.getCurrentTheme().getKeySuperFGColor();
		float horizOffset = KeyboardThemeManager.getCurrentTheme().getSuperHorizOffset();

		mPaint.setTypeface(Typeface.DEFAULT);

		// Calculate vertical location of label
		float verticalGap = getResources().getDimension(R.dimen.kb_gap_vertical)
			* getKeyboard().getKeyScale().getRowDefault();
		if(key.edgeFlags == Keyboard.EDGE_BOTTOM) {
			verticalGap = getResources().getDimension(R.dimen.kb_gap_vertical)
				* getKeyboard().getKeyScale().getRowBottom();
		}
		float vertOffset = verticalGap + size;

		// Calculate horizontal location of label
		if(horizOffset == 0) {
			// Center
			mPaint.setTextAlign(Paint.Align.CENTER);
			horizOffset = key.x + (key.width/2);
		} else if(horizOffset > 0) {
			// Right-align
			mPaint.setTextAlign(Paint.Align.RIGHT);
			horizOffset = key.x + key.width - keyPadding.right - horizOffset;
		} else if(horizOffset < 0) {
			// Left-align
			mPaint.setTextAlign(Paint.Align.LEFT);
			horizOffset = key.x + keyPadding.left - horizOffset;
		}

		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mPaint.setFakeBoldText(true);
		if (KeyboardThemeManager.getCurrentTheme().getThemeName().equals("ICS")) {
			// ICS THEME HACK
			if (key.codes[0] < 0 || key.codes[0] == 44 || key.codes[0] == 46
					|| key.codes[0] == 32)
				color = KeyboardTheme.icsFnSuperColor;
		}

		mPaint.setTextSize(size);
		mPaint.setColor(color);
		canvas.drawText(superLabel, horizOffset, key.y + vertOffset, mPaint);
	}



	/**
	 * Returns the superscript label to draw at the top of this key
	 * 
	 * @param key		The key to check
	 * @param paint		The paintbrush we are using. This method sets the font weight
	 *            		based on the type of label (bold for labels, regular for
	 *            		characters).
	 * @return			The superscript label
	 */
	protected String getSuperLabel(Key key) {
		String superLabel = "";

		if(key == getKeyboard().getAnyKey()) {
			if(mIME.mURL)
				// User is editing an URL field.
				superLabel = "URL";
		} else if (key.popupCharacters != null
				&& key.popupCharacters.length() > 2
				&& key.popupCharacters.charAt(0) == '{'
				&& key.popupCharacters.charAt(key.popupCharacters.length() - 1) == '}') {
			// popupCharacters wrapped in {} are actually labels
			superLabel = key.popupCharacters.toString().substring(1, key.popupCharacters.length() - 1);
		} else if (key.popupCharacters != null
				&& key.popupCharacters.length() > 0
				&& showSuperChar(key.popupCharacters.charAt(0))) {
			// Use the first non-letter popup key as the superscript label
			for(int iChar = 0; iChar < key.popupCharacters.length(); iChar++) {
				char c = key.popupCharacters.charAt(iChar);
				if(!Character.isLetter(c) || c == 'ß') {
					superLabel = Character.toString(c);
					break;
				}
			}
		}

		// Set shift state
		if(isShifted())
			superLabel = superLabel.toUpperCase();

		return superLabel;
	}



	/**
	 * Checks if a character should be displayed as a super-label.
	 * 
	 * @param c		The character to check.
	 * @return		true if c is a valid super-label.
	 */
	protected boolean showSuperChar(char c) {
		if ((Character.isLetter(c) || c == 'ß')
				&& !KeyboardLayout.getCurrentLayout().showSuperLetters())
			return false;

		return true;
	}



	/*
	 * Key icon drawing methods.
	 */

	/**
	 * Draw an icon on a key.
	 * 
	 * @param key			The key to draw a label on
	 * @param canvas		The canvas to paint on
	 * @param kbPadding		Keyboard padding
	 * @param keyPadding	Key padding
	 */
	private final Rect mIconRect = new Rect();
	protected void drawIcon(Key key, Canvas canvas, Rect kbPadding,Rect keyPadding) {
		int size = (int) (getResources().getDimension(R.dimen.key_text_size)
				* getKeyboard().getKeyScale().getRowDefault());
		if(key.edgeFlags == Keyboard.EDGE_BOTTOM)
			size = (int) (getResources().getDimension(R.dimen.key_text_size)
					* getKeyboard().getKeyScale().getRowBottom());
		
		if(hasSuper(key)) {
			// Bottom align icon to content area.
			mIconRect.left = kbPadding.left + key.x + (key.width - size) / 2;
			mIconRect.right = mIconRect.left + size;
			mIconRect.bottom = kbPadding.top + key.y + key.height - keyPadding.bottom;
			mIconRect.top = mIconRect.bottom - size;
		} else {
			// Center icon to key.
			mIconRect.left = kbPadding.left + key.x + (key.width - size) / 2;
			mIconRect.right = mIconRect.left + size;
			mIconRect.top = kbPadding.top + key.y + (key.height - size) / 2;
			mIconRect.bottom = mIconRect.top + size;
		}

		key.icon.setBounds(mIconRect);
		key.icon.draw(canvas);
	}
	
	
	
	protected boolean hasSuper(Key key) {
		// Check for super-label.
		if(getSuperLabel(key).length() > 0)
			return true;

		// Any Key as a super icon.
		if(key == getKeyboard().getAnyKey() || key == getKeyboard().getActionKey())
			return true;
		
		return false;
	}

	
	
	/**
	 * Draw superscript icon on a key.
	 * 
	 * @param key			The key to draw a label on
	 * @param canvas		The canvas to paint on
	 * @param kbPadding		Keyboard padding
	 * @param keyPadding	Key padding
	 * @param icon			Key Icon
	 */
	private final Rect mSuperIconRect = new Rect();
	protected void drawSuperIcon(Key key, Canvas canvas, Rect kbPadding, Rect keyPadding, Drawable icon) {
		KeyboardTheme theme = KeyboardThemeManager.getCurrentTheme();
		
		int size = (int) (getResources().getDimension(R.dimen.key_super_label_text_size)
				* getKeyboard().getKeyScale().getRowDefault());

		int horizOffset = theme.getSuperHorizOffset();
		// Calculate horizontal location of label
		if(horizOffset == 0) {
			// Center
			mSuperIconRect.right = kbPadding.left + key.x + (key.width + size) / 2;
			mSuperIconRect.left = mSuperIconRect.right - size;
		} else if(horizOffset > 0) {
			// Right-align
			mSuperIconRect.right = kbPadding.left + key.x + key.width - keyPadding.right - horizOffset;
			mSuperIconRect.left = mSuperIconRect.right - size;
		} else if(horizOffset < 0) {
			// Left-align
			mSuperIconRect.left = kbPadding.left + key.x + keyPadding.left - horizOffset;
			mSuperIconRect.right = mSuperIconRect.left + size;
		}

		mSuperIconRect.top = kbPadding.top + key.y + keyPadding.top;
		mSuperIconRect.bottom = mSuperIconRect.top + size;

		icon.setBounds(mSuperIconRect);
		icon.draw(canvas);
	}	




	/**
	 * Called to update all key icons, in case the theme changed.
	 * 
	 * @param key	The key to update
	 */
	protected void updateKeyIcon(Key key) {
		// TODO: This is probably very inefficient. :(
		if(key == getKeyboard().getAnyKey()) {
			updateAnyKeySym();
		} else if(key == getKeyboard().getActionKey()) {
			// The action key icon is handled by BaseKeyboard.setImeOptions()
			return;
		} else {
			_updateKeyIcon(key, R.integer.keycode_arrow_left,
					KeyboardTheme.KEY_SYM_ARROW_LEFT);
			_updateKeyIcon(key, R.integer.keycode_arrow_right,
					KeyboardTheme.KEY_SYM_ARROW_RIGHT);
			_updateKeyIcon(key, R.integer.keycode_arrow_down,
					KeyboardTheme.KEY_SYM_ARROW_DOWN);
			_updateKeyIcon(key, R.integer.keycode_arrow_up,
					KeyboardTheme.KEY_SYM_ARROW_UP);
			_updateKeyIcon(key, R.integer.keycode_arrow_back,
					KeyboardTheme.KEY_SYM_ARROW_BACK);
			_updateKeyIcon(key, R.integer.keycode_arrow_next,
					KeyboardTheme.KEY_SYM_ARROW_NEXT);

			setShiftKeyIcon(getCapsLock(), false);
			_updateKeyIcon(key, R.integer.keycode_delete,
					KeyboardTheme.KEY_SYM_DELETE);
			_updateKeyIcon(key, R.integer.keycode_space,
					KeyboardTheme.KEY_SYM_SPACE);

			_updateKeyIcon(key, R.integer.keycode_arrows,
					KeyboardTheme.KEY_SYM_ARROWS);
			_updateKeyIcon(key, R.integer.keycode_voice,
					KeyboardTheme.KEY_SYM_MIC);
			_updateKeyIcon(key, R.integer.keycode_settings,
					KeyboardTheme.KEY_SYM_SETTINGS);
			_updateKeyIcon(key, R.integer.keycode_translate,
					KeyboardTheme.KEY_SYM_TRANSLATE);
			_updateKeyIcon(key, R.integer.keycode_locale,
					KeyboardTheme.KEY_SYM_LOCALE);
			_updateKeyIcon(key, R.integer.keycode_smiley,
					KeyboardTheme.KEY_SYM_SMILEY);
		}
		

		BaseKeyboard.checkForNulls("updateKeyIcon()", key);
	}



	/**
	 * Updates the Any Key symbol, based on user preference.
	 */
	protected void updateAnyKeySym() {
		Key key = getKeyboard().getAnyKey();
		KeyboardTheme kbTheme = KeyboardThemeManager.getCurrentTheme();
		if(key == null)
			return;

		// Override default label/icon for customizable Any Key
		if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_smiley_menu))) {
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SMILEY);
			key.codes[0] = BaseKeyboard.KEYCODE_SMILEY;
		} else if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_arrow_keypad))) {
			// Arrow keypad
			key.label = null;
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_ARROWS);
			key.codes[0] = BaseKeyboard.KEYCODE_ARROWS;
		} else if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_voice_input))) {
			// Voice input
			key.label = null;
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_MIC);
			key.codes[0] = BaseKeyboard.KEYCODE_VOICE;
		} else if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_translator))) {
			// Translator
			key.label = null;
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_TRANSLATE);
			key.codes[0] = BaseKeyboard.KEYCODE_TRANSLATE;
		} else if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_locale))) {
			// Language
			key.label = null;
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_LOCALE);
			key.codes[0] = BaseKeyboard.KEYCODE_LOCALE;
		} else if (mAnyKeyAction.equals(getContext().getString(
				R.string.any_key_action_id_settings))) {
			// Settings
			key.label = null;
			key.icon = getAnyKeyIcon(mAnyKeyAction, false);
			key.iconPreview = kbTheme.getDrawable(KeyboardTheme.KEY_SYM_POPUP_SETTINGS);
			key.codes[0] = BaseKeyboard.KEYCODE_SETTINGS;
		} else
			Assert.assertTrue(false);

		BaseKeyboard.checkForNulls("updateAnyKeySym()", key);

		BaseKeyboard.updateKeyBounds(key);

		// Assign the any key as one of the special keys
		getKeyboard().assignSpecialKey(key);

		if (mAnyKeyLongAction != null)
			getKeyboard().assignSpecialKey(key, getFnKeyCode(mAnyKeyLongAction));
		
		if (mReturnKeyLongAction != null)
			getKeyboard().assignSpecialKey(key, getFnKeyCode(mReturnKeyLongAction));		

	}

	/**
	 * Updates the icon for a particular key.
	 * 
	 * @param key				The key to update.
	 * @param keyCodeResID		An integer resource for the key code.
	 * @param drawableID		The theme drawable id.
	 * @return					Returns 0 if the key already had the requested drawable, 1 if it
	 *         is updated, and -1 if key doesn't match the key code.
	 */
	private int _updateKeyIcon(Key key, int keyCodeResID, int drawableID) {
		Resources res = getContext().getResources();
		Drawable drawable;

		if (key.codes[0] == res.getInteger(keyCodeResID)) {
			drawable = KeyboardThemeManager.getCurrentTheme().getDrawable(drawableID);
			if (key.icon == drawable) {
				return 0;
			} else {
				key.icon = drawable;
				return 1;
			}
		}

		return -1;
	}


	/*
	 * Photo wallpaper methods.
	 */

	/**
	 * Load the photo wallpaper drawable from shared prefs.
	 * 
	 * @param context	A context.
	 */
	protected void loadWallpaper(Context context) {
		if(mPhotoBackground != null)
			// Don't bother reloading
			return;

		// Get alpha and fit from preferences
		SharedPreferences preference = context.getSharedPreferences(
				Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		mPhotoKeyBGAlpha = preference.getInt("wallpaper_alpha", 255);
		mBackgroundFit = preference.getBoolean("wallpaper_fit", true);
		mPhotoBackground = WallpaperPhoto.loadWallpaper(context);
		setPhotoWallpaperBounds();
	}



	/**
	 * Apply a photo wallpaper to the background. Used by WallpaperPhoto for
	 * preview purposes only.
	 * 
	 * @param wallpaper
	 * @param alpha
	 * @param fit
	 */
	public void applyWallpaper(Drawable wallpaper, int alpha, boolean fit) {
		mPhotoBackground = wallpaper;
		mPhotoKeyBGAlpha = alpha;
		mBackgroundFit = fit;
		setPhotoWallpaperBounds();
		invalidateAllKeys();
	}


	/**
	 * Reloads the wallpaper. Called by WallpaperPhoto to restore the original
	 * wallpaper if user cancels the preview.
	 */
	public void reloadWallpaper() {
		mPhotoBackground = null;
		loadWallpaper(getContext());
	}


	/**
	 * Reloads the current theme.
	 */
	public void reloadTheme() {
		invalidateAllKeys();
	}

    
    
    protected int getKeyIndices(int x, int y, int[] allKeys) {
        final Key[] keys = mKeys;
        int primaryIndex = NOT_A_KEY;
        int closestKey = NOT_A_KEY;
        int closestKeyDist = mProximityThreshold + 1;
        java.util.Arrays.fill(mDistances, Integer.MAX_VALUE);
        int [] nearestKeyIndices = mKeyboard.getNearestKeys(x, y);
        final int keyCount = nearestKeyIndices.length;
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys[nearestKeyIndices[i]];
            int dist = 0;
            boolean isInside = key.isInside(x,y);
            if (isInside) {
                primaryIndex = nearestKeyIndices[i];
            }

            if (((mProximityCorrectOn 
                    && (dist = key.squaredDistanceFrom(x, y)) < mProximityThreshold) 
                    || isInside)
                    && key.codes[0] > 32) {
                // Find insertion point
                final int nCodes = key.codes.length;
                if (dist < closestKeyDist) {
                    closestKeyDist = dist;
                    closestKey = nearestKeyIndices[i];
                }
                
                if (allKeys == null) continue;
                
                for (int j = 0; j < mDistances.length; j++) {
                    if (mDistances[j] > dist) {
                        // Make space for nCodes codes
                        System.arraycopy(mDistances, j, mDistances, j + nCodes,
                                mDistances.length - j - nCodes);
                        System.arraycopy(allKeys, j, allKeys, j + nCodes,
                                allKeys.length - j - nCodes);
                        for (int c = 0; c < nCodes; c++) {
                            allKeys[j + c] = key.codes[c];
                            mDistances[j + c] = dist;
                        }
                        break;
                    }
                }
            }
        }
        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey;
        }
        return primaryIndex;
    }

    private void detectAndSendKey(int index, int x, int y, long eventTime) {
        if (index != NOT_A_KEY && index < mKeys.length) {
            final Key key = mKeys[index];
            if (key.text != null) {
                mKeyboardActionListener.onText(key.text);
                mKeyboardActionListener.onRelease(NOT_A_KEY);
            } else {
                int code = key.codes[0];
                //TextEntryState.keyPressedAt(key, x, y);
                int[] codes = new int[MAX_NEARBY_KEYS];
                Arrays.fill(codes, NOT_A_KEY);
                getKeyIndices(x, y, codes);
                // Multi-tap
                if (mInMultiTap) {
                    if (mTapCount != -1) {
                        mKeyboardActionListener.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE);
                    } else {
                        mTapCount = 0;
                    }
                    code = key.codes[mTapCount];
                }
                mKeyboardActionListener.onKey(code, codes);
                mKeyboardActionListener.onRelease(code);
            }
            mLastSentIndex = index;
            mLastTapTime = eventTime;
        }
    }

    
    
    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    private CharSequence getPreviewText(Key key) {
        if (mInMultiTap) {
            // Multi-tap
            mPreviewLabel.setLength(0);
            mPreviewLabel.append((char) key.codes[mTapCount < 0 ? 0 : mTapCount]);
            return adjustCase(mPreviewLabel);
        } else {
        	if(mLongPress && key.popupCharacters != null)
                return adjustCase(key.popupCharacters);

            return adjustCase(key.label);
        }
    }

    protected void updatePreview(Key key) {
    	for(int iKey = 0; iKey < mKeys.length; iKey++)
    		if(mKeys[iKey] == key) {
    			updatePreview(iKey);
    			break;
    		}
    }

    protected void updatePreview(int keyIndex) {
        int oldKeyIndex = mCurrentKeyIndex;

        mCurrentKeyIndex = keyIndex;
        // Release the old key and press the new key
        final Key[] keys = mKeys;
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (oldKeyIndex != NOT_A_KEY && keys.length > oldKeyIndex) {
                keys[oldKeyIndex].onReleased(mCurrentKeyIndex == NOT_A_KEY);
                invalidateKey(oldKeyIndex);
            }
            if (mCurrentKeyIndex != NOT_A_KEY && keys.length > mCurrentKeyIndex) {
                keys[mCurrentKeyIndex].onPressed();
                invalidateKey(mCurrentKeyIndex);
            }
        }
        // If key changed or superscript was activated, and preview is on ...
        if ((oldKeyIndex != mCurrentKeyIndex || mLongPress) && mShowPreview) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW);
            if (mPreviewPopup.isShowing()) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler.sendMessageDelayed(mHandler
                            .obtainMessage(MSG_REMOVE_PREVIEW), 
                            DELAY_AFTER_PREVIEW);
                }
            }
            if (keyIndex != NOT_A_KEY) {
                if (mPreviewPopup.isShowing() && mPreviewText.getVisibility() == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showPreview(keyIndex);
                } else {
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0), 
                            DELAY_BEFORE_PREVIEW);
                }
            }
        }
    }

    
    private void showPreview(final int keyIndex) {
        if (keyIndex < 0 || keyIndex >= mKeys.length) return;
        showPreview(mKeys[keyIndex]);
    }
    private void showPreview(final Key key) {
    	if(key.label == null && key.icon == null)
    		return;
    	if (key.icon != null) {
            mPreviewText.setCompoundDrawables(null, null, null, 
                    key.iconPreview != null ? key.iconPreview : key.icon);
            mPreviewText.setText(null);
        } else {
        	CharSequence previewText = getPreviewText(key);
        	
            mPreviewText.setCompoundDrawables(null, null, null, null);
            mPreviewText.setText(previewText);
            
            if (previewText.length() > 1 && key.codes.length < 2) {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize);
                mPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge);
                mPreviewText.setTypeface(Typeface.DEFAULT);
            }
        }
        mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = Math.max(mPreviewText.getMeasuredWidth(),
        		key.width + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight());
        int popupHeight = key.height + mPreviewText.getPaddingTop() + mPreviewText.getPaddingBottom();
        LayoutParams lp = mPreviewText.getLayoutParams();
        if (lp != null) {
            lp.width = popupWidth;
            lp.height = popupHeight;
        }
        if (!mPreviewCentered) {
            mPopupPreviewX = key.x - mPreviewText.getPaddingLeft() + getPaddingLeft();
            mPopupPreviewY = key.y - popupHeight + mPreviewOffset;
        } else {
            // TODO: Fix this if centering is brought back
            mPopupPreviewX = 160 - mPreviewText.getMeasuredWidth() / 2;
            mPopupPreviewY = - mPreviewText.getMeasuredHeight();
        }
        mHandler.removeMessages(MSG_REMOVE_PREVIEW);
        
        getLocationInWindow(mWindowOffsetCoordinates);
        mWindowOffsetCoordinates[0] += mPopupKeyboardOffsetX; // Offset may be zero
        mWindowOffsetCoordinates[1] += mPopupKeyboardOffsetY; // Offset may be zero
        getLocationOnScreen(mScreenOffsetCoordinates);

        // Set the preview background state
        mPreviewText.getBackground().setState(
                key.popupResId != 0 ? LONG_PRESSABLE_STATE_SET : EMPTY_STATE_SET);

        if(KeyboardService.getIME() != null
        		&& !KeyboardService.getIME().isFullscreenMode()){
        	mPopupPreviewX += mWindowOffsetCoordinates[0];
	        mPopupPreviewY += mWindowOffsetCoordinates[1];	        
         }        
        mPopupPreviewX += mScreenOffsetCoordinates[0];
        
        if(KeyboardService.getIME() != null
        		&& !KeyboardService.getIME().isFullscreenMode()){
        	if(mScreenOffsetCoordinates[1] == 0)
        		postDelayed(new Runnable() {
					@Override
					public void run() {
						showPreview(key);
					}
				}, 100);
        }

        if(!mPreviewPopup.isShowing()){
            mPreviewPopup.setWidth(popupWidth);
            mPreviewPopup.setHeight(popupHeight);
            mPreviewPopup.showAtLocation(mPopupParent, Gravity.NO_GRAVITY, mPopupPreviewX, mPopupPreviewY);
            Log.d("PREVIEWPOPUP", "showAtLocation(" + mPreviewPopup.getAnimationStyle() + ")");
        } else {
            mPreviewPopup.update(mPopupPreviewX, mPopupPreviewY, -1, -1);
            Log.d("PREVIEWPOPUP", "update(" + mPopupPreviewY + ")");
        }
        
        mPopupPreviewWidthPrev = popupWidth;
        mPopupPreviewHeightPrev = popupHeight;
        
        mPreviewText.setVisibility(VISIBLE);
    }

    
    
	/**
	 * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is
	 * not sufficient because the keyboard renders the keys to an off-screen
	 * buffer and an invalidate() only draws the cached buffer.
	 * 
	 * @see #invalidateKey(int)
	 */
	public void invalidateAllKeys() {
		mDirtyRect.union(0, 0, getWidth(), getHeight());
		mDrawPending = true;
		invalidate();
	}

	/**
	 * Invalidates a key so that it will be redrawn on the next repaint. Use
	 * this method if only one key is changing it's content. Any changes that
	 * affect the position or size of the key may not be honored.
	 * 
	 * @param keyIndex		the index of the key in the attached {@link Keyboard}.
	 * @see #invalidateAllKeys
	 */
	public void invalidateKey(int keyIndex) {
		Object keys[] = getKeyboard().getKeys().toArray();
		if (keys == null)
			return;
		if (keyIndex < 0 || keyIndex >= keys.length) {
			return;
		}
		final Key key = (Key) keys[keyIndex];
		mInvalidatedKey = key;
		mDrawPending = true;
		mDirtyRect.union(key.x + getPaddingLeft(),
			key.y + getPaddingTop(), 
			key.x + key.width + getPaddingLeft(),
			key.y + key.height + getPaddingTop());
		invalidate(key.x + getPaddingLeft(),
				key.y + getPaddingTop(),
				key.x + key.width + getPaddingLeft(),
				key.y + key.height + getPaddingTop());
	}

    private boolean openPopupIfRequired(MotionEvent me) {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false;
        }
        if (mCurrentKey < 0 || mCurrentKey >= mKeys.length) {
            return false;
        }

        Key popupKey = mKeys[mCurrentKey];        
        boolean result = onLongPress(popupKey);
//        if (result) {
////            mAbortKey = true;
//            showPreview(NOT_A_KEY);
//        }
        return result;
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
	protected boolean onLongPress(Key key) {
		KeyboardService.writeDebug("KeyboardView.onKey(key=" + key.codes[0] + ")");

		if(mIME.inPreviewMode())
			return true;
		
		updatePreview(NOT_A_KEY);
		
		mLongPress = true;

		// Send keystroke feedback
		mIME.keyFeedback(key.codes[0]);

		if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
			getOnKeyboardActionListener().onKey(BaseKeyboard.KEYCODE_SETTINGS, null);
			return true;
		} else if(key.codes[0] == Keyboard.KEYCODE_SHIFT) {
			// Enable caps lock
			setCapsLock(!getCapsLock());
			setShifted(getCapsLock(), getCapsLock());
			return true;
		} else if(key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
			// Open main menu
			mIME.keyFeedback(key.codes[0]);
			mIME.onMainMenuKey();
			return true;
		} else if(key.codes[0] == '.') {
			// Open symbols quick menu
			openSymQuickMenu(key);
			return true;
		} else if(key.codes[0] == BaseKeyboard.KEYCODE_ACTION){
			int keycode = getFnKeyCode(mReturnKeyLongAction);
			getOnKeyboardActionListener().onKey(keycode, new int[] { keycode });
			return true;
		} else if(key == getKeyboard().getActionKey()) {
			if(getKeyboard().getEditorAction(true) != EditorInfo.IME_ACTION_NONE) {
				// Perform the default editor action
				mIME.keyFeedback(key.codes[0]);
				mIME.performEditorAction();
				return true;
			}
		} else if(key == getKeyboard().getAnyKey()) {
			if(mIME.mURL) {
				// Open the URL menu	
				mIME.keyFeedback(key.codes[0]);
				openUrlKeyboard();
				return true;
			} else {
				int keycode = getFnKeyCode(mAnyKeyLongAction);
				getOnKeyboardActionListener().onKey(keycode, new int[] { keycode });
				return true;
			}
		} else if (key.popupCharacters != null && key.popupCharacters.length() > 0) {
			// Open a popup menu
			if(key.popupCharacters.length() == 1) {
				// There is only one alternate character.
				if (!mIME.isDrawSuperLabel(key) && !Character.isDigit(key.popupCharacters.charAt(0))) {
					// disabled long press
					getOnKeyboardActionListener().onKey(key.codes[0], key.codes);
					return true;
				} else {
					// Change preview from key label to superlabel
					updatePreview(key);
					return false;
				}
			} else if(key.popupCharacters.length() > 1 && key.popupResId != 0) {
				// There is more than one alternate character. Show a popup keyboard.
				boolean clearSymbols = !mIME.isSuperLabelEnabled() && key.edgeFlags != BaseKeyboard.EDGE_BOTTOM;
				CharSequence popupChars = null;
				if(clearSymbols){
					popupChars = key.popupCharacters;
					// remove symbols					
					StringBuilder chars = new StringBuilder(key.popupCharacters);
					for(int i=0; i< chars.length(); i++){
						char c = chars.charAt(i);
						if(!(Character.isLetter(c) || Character.isDigit(c))){
							chars.deleteCharAt(i);
							i--;
						}
					}

					key.popupCharacters = chars;
				}	
				openPopupCharacterKeyboard(key);	

				if(clearSymbols) 
					key.popupCharacters = popupChars;

				return true;
			}

			return false;
		}

		return false;
	}



	protected boolean onLongPressRelease(Key key) {
		if(key.popupCharacters != null && key.popupCharacters.length() == 1) {
			// There is only one alternate character.
//			if (!mIME.isDrawSuperLabel(key) && !Character.isDigit(key.popupCharacters.charAt(0))) {
				// disabled long press
				int keycode = (int) key.popupCharacters.charAt(0);
				getOnKeyboardActionListener().onKey(keycode, new int[] { keycode });
				return true;
//			}
		}

		return false;
	}

	

    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	
        // Convert multi-pointer up/down events to single up/down events to 
        // deal with the typical multi-pointer behavior of two-thumb typing
        final int pointerCount = me.getPointerCount();
        final int action = me.getAction();
        boolean result = false;
        final long now = me.getEventTime();

        if (pointerCount != mOldPointerCount) {
            if (pointerCount == 1) {
                // Send a down event for the latest pointer
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                        me.getX(), me.getY(), me.getMetaState());
                result = onModifiedTouchEvent(down, false);
                down.recycle();
                // If it's an up action, then deliver the up as well.
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(me, true);
                }
            } else {
                // Send an up event for the last pointer
                MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP,
                        mOldPointerX, mOldPointerY, me.getMetaState());
                result = onModifiedTouchEvent(up, true);
                up.recycle();
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(me, false);
                mOldPointerX = me.getX();
                mOldPointerY = me.getY();
            } else {
                // Don't do anything when 2 pointers are down and moving.
                result = true;
            }
        }
        mOldPointerCount = pointerCount;

        return result;
    }

    
    protected boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
        int touchX = (int) me.getX() - getPaddingLeft();
        int touchY = (int) me.getY() + mVerticalCorrection - getPaddingTop();
        final int action = me.getAction();
        final long eventTime = me.getEventTime();
        int keyIndex = getKeyIndices(touchX, touchY, null);
        mPossiblePoly = possiblePoly;

        
    	if (mPopupKeyboardWindow != null
    			&& mPopupKeyboardWindow.isShowing()
    			&& mPopupKeyboardView != null
    			&& mPopupKeyboardView.isOneTouch()) {
    		me.offsetLocation(-mPopupKeyboardRect.left, mIME.getCandidateHeight() - mPopupKeyboardRect.top);
    		return mPopupKeyboardView.dispatchTouchEvent(me);
    	}

    	
        // Track the last few movements to look for spurious swipes.
        if (action == MotionEvent.ACTION_DOWN) mSwipeTracker.clear();
        mSwipeTracker.addMovement(me);

        // Ignore all motion events until a DOWN.
//        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
//            return true;
//        }

        if (mGestureDetector.onTouchEvent(me)) {
            updatePreview(NOT_A_KEY);
            mHandler.removeMessages(MSG_REPEAT);
            mHandler.removeMessages(MSG_LONGPRESS);

            return true;
        }

//        if (mLongPress && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL)
//            return true;


        switch (action) {
        case MotionEvent.ACTION_DOWN:
//        	mAbortKey = false;
    		mLongPress = false;
        	mLastCodeX = touchX;
        	mLastCodeY = touchY;
        	mLastKeyTime = 0;
        	mCurrentKeyTime = 0;
        	mLastKey = NOT_A_KEY;
        	mCurrentKey = keyIndex;
        	mDownTime = me.getEventTime();
        	mLastMoveTime = mDownTime;
        	checkMultiTap(eventTime, keyIndex);
        	mKeyboardActionListener.onPress(keyIndex != NOT_A_KEY ? 
        			mKeys[keyIndex].codes[0] : 0);
        	if ( mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable ) {
        		mRepeatKeyIndex = mCurrentKey;
        		Message msg = mHandler.obtainMessage(MSG_REPEAT);
        		mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY);
        		repeatKey();
        		// Delivering the key could have caused an abort
//        		if (mAbortKey) {
//        			mRepeatKeyIndex = NOT_A_KEY;
//        			break;
//        		}
        	}
        	if (mCurrentKey != NOT_A_KEY) {
        		Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
        		mHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT);
        	}
        	updatePreview(keyIndex);
        	break;

        case MotionEvent.ACTION_MOVE:
        	boolean continueLongPress = false;
        	if (keyIndex != NOT_A_KEY) {
        		if (mCurrentKey == NOT_A_KEY) {
        			mCurrentKey = keyIndex;
        			mCurrentKeyTime = eventTime - mDownTime;
        		} else {
        			if (keyIndex == mCurrentKey) {
        				mCurrentKeyTime += eventTime - mLastMoveTime;
        				continueLongPress = true;
        			} else if (mRepeatKeyIndex == NOT_A_KEY) {
        				resetMultiTap();
        				mLastKey = mCurrentKey;
        				mLastCodeX = mLastX;
        				mLastCodeY = mLastY;
        				mLastKeyTime =
        						mCurrentKeyTime + eventTime - mLastMoveTime;
        				mCurrentKey = keyIndex;
        				mCurrentKeyTime = 0;
        			}
        		}
        	}
        	if (!continueLongPress) {
        		// Cancel old longpress
        		mHandler.removeMessages(MSG_LONGPRESS);
        		// Start new longpress if key has changed
        		if (keyIndex != NOT_A_KEY) {
        			Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
        			mHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT);
        		}
        	}
        	updatePreview(mCurrentKey);
        	mLastMoveTime = eventTime;
        	break;

        case MotionEvent.ACTION_UP:
        	
        	removeMessages();
        	if (keyIndex == mCurrentKey) {
        		mCurrentKeyTime += eventTime - mLastMoveTime;
        	} else {
        		resetMultiTap();
        		mLastKey = mCurrentKey;
        		mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime;
        		mCurrentKey = keyIndex;
        		mCurrentKeyTime = 0;
        	}
        	if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME
        			&& mLastKey != NOT_A_KEY) {
        		mCurrentKey = mLastKey;
        		touchX = mLastCodeX;
        		touchY = mLastCodeY;
        	}
        	updatePreview(NOT_A_KEY);
        	Arrays.fill(mKeyIndices, NOT_A_KEY);
        	if(mLongPress && keyIndex != NOT_A_KEY) {
        		onLongPressRelease((Key) mKeys[keyIndex]);
        	}
        	// If we're not on a repeating key (which sends on a DOWN event)
        	else if (mRepeatKeyIndex == NOT_A_KEY /*&& !mAbortKey*/) {
        		detectAndSendKey(mCurrentKey, touchX, touchY, eventTime);
        	}
        	invalidateKey(keyIndex);
        	mRepeatKeyIndex = NOT_A_KEY;
    		mLongPress = false;
        	break;
        case MotionEvent.ACTION_CANCEL:
        	removeMessages();
    		mLongPress = false;
        	updatePreview(NOT_A_KEY);
        	invalidateKey(mCurrentKey);
        	break;
        }
        mLastX = touchX;
        mLastY = touchY;
        return true;
    }
    
    
    
	// Overriding this method may provide some performance improvements (see View docs)
    @Override
    public boolean isOpaque() {
    	return true;
    }

    private boolean repeatKey() {
        Key key = mKeys[mRepeatKeyIndex];
        detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime);
        return true;
    }
    
    protected void swipeRight() {
        mKeyboardActionListener.swipeRight();
    }
    
    protected void swipeLeft() {
        mKeyboardActionListener.swipeLeft();
    }

    protected void swipeUp() {
        mKeyboardActionListener.swipeUp();
    }

    protected void swipeDown() {
        mKeyboardActionListener.swipeDown();
    }
    
    public int getVerticalCorrection(){
    	return mVerticalCorrection;
    }

    public void closing() {
        if (mPreviewPopup.isShowing()) {
            mPreviewPopup.dismiss();
        }
        removeMessages();
        
		dismissAllPopupWindows(true);
        mBuffer = null;
        mCanvas = null;
    }

    private void removeMessages() {
        mHandler.removeMessages(MSG_REPEAT);
        mHandler.removeMessages(MSG_LONGPRESS);
        mHandler.removeMessages(MSG_SHOW_PREVIEW);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    private void resetMultiTap() {
        mLastSentIndex = NOT_A_KEY;
        mTapCount = 0;
        mLastTapTime = -1;
        mInMultiTap = false;
    }
    
    private void checkMultiTap(long eventTime, int keyIndex) {
        if (keyIndex == NOT_A_KEY) return;
        Key key = mKeys[keyIndex];
        if (key.codes.length > 1) {
            mInMultiTap = true;
            if (eventTime < mLastTapTime + MULTITAP_INTERVAL
                    && keyIndex == mLastSentIndex) {
                mTapCount = (mTapCount + 1) % key.codes.length;
                return;
            } else {
                mTapCount = -1;
                return;
            }
        }
        if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
            resetMultiTap();
        }
    }
    
    
	public int getTotalHeight() {
		return getHeight() + mIME.getCandidateHeight();
	}


    private static class SwipeTracker {

        static final int NUM_PAST = 4;
        static final int LONGEST_PAST_TIME = 200;

        final float mPastX[] = new float[NUM_PAST];
        final float mPastY[] = new float[NUM_PAST];
        final long mPastTime[] = new long[NUM_PAST];

        float mYVelocity;
        float mXVelocity;

        public void clear() {
            mPastTime[0] = 0;
        }

        public void addMovement(MotionEvent ev) {
            long time = ev.getEventTime();
            final int N = ev.getHistorySize();
            for (int i=0; i<N; i++) {
                addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i),
                        ev.getHistoricalEventTime(i));
            }
            addPoint(ev.getX(), ev.getY(), time);
        }

        private void addPoint(float x, float y, long time) {
            int drop = -1;
            int i;
            final long[] pastTime = mPastTime;
            for (i=0; i<NUM_PAST; i++) {
                if (pastTime[i] == 0) {
                    break;
                } else if (pastTime[i] < time-LONGEST_PAST_TIME) {
                    drop = i;
                }
            }
            if (i == NUM_PAST && drop < 0) {
                drop = 0;
            }
            if (drop == i) drop--;
            final float[] pastX = mPastX;
            final float[] pastY = mPastY;
            if (drop >= 0) {
                final int start = drop+1;
                final int count = NUM_PAST-drop-1;
                System.arraycopy(pastX, start, pastX, 0, count);
                System.arraycopy(pastY, start, pastY, 0, count);
                System.arraycopy(pastTime, start, pastTime, 0, count);
                i -= (drop+1);
            }
            pastX[i] = x;
            pastY[i] = y;
            pastTime[i] = time;
            i++;
            if (i < NUM_PAST) {
                pastTime[i] = 0;
            }
        }

        public void computeCurrentVelocity(int units) {
            computeCurrentVelocity(units, Float.MAX_VALUE);
        }

        public void computeCurrentVelocity(int units, float maxVelocity) {
            final float[] pastX = mPastX;
            final float[] pastY = mPastY;
            final long[] pastTime = mPastTime;

            final float oldestX = pastX[0];
            final float oldestY = pastY[0];
            final long oldestTime = pastTime[0];
            float accumX = 0;
            float accumY = 0;
            int N=0;
            while (N < NUM_PAST) {
                if (pastTime[N] == 0) {
                    break;
                }
                N++;
            }

            for (int i=1; i < N; i++) {
                final int dur = (int)(pastTime[i] - oldestTime);
                if (dur == 0) continue;
                float dist = pastX[i] - oldestX;
                float vel = (dist/dur) * units;   // pixels/frame.
                if (accumX == 0) accumX = vel;
                else accumX = (accumX + vel) * .5f;

                dist = pastY[i] - oldestY;
                vel = (dist/dur) * units;   // pixels/frame.
                if (accumY == 0) accumY = vel;
                else accumY = (accumY + vel) * .5f;
            }
            mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity)
                    : Math.min(accumX, maxVelocity);
            mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity)
                    : Math.min(accumY, maxVelocity);
        }

        public float getXVelocity() {
            return mXVelocity;
        }

        public float getYVelocity() {
            return mYVelocity;
        }
    }
    
    
    
 
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    


	/*
	 * Methods to save and restore PopupWindows in case the view is
	 * re-initialized
	 */

	/**
	 * Returns the layout id of contents of the currently-open PopupWindow
	 * 
	 * @return	A layout id
	 */
	public int getCurrPopupLayoutID() {
		if (mLastPopupWindow == null)
			return -1;

		int popupKeyboardID = mLastPopupWindow.getContentView().getId();

		return popupKeyboardID;
	}



	/**
	 * Returns the keyboard id of the currently-open popup keyboard
	 * 
	 * @return	A keyboard id
	 */
	public int getCurrPopupKeyboardID() {
		if (mLastPopupWindow == null)
			return -1;

		ViewGroup keyboardLayout = (ViewGroup) mLastPopupWindow .getContentView();
		if (keyboardLayout instanceof PopupKeyboardLayout) {
			PopupKeyboardLayout popupKeyboardLayout = (PopupKeyboardLayout) mLastPopupWindow.getContentView();
			int popupKeyboardID = popupKeyboardLayout.getKeyboardView().getKeyboard().getXMLResID();
			return popupKeyboardID;
		}
		return -1;
	}



	/**
	 * Restores a PopupWindow after the view is re-initialized
	 * 
	 * @param popupLayoutID		The layout ID of the popup keyboard.
	 * @param popupKeyboardID	The ID of the keyboard XML file.
	 * @param parm				??? The Any Key ???
	 */
	protected void restorePopupWindow(int popupLayoutID, int popupKeyboardID, Object parm) {
		if (popupLayoutID == -1)
			return;

		if (popupLayoutID == R.id.popupKeyboardLayout) {
			if (popupKeyboardID == R.xml.smiley_menu)
				openSmileyKeyboard();
			else if (popupKeyboardID == R.xml.url_menu)
				openUrlKeyboard();
		} else if (popupLayoutID == R.id.sym_menu_layout)
			openSymMenu();
		else if (popupLayoutID == R.id.translating_keyboard)
			openTranslator();
		else if (popupLayoutID == R.id.popupKeyboardLayout)
			if (popupKeyboardID == R.xml.url_menu)
				openUrlKeyboard();
			else if (popupLayoutID == R.id.main_menu) {
				Assert.assertTrue(parm != null);
				openMainMenu();
			}
	}

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
 
	
	
	
	
	public String toString(String prefix) {
		StringBuilder buffer = new StringBuilder();

		Utils.appendLine(buffer, prefix, getClass().getName());
		Utils.appendLine(buffer, prefix, "{");

		String subPrefix = prefix + "\t"; 
		Utils.appendLine(buffer, subPrefix, "mDrawPending" + " = " + mDrawPending);
		Utils.appendLine(buffer, subPrefix, "mBuffer" + " = " + mBuffer);
		Utils.appendLine(buffer, subPrefix, "mCanvas" + " = " + mCanvas);
		Utils.appendLine(buffer, subPrefix, "mInvalidatedKey" + " = " + mInvalidatedKey);
		Utils.appendLine(buffer, subPrefix, "mPhotoBackground" + " = " + mPhotoBackground);
		Utils.appendLine(buffer, subPrefix, "mKeyBGAlpha" + " = " + mPhotoKeyBGAlpha);
		Utils.appendLine(buffer, subPrefix, "mBackgroundFit" + " = " + mBackgroundFit);
		Utils.appendLine(buffer, subPrefix, "mPopupKeyboardWindow" + " = " + mPopupKeyboardWindow);
		Utils.appendLine(buffer, subPrefix, "mPopupKeyboardLayout" + " = " + mPopupKeyboardLayout);
		Utils.appendLine(buffer, subPrefix, "mPopupKeyboardView" + " = " + mPopupKeyboardView);
		Utils.appendLine(buffer, subPrefix, "mSymMenuWindow" + " = " + mSymMenuWindow);
		Utils.appendLine(buffer, subPrefix, "mSymMenuLayout" + " = " + mSymMenuLayout);
		Utils.appendLine(buffer, subPrefix, "mSymQuickMenuWindow" + " = " + mSymQuickMenuWindow);
		Utils.appendLine(buffer, subPrefix, "mTranslatorWindow" + " = " + mTranslatorWindow);
		Utils.appendLine(buffer, subPrefix, "mTranslatorView" + " = " + mTranslatorView);

		// Add all native fields
		Utils.appendLine(buffer, subPrefix, "");
		Utils.appendLine(buffer, subPrefix,
				Utils.getClassString(this, subPrefix));
		Utils.appendLine(buffer, prefix, "}");

		return buffer.toString();
	}

	
	
	
    
    
    
    
    
    
    



	public void reportError_OutOfMemory(Throwable e, int width, int height) {

		ErrorReport errorReport = new ErrorReport(mIME, e, "OutOfMemory");

		try {
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putRunningProcesses();
			errorReport.putInstalledPackages();
			errorReport.putMainObjects();
			errorReport.putCallTrace();
			errorReport.putParam("width", "" + width);
			errorReport.putParam("height", "" + height);
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();

		Toast.makeText(mIME, mIME.getString(R.string.error_report_sent),
				Toast.LENGTH_LONG).show();
	}

	public void reportError_ShowKey(Throwable e, MotionEvent event) {

		ErrorReport errorReport = new ErrorReport(mIME, e, "showKey");

		try {
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putRunningProcesses();
			errorReport.putInstalledPackages();
			errorReport.putMainObjects();
			errorReport.putCallTrace();
			errorReport.putParam("action", "" + event.getAction());
			errorReport.putParam("x", "" + event.getX());
			errorReport.putParam("y", "" + event.getY());
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();

		Toast.makeText(mIME, mIME.getString(R.string.error_report_sent),
				Toast.LENGTH_LONG).show();
	}

}