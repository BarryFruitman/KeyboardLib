package com.comet.keyboard.purchase;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.KeyboardApp.AppStore;
import com.comet.keyboard.purchase.google.InAppStoreGoogle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class InAppPurchase extends Activity {

	private final int REQ_PURCHASE = 20000;
	private String mSKU;
	public static final int SUCCESS = 1;
	public static final int FAIL = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSKU = getIntent().getStringExtra("sku");

		if(launchToInAppStore() == false)
			Toast.makeText(KeyboardApp.getApp(), "Cannot connect to app store", Toast.LENGTH_LONG).show();
	}
	
	
	private boolean launchToInAppStore() {
		AppStore appStore = KeyboardApp.getApp().mAppStore; 
		if(appStore == AppStore.Google) {
			Intent intent = new Intent(this, InAppStoreGoogle.class);
			intent.putExtra("sku", mSKU);
			startActivityForResult(intent, REQ_PURCHASE);
			return true;
		} else if(appStore == AppStore.Amazon)
			return false;
		else if(appStore == AppStore.Nook)
			return false;

		return false;
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v(KeyboardApp.LOG_TAG, "InAppPurchase.onActivityResult(" + resultCode + ")");
		
		// Pass the result of the child activity to the calling activity
		setResult(resultCode);
		
		finish();
	}


	public void success() {
		Log.v(KeyboardApp.LOG_TAG, "Purchase complete");
		Toast.makeText(KeyboardApp.getApp(), "Purchase complete", Toast.LENGTH_LONG).show();
		setResult(SUCCESS);
	}


	
	public void fail() {
		Log.d(KeyboardApp.LOG_TAG, "Purchase failed");
		Toast.makeText(KeyboardApp.getApp(), "Purchase failed", Toast.LENGTH_LONG).show();
		setResult(FAIL);
	}



	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}