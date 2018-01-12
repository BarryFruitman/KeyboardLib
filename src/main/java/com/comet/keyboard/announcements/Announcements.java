package com.comet.keyboard.announcements;

import com.comet.keyboard.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Announcements extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.announcement);

		String message = getIntent().getStringExtra("message");
		((TextView) findViewById(R.id.announcement_title)).setText(getResources().getString(R.string.announcement_title));
		((TextView) findViewById(R.id.announcement_message)).setText(message);
	}



	public void onClickDismiss(View v) {
		finish();
	}
}
