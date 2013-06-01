/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.purchase;

import junit.framework.Assert;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.purchase.BillingService.RequestPurchase;
import com.comet.keyboard.purchase.BillingService.RestoreTransactions;
import com.comet.keyboard.purchase.Consts.PurchaseState;
import com.comet.keyboard.purchase.Consts.ResponseCode;
import com.comet.keyboard.settings.Settings;

public class Purchase {
	public static Purchase PURCHASE = new Purchase();
	private static final String TAG = "TypeSmart Keyboard";
	private final static String PURCHASE_TRANS_ITEM = "inapp_200_translations";
	public TSPurchaseObserver mTSPurchaseObserver;
	public Handler mHandler;

    public BillingService mBillingService;
	private boolean isAvailable = false;

	
	public static Purchase getInstance() {
		if(PURCHASE == null)
			PURCHASE = new Purchase();
		
		return PURCHASE;
	}
	
	/**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    private class TSPurchaseObserver extends PurchaseObserver {
        public TSPurchaseObserver(Handler handler) {
            super(KeyboardService.getIME(), handler);
        }

        @Override
        public void onBillingSupported(boolean supported) {
        	Purchase.getInstance().setBillingAvailablity(supported);
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
                int quantity, long purchaseTime, String developerPayload) {
            if (purchaseState == PurchaseState.PURCHASED) {
            	KeyboardService.getIME().billingCompleted();
            }
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request,
                ResponseCode responseCode) {
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase was successfully sent to server");
                }
                // logProductActivity(request.mProductId, "sending purchase request");
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "user canceled purchase");
                }
                // logProductActivity(request.mProductId, "dismissed purchase dialog");
            } else {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase failed");
                }
                // logProductActivity(request.mProductId, "request purchase returned " + responseCode);
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                ResponseCode responseCode) {
        	/*
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.d(TAG, "completed RestoreTransactions request");
                }
            } else {
                if (Consts.DEBUG) {
                    Log.d(TAG, "RestoreTransactions error: " + responseCode);
                }
            }
            */
        }
    }
    
	private Purchase() {
		mHandler = new Handler();
        mTSPurchaseObserver = new TSPurchaseObserver(mHandler);
        mBillingService = new BillingService();
        mBillingService.setContext(KeyboardService.getIME());
        
        // Check if billing is supported.
        ResponseHandler.register(mTSPurchaseObserver);
        
        isAvailable = mBillingService.checkBillingSupported();
	}
	
	public void initModule() {}
	
	protected void finalize() {
		destroyModule();
	}
	
	public void destroyModule() {
		mBillingService.unbind();
	}
	
	public boolean requestPurchase() {
		boolean result;
		
		result = mBillingService.requestPurchase(PURCHASE_TRANS_ITEM, null);
			
		return result;
	}
	
	public void setBillingAvailablity(boolean supported) {
		isAvailable = supported;
	}
	
	public boolean isBillingAvailable() {
		return isAvailable;
	}
	
	public static int getTransPoint(Context context) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		int version = sharedPrefs.getInt("trans_point", 0);
		
		return version;
	}
	
	public static void putTransPoint(Context context, int point) {
		Assert.assertTrue(point >= 0);
		
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
		sharedPrefsEditor.putInt("trans_point", point);
		sharedPrefsEditor.commit();
	}
}
