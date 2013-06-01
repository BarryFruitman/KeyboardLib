package com.zendesk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.comet.keyboard.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class ZendeskDialog {
	public static final String TAG = "Zendesk";
	private static final String TITLE_DEFAULT = "Title";
	private static final String TAG_DEFAULT = "dropbox";

	private static String title;
	private static String url;
	private static String tag;

	private static Context context;
	private static View dialogView;
	private static Handler toastHandler;

	private static TextView titleTV;
	private static TextView descriptionTV;
	private static EditText descriptionET;
	private static TextView subjectTV;
	private static EditText subjectET;
	private static TextView emailTV;
	private static EditText emailET;

	private static AlertDialog aDialog;

	private ZendeskDialog(Context context) {
		ZendeskDialog.context = context;
		dialogView = createDialogView(context);
		aDialog = new AlertDialog.Builder(context).setTitle(TITLE_DEFAULT).setView(dialogView).create();
	}

	public static ZendeskDialog Builder(Context context) {
		return new ZendeskDialog(context);
	}

	public ZendeskDialog setTitle(String title) {
		ZendeskDialog.title = title;
		return new ZendeskDialog(ZendeskDialog.context);
	}

	public ZendeskDialog setUrl(String url) {
		ZendeskDialog.url = url;
		return new ZendeskDialog(ZendeskDialog.context);
	}
	
	public ZendeskDialog setTag(String tag) {
		ZendeskDialog.tag = tag;
		return new ZendeskDialog(ZendeskDialog.context);
	}

	public AlertDialog create() {
		// set Dialog Title
		if (ZendeskDialog.title != null)
			aDialog.setTitle(ZendeskDialog.title);
		else if (getMetaDataByKey("zendesk_title") != null)
			aDialog.setTitle(getMetaDataByKey("zendesk_title"));

		if (ZendeskDialog.tag == null){ // not already configured programatically
			String tagConfig = getMetaDataByKey("zendesk_tag");
			if (tagConfig != null){
				ZendeskDialog.tag = getMetaDataByKey("zendesk_tag");
			}
			else{
				ZendeskDialog.tag = TAG_DEFAULT;
			}
		}
		
		// set Dialog url
		if (ZendeskDialog.url == null)
			ZendeskDialog.url = getMetaDataByKey("zendesk_url");

		if (ZendeskDialog.url != null) {
			return aDialog;
		} else {
			Log.e(TAG, "Meta Data with key \"zendesk_url\" couldn't be found in AndroidManifext.xml");
			return null;
		}
	}

	private static String getMetaDataByKey(String key) {
		PackageManager manager = null;
		ApplicationInfo info = null;
		String valueByKey = "";
		try {
			manager = context.getPackageManager();
			info = manager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			valueByKey = info.metaData.getString(key);
			Log.d(TAG, "Key: " + key + " - Value: " + valueByKey);
		} catch (Exception e) {
			Log.e(TAG, "Error reading meta data from AndroidManifest.xml", e);
			return null;
		}
		return valueByKey;
	}

	static Runnable runnable = new Runnable() {
		public void run() {
			Message message = new Message();
			String description = descriptionET.getText().toString();
			String subject = subjectET.getText().toString();
			String email = emailET.getText().toString();

			// Submit query here
			try {
				String server = ZendeskDialog.url;
				String dir = "/requests/mobile_api/create.json";
				String reqDesc = "description=" + URLEncoder.encode(description, "UTF-8");
				String reqEmail = "email=" + URLEncoder.encode(email, "UTF-8");
				String reqSubject = "subject=" + URLEncoder.encode(subject, "UTF-8");
				String reqTag = "set_tags=" + URLEncoder.encode(ZendeskDialog.tag, "UTF-8");
				
				String reqContent = reqDesc + "&" + reqEmail + "&" + reqSubject + "&" + reqTag;
				String requestUrl = "http://" + server + dir;

				URL url = new URL(requestUrl);
				Log.d(TAG, "Sending Request " + url.toExternalForm());
				
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty ("Content-Type","application/x-www-form-urlencoded");
				connection.setRequestProperty("Content-Length", "" + Integer.toString(reqContent.getBytes().length));
				connection.addRequestProperty("X-Zendesk-Mobile-API", "1.0");
				connection.setUseCaches(false);
				connection.setDoInput(true);
				connection.setDoOutput(true);

				// send request
				DataOutputStream out = new DataOutputStream(connection.getOutputStream());
				out.writeBytes(reqContent);
				out.flush();
				out.close();

				// get response
				InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
				BufferedReader bufferReader = new BufferedReader(inputStreamReader, 8192);
				String line = "";
				while ((line = bufferReader.readLine()) != null) {
					Log.d(TAG, line);
				}
				bufferReader.close();
	
				aDialog.dismiss();
				resetDialogView();
			
				message.getData().putString("submit", "successfully");
				toastHandler.sendMessage(message);

			} catch (Exception e) {
				message.getData().putString("submit", "failed");
				toastHandler.sendMessage(message);
				Log.e(TAG, "Error while, submit request", e);
			}
		}
	};

	private static View createDialogView(Context context) {
		LinearLayout llRoot = new LinearLayout(context);
		llRoot.setOrientation(LinearLayout.VERTICAL);
		llRoot.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		ScrollView sv = new ScrollView(context);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		LinearLayout llContent = new LinearLayout(context);
		llContent.setOrientation(LinearLayout.VERTICAL);
		llContent.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		llContent.setPadding(10, 0, 10, 0);

		LinearLayout llTop = new LinearLayout(context);
		llTop.setOrientation(LinearLayout.VERTICAL);

		titleTV = new TextView(context);
		titleTV.setText(context.getString(R.string.support_zendesk_title));
		titleTV.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		titleTV.setTextColor(Color.WHITE);
		descriptionTV = new TextView(context);
		descriptionTV.setText(context.getString(R.string.support_zendesk_description));
		descriptionTV.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		descriptionTV.setTextColor(Color.WHITE);
		descriptionET = new EditText(context);
		descriptionET.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		descriptionET.setMinLines(2);
		descriptionET.setMaxLines(2);
		descriptionET.setInputType(InputType.TYPE_CLASS_TEXT 
				| InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
				| InputType.TYPE_TEXT_FLAG_AUTO_CORRECT );

		subjectTV = new TextView(context);
		subjectTV.setText(context.getString(R.string.support_zendesk_subject));
		subjectTV.setTextColor(Color.WHITE);
		subjectET = new EditText(context);
		subjectET.setSingleLine(true);
		subjectET.setInputType(InputType.TYPE_CLASS_TEXT 
				| InputType.TYPE_TEXT_FLAG_CAP_WORDS
				| InputType.TYPE_TEXT_FLAG_AUTO_CORRECT );

		emailTV = new TextView(context);
		emailTV.setText(context.getString(R.string.support_zendesk_email));
		emailTV.setTextColor(Color.WHITE);
		emailET = new EditText(context);
		emailET.setSingleLine(true);
		emailET.setInputType(InputType.TYPE_CLASS_TEXT 
				| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS );

		LinearLayout llBottom = new LinearLayout(context);
		llBottom.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		TextView poweredByTV = new TextView(context);
		poweredByTV.setText(context.getString(R.string.support_zendesk_powered_by));
		poweredByTV.setPadding(0, 0, 10, 0);
		poweredByTV.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
		poweredByTV.setGravity(Gravity.CENTER_VERTICAL);

		ImageView poweredByIV = new ImageView(context);
		InputStream in = ZendeskDialog.class.getResourceAsStream("/com/zendesk/zendesk.png");
		Bitmap poweredBy = BitmapFactory.decodeStream(in);
		poweredByIV.setImageBitmap(poweredBy);

		LinearLayout llButton = new LinearLayout(context);
		llButton.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		llButton.setOrientation(LinearLayout.HORIZONTAL);
		llButton.setBackgroundColor(0xFFBDBDBD);
		llButton.setPadding(0, 4, 0, 0);

		Button submit = new Button(context);
		submit.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		submit.setText(context.getString(R.string.submit));
		submit.setId(DialogInterface.BUTTON1);
		submit.setOnClickListener(buttonListener);

		Button cancel = new Button(context);
		cancel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		cancel.setText(context.getString(R.string.cancel));
		cancel.setId(DialogInterface.BUTTON2);
		cancel.setOnClickListener(buttonListener);

		llRoot.addView(sv);
		llRoot.addView(llButton);

		sv.addView(llContent);

		llContent.addView(llTop);
		llContent.addView(llBottom);

		llTop.addView(titleTV);
		llTop.addView(subjectTV);
		llTop.addView(subjectET);
		llTop.addView(descriptionTV);
		llTop.addView(descriptionET);
		llTop.addView(emailTV);
		llTop.addView(emailET);

		llBottom.addView(poweredByTV);
		llBottom.addView(poweredByIV);

		llButton.addView(submit);
		llButton.addView(cancel);
		return llRoot;
	}

	private static void resetDialogView() {
		descriptionET.setText("");
		subjectET.setText("");
		emailET.setText("");
		titleTV.setTextColor(Color.WHITE);
		descriptionTV.setTextColor(Color.WHITE);
		subjectTV.setTextColor(Color.WHITE);
		emailTV.setTextColor(Color.WHITE);
	}

	private static View.OnClickListener buttonListener = new View.OnClickListener() {

		public void onClick(View v) {
			switch (v.getId()) {
			case DialogInterface.BUTTON1:
				if (descriptionET.length() != 0 && subjectET.length() != 0 && emailET.length() != 0) {
					toastHandler = new Handler() {
						public void handleMessage(Message msg) {
							super.handleMessage(msg);
							// notify to user here
							String message = msg.getData().getString("submit");
							if (message.equals("successfully"))
								message = context.getString(R.string.support_zendesk_submitted);
							else
								message = context.getString(R.string.support_zendesk_submit_failed);
							Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
						}
					};
					new Thread(runnable).start();
				} else {
					if (descriptionET.length() == 0)
						descriptionTV.setTextColor(Color.RED);
					else
						descriptionTV.setTextColor(Color.WHITE);
					if (subjectET.length() == 0)
						subjectTV.setTextColor(Color.RED);
					else
						subjectTV.setTextColor(Color.WHITE);
					if (emailET.length() == 0)
						emailTV.setTextColor(Color.RED);
					else
						emailTV.setTextColor(Color.WHITE);

					Toast.makeText(context, context.getString(R.string.support_zendesk_missing_field), Toast.LENGTH_SHORT).show();
				}
				break;
			case DialogInterface.BUTTON2:
				aDialog.dismiss();
				resetDialogView();
				break;
			}
		}

	};
}
