package com.comet.keyboard.settings;

import com.comet.keyboard.R;
import com.comet.keyboard.license.LicenseClient;
import com.comet.keyboard.util.ExtendedRunnable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class CouponActivity extends Activity {
	// coupon
	private AlertDialog mCouponSubmitDialog;
	private ProgressDialog mPDWaiting;
	private EditText mETCoupon;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// create dialog builders
		AlertDialog.Builder builder = new AlertDialog.Builder(
				this);
		builder.setPositiveButton(R.string.submit, mSubmitClickedListener);
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
						getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
						imm.hideSoftInputFromWindow(mETCoupon.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						
						dialog.dismiss();
						CouponActivity.this.finish();
					}
				});
		
		// set content view
		LayoutInflater li = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup dlgView = (ViewGroup) li.inflate(R.layout.coupon, null);
		builder.setView(dlgView);
		
		// create dialog
		mCouponSubmitDialog = builder.create();
		mCouponSubmitDialog.show();
		
		mETCoupon = (EditText)mCouponSubmitDialog.findViewById(R.id.etCoupon);
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				imm.showSoftInput(mETCoupon, InputMethodManager.SHOW_IMPLICIT);
			}
			
		}, 500);
	}
	
	@Override
	public void onBackPressed () {
		finish();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
    private DialogInterface.OnClickListener mSubmitClickedListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			imm.hideSoftInputFromWindow(mETCoupon.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			
			String couponCode = mETCoupon.getText().toString();
			if(couponCode.length() == 0) {
				CouponActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String strCouponPrompt = CouponActivity.this.getString(R.string.settings_title_enter_coupon_code);
						Toast.makeText(CouponActivity.this, strCouponPrompt, Toast.LENGTH_SHORT).show();
					}
				});
				
				CouponActivity.this.finish();
				return;
			}
			
			// show waiting dialog
			mPDWaiting = showWaitingDlg(CouponActivity.this);
			Thread thread = new Thread(new ExtendedRunnable(couponCode) {
				@Override
				public void run() {
					LicenseClient licenseClient = new LicenseClient(CouponActivity.this);
					String couponCode = (String)item;
					
					// hide waiting dialog
					mPDWaiting.dismiss();
					
					try {
						long remainedTime = licenseClient.submitCoupon(couponCode);
						if(remainedTime <= 0) {
							CouponActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									String strCouponError = CouponActivity.this.getString(R.string.error_coupon_code);
									Toast.makeText(CouponActivity.this, strCouponError, Toast.LENGTH_SHORT).show();
								}
							});
							
							
						} else {
							CouponActivity.this.runOnUiThread(new ExtendedRunnable(Long.valueOf(remainedTime)) {
								@Override
								public void run() {
									long remainedTime = ((Long)item).longValue();
									long InDay = (long)(Math.round((double)remainedTime / (3600 * 24 * 1000)));
									String strMessage = String.format(CouponActivity.this.getString(R.string.coupon_applied), InDay);
									Toast.makeText(CouponActivity.this, strMessage, Toast.LENGTH_LONG).show();
								}
							});
						}
					} catch (Exception e) {
						CouponActivity.this.runOnUiThread(new ExtendedRunnable(e) {
							@Override
							public void run() {
								Exception e = (Exception)item;
								String strCouponError = e.getMessage();
								Toast.makeText(CouponActivity.this, strCouponError,
										Toast.LENGTH_SHORT).show();
							}
						});
					}
					
					CouponActivity.this.finish();
				}
			});
			thread.start();
		}
    };

    public static ProgressDialog showWaitingDlg(Context context) {
		ProgressDialog progressDlg = new ProgressDialog(context);

		progressDlg.setMessage(context.getString(R.string.waiting));
		progressDlg.setIndeterminate(true);
		progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDlg.setCancelable(true);
		progressDlg.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.dismiss();
				}
				return false;
			}
		});
		progressDlg.show();
		
		return progressDlg;
	}
}
