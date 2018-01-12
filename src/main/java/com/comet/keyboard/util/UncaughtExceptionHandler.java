/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.comet.keyboard.KeyboardApp;

import android.os.Build;
import android.util.Log;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	public void uncaughtException(Thread thread, Throwable exception) {
		String version = Build.VERSION.CODENAME + ":"
		+ Build.VERSION.INCREMENTAL	+ ":"
		+ Build.VERSION.RELEASE + ":"
		+ Build.VERSION.SDK + ":"
		+ Build.VERSION.SDK_INT;
		String appName = KeyboardApp.appName;
		String appVersion = KeyboardApp.version;
		String message = exception.getMessage();
		StringWriter stackWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stackWriter);
		exception.printStackTrace(printWriter);
		Throwable cause = exception.getCause();
		String causeMessage = "";
		String causeStackTrace = "";
		if(cause != null) {
			causeMessage = cause.getMessage();
			causeStackTrace = cause.toString();
		}
		
		Log.e("COMET_ERROR", "An unhandled exception occurred:");
		Log.e("COMET_ERROR", "App name:" + appName);
		Log.e("COMET_ERROR", "App version:" + appVersion);
		Log.e("COMET_ERROR", "Android version:" + version);
		Log.e("COMET_ERROR", "Message:" + message);
		Log.e("COMET_ERROR", "Stack trace:" + stackWriter.getBuffer());
		Log.e("COMET_ERROR", "Thread:" + thread.toString());
		Log.e("COMET_ERROR", "Cause message:" + causeMessage);
		Log.e("COMET_ERROR", "Cause stack trace:" + causeStackTrace);
		
		System.exit(0);
/*
        Intent intent = new Intent();
        intent.setClassName("com.comet.keyboard", ".util.ErrorReportActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.version", version);
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.message", message);
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.stackTrace", stackTrace);
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.thread", thread.toString());
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.causeMessage", causeMessage);
    	intent.putExtra("com.comet.keyboard.util.UncaughtExceptionHandler.causeStackTrace", causeStackTrace);
    	KeyboardService.baseKeyboardService.startActivity(intent);
*/
	}
}
