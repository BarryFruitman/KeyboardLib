package com.comet.keyboard;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.style.SuggestionSpan;

public class SuggestionMenuReceiver extends BroadcastReceiver {

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onReceive(Context context, Intent intent) {
		String before = intent.getExtras().getString(SuggestionSpan.SUGGESTION_SPAN_PICKED_BEFORE);
		String after = intent.getExtras().getString(SuggestionSpan.SUGGESTION_SPAN_PICKED_AFTER);
		String hash = intent.getExtras().getString(SuggestionSpan.SUGGESTION_SPAN_PICKED_HASHCODE);

		KeyboardService.IME.onSuggestionMenuItemClick(after);
	}
}
