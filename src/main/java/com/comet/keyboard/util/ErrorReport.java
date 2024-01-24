/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.comet.keyboard.KeyboardService;
import com.comet.keyboard.settings.Settings;

public class ErrorReport implements Runnable {

//	private ArrayList<BasicNameValuePair> mParams = new ArrayList<BasicNameValuePair>();

	Throwable mException;
	Context mContext;

	public ErrorReport(Context context, Throwable exception, String errorId) {
		mContext = context;
		
//		// Get stack trace
//		mException = exception;
//		StringWriter stackWriter = new StringWriter();
//		PrintWriter printWriter = new PrintWriter(stackWriter);
//		if(mException == null) {
//			mException = new Exception("No exception");
//			mException.fillInStackTrace();
//		}
//		mException.printStackTrace(printWriter);
//
//		String enabledIMEs = android.provider.Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
//				android.provider.Settings.Secure.ENABLED_INPUT_METHODS);
//		putParam("enabled_imes", enabledIMEs);
//
//		// Get screen size
//		int SCREEN_WIDTH = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
//		int SCREEN_HEIGHT = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
//
//		putParam("stack_trace", stackWriter.getBuffer().toString());
//		putParam("error_id", errorId);
//		putParam("device_id", Utils.getDeviceID(context));
////		putParam("google_id", Utils.getGmailAcount(context));
//		putParam("heap_size", "" + Debug.getNativeHeapSize());
//		putParam("heap_free", "" + Debug.getNativeHeapFreeSize());
//		putParam("heap_used", "" + Debug.getNativeHeapAllocatedSize());
//		putParam("app_ver", KeyboardApp.version);
//		putParam("os_ver", android.os.Build.VERSION.RELEASE);
//		putParam("screen", SCREEN_WIDTH + "x" + SCREEN_HEIGHT);
	}


	// Constructor for non-exception errors
	public ErrorReport(Context context, String errorId) {
		this(context, null, errorId);
	}


	public void putParam(String name, String value) {
//		mParams.add(new BasicNameValuePair(name, value));
	}
	
	
	public void putSharedPrefs(String filename) {
		SharedPreferences sharedPrefs = mContext.getSharedPreferences(filename, Context.MODE_PRIVATE);
		Map<String,?> allPrefs = sharedPrefs.getAll();
		for(HashMap.Entry<String,?> entry : allPrefs.entrySet()) {
			String value = entry.getValue().toString();
			putParam(filename + "_" + entry.getKey(), value);
		}
	}
	
	
	public void putRunningProcesses() {
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses();
		Object processes[] = processList.toArray();
		int iProcess = 1;
		for(Object process : processes) {
			putParam("process_" + iProcess++, ((ActivityManager.RunningAppProcessInfo) process).processName);
		}
	}
	
	
	public void putInstalledPackages() {
		PackageManager packageManager = (PackageManager) mContext.getPackageManager();
		List<PackageInfo> packageList = packageManager.getInstalledPackages(0);
		Object packages[] = packageList.toArray();
		int iPackage = 1;
		for(Object pack : packages) {
			putParam("package_" + iPackage++, ((PackageInfo) pack).packageName);
		}
	}

	public void putMainObjects() {
		// BaseKeybaordService
		if (KeyboardService.IME != null) {
			putParam("class_" + KeyboardService.class.getName(), KeyboardService.IME.toString());
		}
	}
	
	public void putCallTrace() {
		KeyboardService ime = KeyboardService.IME;
		if(ime == null)
			return;
		
		ArrayList<String> callTrace = ime.getCallTrace();
		if(callTrace == null)
			return;

		int iCall = 0;
		for(Object call : callTrace) {
			putParam("trace_" + iCall++, (String) call);
		}
	}
	

	
	public void post() {
		// Post to server in another thread
		(new Thread(this)).start();
	}
	
	

	public static void reportError(Throwable e, Context context, String errorId, String details) {

		ErrorReport errorReport = new ErrorReport(context, e, errorId);
		
		try {
			errorReport.putParam("details", details);
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putRunningProcesses();
			errorReport.putInstalledPackages();
			errorReport.putMainObjects();
			errorReport.putCallTrace();
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
	public static void reportError(Throwable e, Context context, String errorId) {
		reportError(e, context, errorId, "");
	}



	public static void reportShortError(Throwable e, Context context, String errorId, String details) {

		ErrorReport errorReport = new ErrorReport(context, e, errorId);
		
		try {
			errorReport.putParam("details", details);
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putCallTrace();
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
	public static void reportShortError(Throwable e, Context context, String errorId) {
		reportShortError(e, context, errorId, "");
	}



	public static void reportError(Context context, String errorId, String details) {

		ErrorReport errorReport = new ErrorReport(context, errorId);
		
		try {
			errorReport.putParam("details", details);
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putRunningProcesses();
			errorReport.putInstalledPackages();
			errorReport.putMainObjects();
			errorReport.putCallTrace();
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
	public static void reportError(Context context, String errorId) {
		reportError(context, errorId, "");
	}
	
	
	
	public static void reportShortError(Context context, String errorId, String details) {

		ErrorReport errorReport = new ErrorReport(context, errorId);
		
		try {
			errorReport.putParam("details", details);
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putCallTrace();
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
	public static void reportShortError(Context context, String errorId) {
		reportShortError(context, errorId, "");
	}


	@Override
	public void run() {
//		// Post to server
//		try {
//			HttpClient client = new DefaultHttpClient();
//			String postURL = "http://cometapps.com/typesmart/reports/error_report.php";
//			HttpPost post = new HttpPost(postURL);
//			UrlEncodedFormEntity ent = new UrlEncodedFormEntity(mParams,HTTP.UTF_8);
//			post.setEntity(ent);
//			HttpResponse responsePOST = client.execute(post);
//			HttpEntity resEntity = responsePOST.getEntity();
//			if (resEntity != null) {
//				Log.d(KeyboardApp.LOG_TAG, EntityUtils.toString(resEntity));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
}
