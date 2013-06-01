/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2012 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.license;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import junit.framework.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
//import android.util.Log;

public final class LicenseClient implements Runnable {
	// Time constants
	public final static long MILLISECONDS_PER_HOUR = 1000 * 60 *60; 
	public final static long MILLISECONDS_PER_DAY = MILLISECONDS_PER_HOUR * 24;
	public final static long MILLISECONDS_PER_MONTH = MILLISECONDS_PER_DAY * 30;

	private Context mContext;
	private String mCouponCode;

	private final String API_KEY = "$*DFKL*(B451471281A425490439CC52C08930C2JD";
//	private final String API_KEY = "DXDG7X3PAJAZPHC98JM1";

	// Trial Status
	public final long TRIAL_MTIME = MILLISECONDS_PER_MONTH;

	private long mLastUpdateFromServer = 0;

	private static final String INSTALLED_TIME_KEY = "installed_on";
	private static final String EXPIRES_ON_KEY = "expires_on";

	public LicenseClient(Context context) {
		mContext = context;

		// Update the license in a background thread
		updateLicenseAsync();

		// Set the install time, if this is the first launch
		setInstallTime();
	}

	// Login
	/*
	- register
	post parm:
	 api_key: security constant value
	 device_id: device identification
	 google_id: gmail acount
	 app_version: application version number
	 device_type: device model
	 os_version: android os version
	 product_type: free/paid
	 coupon_code: coupon code

	response:
	 <license>
	 	<status>success/fail</status>
	 	<remained_time>time in seconds<remained_time>
	 </license>
	 */
	private final static int HTTP_CONNECTION_MTIMEOUT = 20000;
	
	private ArrayList<NameValuePair> buildLoginParm() {
		ArrayList<NameValuePair> postParms = new ArrayList<NameValuePair>();
	    BasicNameValuePair pair;

	    // security fields
	    pair = new BasicNameValuePair("api_key", API_KEY);
	    postParms.add(pair);

	    // product
	    SharedPreferences sharedPrefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
	    String product = KeyboardApp.getApp().getProduct();
	    pair = new BasicNameValuePair("product", product);
	    postParms.add(pair);
	    
	    // device_id
	    String device_id = Utils.getDeviceID(mContext);
	    pair = new BasicNameValuePair("device_id", device_id);
	    postParms.add(pair);

	    // device_type
	    String device_type = Build.MODEL;
	    pair = new BasicNameValuePair("device_type", device_type);
	    postParms.add(pair);

	    // google_id
	    String google_id = Utils.getGmailAcount(mContext);
	    if(device_type.equals("sdk"))
	    	google_id = "emulator@cometapps.com";
	    pair = new BasicNameValuePair("google_id", google_id);
	    postParms.add(pair);

	    // app_version
	    String app_version = mContext.getResources().getString(R.string.version);
	    pair = new BasicNameValuePair("app_version", app_version);
	    postParms.add(pair);

	    // os_version
	    String os_version = Build.VERSION.RELEASE;
	    pair = new BasicNameValuePair("os_version", os_version);
	    postParms.add(pair);

	    // product_type
	    String product_type = KeyboardApp.getApp().getProductType();
	    pair = new BasicNameValuePair("product_type", product_type);
	    postParms.add(pair);
	    
	    String app_store = KeyboardApp.getApp().mAppStore.toString();
	    pair = new BasicNameValuePair("app_store", app_store);
	    postParms.add(pair);

	    // referrer
	    String referrer = sharedPrefs.getString("referrer", "");
	    try {
			referrer = URLEncoder.encode(referrer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ErrorReport.reportError(mContext, "referrer_encoding", "referrer=" + referrer);
			referrer = "";
		}
	    pair = new BasicNameValuePair("referrer", referrer);
	    postParms.add(pair);
	    
	    // coupon_code
	    if(mCouponCode != null && mCouponCode.length() > 0) {
	    	pair = new BasicNameValuePair("coupon_code", mCouponCode);
	    	postParms.add(pair);
	    }
	    
	    return postParms;
	}


	public long timeLeft() {

		if(System.currentTimeMillis() - mLastUpdateFromServer > MILLISECONDS_PER_DAY)
			updateLicenseAsync();
		
		long timeLeft = TRIAL_MTIME;
		
		try {
			SharedPreferences sharedPrefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
			long expiresOn = sharedPrefs.getLong(EXPIRES_ON_KEY, -1);
			if(expiresOn == -1) {
				// No server value. Use local value.
				long now = System.currentTimeMillis();
				long installedMTime = sharedPrefs.getLong(INSTALLED_TIME_KEY, now);			
				timeLeft = TRIAL_MTIME - (now - installedMTime);
			} else
				timeLeft = expiresOn - System.currentTimeMillis();
		} catch (Exception e) {
			// Do nothing
		}
		
		return timeLeft;
	}
	
	
	
	public boolean isTrialExpired() {
		return timeLeft() < 0;
	}
	
	
	
	/**
	 * Update app status
	 */
	public boolean updateLicenseFromServer() {
		boolean result = true;
		LicenseResult license = getLicenseFromServer();

		if(license == null)
			return false; // Ignore this error (FOR NOW)

		mLastUpdateFromServer = System.currentTimeMillis();

		synchronized (this) {
			SharedPreferences prefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			long expiresOn = license.mMTimeLeft + System.currentTimeMillis();
			editor.putLong(EXPIRES_ON_KEY, expiresOn);
			editor.commit();
		}

		return result;
	}



	public long getExpiryDate() {
		SharedPreferences prefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		return prefs.getLong(EXPIRES_ON_KEY, System.currentTimeMillis());
	}



	/**
	 * Return expire date
	 * @return
	 */
	public static final long LOGIN_ERROR = -1;
	private synchronized LicenseResult getLicenseFromServer() {
		final String loginURL = mContext.getString(R.string.latin_extended_charset);
// 		final String loginURL = "http://www.cometapps.com/typesmart/license/register_test.php";
// 		final String loginURL = "http://192.168.0.108/typesmart/register.php"; 
//		final String loginURL = "http://comet-test.elasticbeanstalk.com/register.php";
		LicenseResult result = null;
		
		// Create a new HttpClient and Post Header
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters, HTTP_CONNECTION_MTIMEOUT);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(httpParameters, HTTP_CONNECTION_MTIMEOUT);

	    HttpClient httpclient = new DefaultHttpClient(httpParameters);
	    HttpPost httppost = new HttpPost(loginURL);
	    
	    ArrayList<NameValuePair> postParms;
	    StringBuilder response = new StringBuilder();
	    try {
		    postParms = buildLoginParm();
	        httppost.setEntity(new UrlEncodedFormEntity(postParms));

	        // Execute HTTP Post Request
	        HttpResponse resp = httpclient.execute(httppost);
	        
	        BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
			String line;

			// Clear buffer
			response.setLength(0);
			while ((line = reader.readLine()) != null)
				response.append(line);
			reader.close();
			
//		    Log.v(KeyboardApp.appName, "HTTP-POST-RESULT: " + response);
		    
		    // Parse response
		    result = parseResponse(response);
	    } catch (Exception e) {
	    	reportError_login(e, response.toString());
	    	
//	    	throw e;
	    }

	    return result;
	}
	
	/**
	 * This method will make sure coupon code is included as a param in LicenseClient.buildLoginParm(),
	 * then call updateLicenseStatus(true). It will then forget couponCode so that it doesn't get submitted on the next login.
	 * @param couponCode
	 */
	public long submitCoupon(String couponCode) throws Exception {
		mCouponCode = couponCode;
		boolean result = updateLicenseFromServer();
		mCouponCode = null;
		if (result)
			return timeLeft();
		else
			return -1;
	}
	
	/**
	 * Parse response from xml string
	 * @param response
	 */
	private LicenseResult parseResponse(StringBuilder response) throws Exception {
		LicenseResult result = new LicenseResult();
		Document doc = null;
		ArrayList<NameValuePair> respPairs = new ArrayList<NameValuePair>();
		String status, timeLeft;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputStream is = new ByteArrayInputStream(response.toString().getBytes("UTF-8"));
		doc = db.parse(is);
		doc.getDocumentElement().normalize();

		// Skip root tag "<TypeSmart>"
		NodeList root = doc.getChildNodes().item(0).getChildNodes();
		for (int i = 0 ; i < root.getLength() ; i++) {
			Node node = root.item(i);
			try {
				if (node instanceof org.w3c.dom.Element) {
					String name = node.getNodeName();
					String value = node.getChildNodes().item(0).getNodeValue();
					respPairs.add(new BasicNameValuePair(name, value));
				}
			} catch (Exception e) {}
		}

		// Get response value
		/// Get status
		status = getValue(respPairs, "status");
		if (status != null && status.equals("success"))
			result.mSuccess = true;
		else {
			result.mErrMessage = getValue(respPairs, "error");
			result.mSuccess = false;
		}
		/// Get remaining time
		timeLeft = getValue(respPairs, "remained_time");
		if (timeLeft != null)
			result.mMTimeLeft = Long.parseLong(timeLeft) * 1000;
		else
			result.mMTimeLeft = TRIAL_MTIME;

		return result;

	}
	
	private static String getValue(ArrayList<NameValuePair> list, String name) {
		NameValuePair pair;
		String value = null;
		int len;
		
		Assert.assertTrue(name != null);
		Assert.assertTrue(list != null);

		try {
			len = list.size();
			
			for (int i = 0 ; i < len ; i++) {
				pair = list.get(i);
				if (pair.getName().equals(name)) {
					value = pair.getValue();
					break;
				}
			}
		} catch (Exception e) {
			return null;
		}
			
		return value;
	}
	
	
	public void updateLicenseAsync() {
		(new Thread(this)).start();
	}

	@Override
	public void run() {
		try {
			updateLicenseFromServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setInstallTime() {
		SharedPreferences prefs = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		if(prefs.getLong(INSTALLED_TIME_KEY, -1) == -1) {
			SharedPreferences.Editor editor = mContext.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
			editor.putLong(INSTALLED_TIME_KEY, System.currentTimeMillis());
			editor.commit();
		}
	}
	
	
	private void reportError_login(Throwable e, String response) {

		ErrorReport errorReport = new ErrorReport(mContext, e, "login");
		
		try {
			errorReport.putParam("response", "" + response);
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
}