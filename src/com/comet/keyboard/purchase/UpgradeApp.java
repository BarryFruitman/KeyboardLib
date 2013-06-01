package com.comet.keyboard.purchase;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class UpgradeApp extends Activity {
	private final int REQ_UPGRADE = 10002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, InAppPurchase.class);
		String sku = getString(R.string.upgrade_sku);
		intent.putExtra("sku", sku);
		startActivityForResult(intent, REQ_UPGRADE);
	}



	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQ_UPGRADE) {
			if(resultCode == InAppPurchase.SUCCESS) {
				// Success!
				KeyboardApp.getApp().upgrade();
				
				Toast.makeText(KeyboardApp.getApp(), "Upgrade complete", Toast.LENGTH_LONG).show();
			} else {
				// Cancelled or failed. Do nothing.
			}

			finish();
		} else
			super.onActivityResult(requestCode, resultCode, data);
	}
}
