package com.comet.keyboard.analytics;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.comet.keyboard.R;
import com.flurry.android.FlurryAgent;

public class MyFlurryAgent extends Object {

	public static void startSession(Context context) {
		FlurryAgent.onStartSession(context, context.getString(R.string.flurry_key));
	}

	public static void endSession(Context context) {
		FlurryAgent.onEndSession(context);
	}
	
	public static void logEvent(String event) {
		FlurryAgent.logEvent(event);
	}

	public static void logParamEvent(String event, String... params) {
		logParamEvent(event, false, params);
	}
	
	private static void logParamEvent(String event, boolean timed, String... args) {
		Map<String, String> params = new HashMap<String, String>();
		if(args != null && args.length > 1)
			for(int iArg = 0; iArg < args.length; iArg+=2)
				params.put(args[iArg], args[iArg+1]);
		FlurryAgent.logEvent(event, params, timed);
	}

	public static void logTimedEvent(String event, String... args) {
		logParamEvent(event, true, args);
	}
	
	public static void endTimedEvent(String event) {
		FlurryAgent.endTimedEvent(event);
	}
	
}
