package com.comet.keyboard.purchase.google;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.purchase.InAppPurchase;
import com.comet.keyboard.util.Utils;

public class InAppStoreGoogle extends Activity {

	private IabHelper mHelper;
	private String mBase64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj0OmbGIaDZiYlyu39xbBRj+k+nWCpUkvWZb5zE5b8RZrpTdWRCQMqQhg3cU4B6oWhaAkQSA7Per1xa121wU1m6HtRUpVPMMdwpETuN/hIpbwQ9vSCLEhkXCQp02+tVZy53iDtC0v6/qwXKzjedhFEt32FqlJ1mxudLUZ6S23L4ovLQBaauQSeZvU0NruF6E5C+iiIRdp4bHJVbPevanBfkE6FzJHCJ5yIOrTsA21SWpk6Uu/qNdT1AuTGqEC+WRAun+pCorLKPT7QbXlko922YWYDyGVH3gLhCPZ7gudbK+9R9U1V3S3EO2cb0x47N32L7l7CxSaHzEkevkhKBbB9wIDAQAB";
	private String mSKU;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSKU = getIntent().getStringExtra("sku");

		// Construct an in-app billing helper
		mHelper = new IabHelper(this, mBase64EncodedPublicKey);
		mHelper.enableDebugLogging(true, KeyboardApp.LOG_TAG);

		makePurchase();
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v(KeyboardApp.LOG_TAG, "InAppStoreGoogle.onActivityResult(" + resultCode + ")");
		
		// This method calls onIabPurchaseFinished() below
		mHelper.handleActivityResult(requestCode, resultCode, data);

		finish();
	}

	
	
	private void makePurchase() {
		// Initialize the helper
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					// Oh no, there was a problem.
					fail();
					Log.d(KeyboardApp.LOG_TAG, "Problem setting up In-app Billing: " + result);
					return;
				} else {
					Log.d(KeyboardApp.LOG_TAG, "InAppStoreGoogle.makePurchase(): Querying inventory...");
					queryInventory();
				}
			}
		});
	}



	private void queryInventory() {
		mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
			public void onQueryInventoryFinished(IabResult result, Inventory inventory)   
			{
				if (result.isFailure()) {
					Log.d(KeyboardApp.LOG_TAG, "InAppStoreGoogle.queryInventory(): Failed");
					fail();
				} else if (inventory.hasPurchase(mSKU)) {
					Log.d(KeyboardApp.LOG_TAG, "InAppStoreGoogle.queryInventory(): Already purchased");
					success();
				} else {
					Log.d(KeyboardApp.LOG_TAG, "InAppStoreGoogle.queryInventory(): Launching purchase flow...");
					launchPurchaseFlow();
				}
			}
		});
	}



	private void launchPurchaseFlow() {
		mHelper.launchPurchaseFlow(this,
				mSKU,
				10001,   
				new IabHelper.OnIabPurchaseFinishedListener() {
					@Override
					public void onIabPurchaseFinished(IabResult result, Purchase purchase) 
					{
						if (result.isFailure()) {
							Log.d(KeyboardApp.LOG_TAG, "Error purchasing: " + result);
							fail();
							return;
						}

						Log.v(KeyboardApp.LOG_TAG, "Purchase complete: " + result);
						success();
					}
				},
		Utils.getDeviceID(this));
	}
	
	
	
	private void success() {
		setResult(InAppPurchase.SUCCESS);

		mHelper.dispose();
		finish();
	}



	private void fail() {
		setResult(InAppPurchase.FAIL);

		mHelper.dispose();
		finish();
	}



	@Override
	protected void onDestroy() {
		super.onDestroy();

		mHelper.dispose();
	}
}