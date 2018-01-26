/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.comet.keyboard.Suggestor.Suggestion;
import com.comet.keyboard.Suggestor.Suggestions;
import com.comet.keyboard.settings.CandidateHeightSetting;
import com.comet.keyboard.theme.KeyboardTheme;
import com.comet.keyboard.theme.KeyboardThemeManager;
import com.comet.keyboard.util.Utils;

public class CandidateView extends View implements OnLongClickListener {
	public enum MessageType {
		MESSAGE_STATIC, 
		MESSAGE_ALARM,
		MESSAGE_SUGGESTION
	};
	
    private static final int OUT_OF_BOUNDS = -1;
	public static final int NO_DEFAULT = -1;

	private KeyboardService mIME;
    private Suggestions mSuggestions;
    public String mOrgWord;
    public String mDisplayMessage;
    
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    
    private Rect mBgPadding;

    private static final int SCROLL_PIXELS = 20;
    
    private int[] mWordWidth = new int[Suggestor.Suggestions.MAX_SUGGESTIONS];
    private int[] mWordX = new int[Suggestor.Suggestions.MAX_SUGGESTIONS];

    // Last selected suggestion
    private Suggestion mLastSelectedSuggestion;
    
    private static final int X_GAP = 10;
    
    private static final Suggestions EMPTY_LIST = null;

    private int mVerticalPadding;
    private Paint mPaint;
    private boolean mScrolled;
    private int mTargetScrollX;
    
    private int mTotalWidth;
    
    private GestureDetector mGestureDetector;
    private boolean mIsLongClicked = false;
    private long mLongClickedTime = 0;
    // skip all event until context menu showed
    private static final int WAIT_MTIME = 1000;
    
    // Message type
    private MessageType mMessageType = MessageType.MESSAGE_SUGGESTION;
    private OnClickListener mMsgClickListener;
    
    
    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIME = (KeyboardService) context;
        
        mSelectionHighlight = context.getResources().getDrawable(
                android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });

        Resources r = context.getResources();

        // Set paddings
        int paddingTop, paddingBottom, paddingLeft, paddingRight;
        paddingTop = getResources().getDimensionPixelSize(R.dimen.candidate_padding_top);
        paddingBottom = getResources().getDimensionPixelSize(R.dimen.candidate_padding_bottom);
        paddingLeft= getResources().getDimensionPixelSize(R.dimen.candidate_padding_left);
        paddingRight = getResources().getDimensionPixelSize(R.dimen.candidate_padding_right);
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        mVerticalPadding += (getPaddingTop() + getPaddingBottom());
        
        int fontSize = CandidateHeightSetting.getFontSizePreference(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);

        setBackground();

        mGestureDetector = new GestureDetector(mIME, new GestureDetector.SimpleOnGestureListener() {
        	private int MIN_SCROLL = 50;
        	
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
            	if(Math.abs(e1.getX() - e2.getX()) < MIN_SCROLL)
            		// Wait until minimum scroll. This makes it easy to long-press words.
            		return true;

                mScrolled = true;
                int sx = getScrollX();
                sx += distanceX;
                if (sx < 0) {
                    sx = 0;
                }

                if (sx + getWidth() > mTotalWidth) {                    
                    sx -= distanceX;
                }

                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        
        // Set event listener
        setOnLongClickListener(this);
        
        setFontHeight(fontSize);
    }
    
    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     */
    public CandidateView(Context context) {
        this(context, null);
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    public void setFontHeight(int fontSize) {
		mPaint.setTextSize(fontSize);
		
		Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
		final int desiredHeight = fontSize + mVerticalPadding
        + padding.top + padding.bottom;
		
		setMeasuredDimension(getMeasuredWidth(), desiredHeight);
	}
    
    private Rect mMeasurePadding = new Rect();
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        mSelectionHighlight.getPadding(mMeasurePadding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + mMeasurePadding.top + mMeasurePadding.bottom;
        
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec); 
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth, measuredHeight);
    }
    
    
	/**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
	private int mHeight = -1;
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);

    	if (canvas == null) return;

        // Compute metrics
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        mTotalWidth = 0;
        int x = 0;
        if(mHeight == -1)
        	mHeight = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final int y = (int) (((mHeight - mPaint.getTextSize()) / 2) - mPaint.ascent());
        final int suggestionPaddingLeft = getResources().getDimensionPixelSize(R.dimen.candidate_suggestion_padding_left);
        final int suggestionPaddingRight = getResources().getDimensionPixelSize(R.dimen.candidate_suggestion_padding_right);
        x = getPaddingLeft();
        

    	if (mMessageType == MessageType.MESSAGE_STATIC || mMessageType == MessageType.MESSAGE_ALARM) {
            // Draw a special message and return
    		paint.setFakeBoldText(false);
    		paint.setColor(KeyboardThemeManager.getCurrentTheme().getCandidateMessageColor());
        	canvas.drawText(mDisplayMessage, (float)(x + X_GAP), y, paint);
        	return;
    	}

        if(mSuggestions == null) return;
        
        // Draw the rest of the suggestions
        Object[] aSuggestions = mSuggestions.getSuggestions().toArray();
        final int count = Math.min(mSuggestions.size(), Suggestor.Suggestions.MAX_SUGGESTIONS); 
        for (int i = 0; i < count; i++) {
            Suggestion suggestion = (Suggestion) aSuggestions[i];
            
            // Compute text metrics
            float textWidth = paint.measureText(suggestion.getWord());
            final int wordWidth = (int) textWidth + X_GAP * 2;

            mWordX[i] = x;
            mWordWidth[i] = wordWidth;
    		if (mIME.mAutoSelect && i == mSuggestions.getDefault()) {
    			// This is the default suggestion
    			paint.setColor(KeyboardThemeManager.getCurrentTheme().getCandidateRecommendedColor());
    			paint.setFakeBoldText(true);
    		} else {
    			// This is a regular suggestion
    			paint.setColor(KeyboardThemeManager.getCurrentTheme().getCandidateNormalColor());
    			paint.setFakeBoldText(false);
    		}
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
            	// Highlight this word because the user touching it.
            	canvas.translate(x, 0);
            	mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, mHeight);
            	mSelectionHighlight.draw(canvas);
            	canvas.translate(-x, 0);
                mSelectedIndex = i;
                mLastSelectedSuggestion = suggestion;
            }

            // Draw the suggestion
            canvas.drawText(suggestion.getWord(), x + X_GAP, y, paint);
            
            // Draw a divider line
            paint.setColor(KeyboardThemeManager.getCurrentTheme().getCandidateDividerColor()); 
            canvas.drawLine(x + wordWidth + suggestionPaddingLeft, bgPadding.top, 
            		x + wordWidth + suggestionPaddingLeft, mHeight + 1, paint);

            // Increment the x-location
            x += (wordWidth + suggestionPaddingLeft + suggestionPaddingRight);
        }
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }



    protected void setBackground() {
    	Drawable background = KeyboardThemeManager.getCurrentTheme().getDrawable(KeyboardTheme.CANDIDATE_BG);
    	if(background != null)
    		// Theme backgrounds are always resized to fit exactly
    		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
    			setBackground(background);
    		else
    			setBackgroundDrawable(background);
    	else
    		// No background image. Use solid color.
    		setBackgroundColor(KeyboardThemeManager.getCurrentTheme().getCandidateBGColor());
    }
    
    
    
    protected void updateView() {
    	setBackground();
    	invalidate();
    }



    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = NO_DEFAULT;
    }

    public boolean setSuggestions(Suggestions suggestions, boolean completions) {
    	if (mMessageType == MessageType.MESSAGE_STATIC)
    		return false;
    	
    	if (mMessageType == MessageType.MESSAGE_ALARM && (suggestions == null || suggestions.size() == 0))
    		return false;
    	
        clear();
        mMessageType = MessageType.MESSAGE_SUGGESTION;
        
        if (suggestions != null)
            mSuggestions = suggestions;
        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
//        onDraw(null);
        invalidate();
        requestLayout();
        
        return true;
    }
    
    public void setDisplayMessage(String message, MessageType type, OnClickListener listener) {
    	mMessageType = type;
    	mDisplayMessage = message;
    	mMsgClickListener = listener;
    	
    	clear();
    	invalidate();
    }
    
    public void clearDisplayMessage() {
    	mMessageType = MessageType.MESSAGE_SUGGESTION;
    	mDisplayMessage = "";
    	mMsgClickListener = null;
    	
    	clear();
    	invalidate();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	super.onTouchEvent(me);

    	if (mGestureDetector.onTouchEvent(me)) {
    		return true;
    	}

    	long mNowTime = Utils.getTimeMilis();

    	if (mIsLongClicked || ((mNowTime - mLongClickedTime) < WAIT_MTIME)) {
    		mLongClickedTime = Utils.getTimeMilis();

    		removeHighlight();
    		mIsLongClicked = false;
    		return true;
    	}

    	int action = me.getAction();
    	int x = (int) me.getX();
    	int y = (int) me.getY();
    	mTouchX = x;

    	switch (action) {
    	case MotionEvent.ACTION_DOWN:
    		mScrolled = false;
    		invalidate();
    		break;
    	case MotionEvent.ACTION_MOVE:
    		if (y <= 0) {
    			// Fling up!?
    			if (mSelectedIndex >= 0) {
    				mIME.touchCandidate(mSelectedIndex);                    
    				mSelectedIndex = NO_DEFAULT;
    			}
    		}
    		invalidate();
    		break;
    	case MotionEvent.ACTION_UP:
    	case MotionEvent.ACTION_CANCEL:
    		if (mMessageType == MessageType.MESSAGE_STATIC || mMessageType == MessageType.MESSAGE_ALARM) {
    			if(mMsgClickListener != null)
    				mMsgClickListener.onClick(this);
    			return true;
    		}
    		if (!mScrolled && mSelectedIndex >= 0) {
    			int selIndex = mSelectedIndex;
                mIME.touchCandidate(selIndex);
    		}
        	mSelectedIndex = NO_DEFAULT;
            removeHighlight();
            requestLayout();
            break;
        }
        return true;
    }
    

    public boolean onLongClick (View v) {
    	if (mScrolled)
    		return true;
    	
    	if (mMessageType != MessageType.MESSAGE_SUGGESTION)
    		return true;
    	
    	if (mSelectedIndex == NO_DEFAULT)
    		return true;
    	
    	mIsLongClicked = true;

		// Delete a word from a dictionary
		if(mIME.getSuggestor().forget(mLastSelectedSuggestion)) {
            String msg = String.format(
                    getContext().getResources().getString(R.string.msg_word_forgotten),
                    mLastSelectedSuggestion.getWord());
			Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
			
			mIME.updateCandidates();
		} else {
			String msg = String.format(
                    getContext().getResources().getString(R.string.error_message_cant_be_deleted),
                            mLastSelectedSuggestion.getWord());
			Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
		}

        return true;
    }

    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }
}
