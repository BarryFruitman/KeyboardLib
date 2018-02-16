package com.comet.keyboard.install.old;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import junit.framework.Assert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.dictionary.updater.DictionaryFileItem;
import com.comet.keyboard.dictionary.updater.DictionaryItem;
import com.comet.keyboard.dictionary.updater.DictionaryUpdater;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;

public class LanguageSelector extends Activity implements View.OnClickListener {
	public static String DOWNLOAD_ONLY = "download_only";
	public static String LANG_LIST = "lang_list";
	public static String BUNDLE_KEY = "get_bundle";

	private String[] mLangNames;
	private String[] mLangCodes;

	private AlertDialog mLanguageDialog;
	private RadioGroup mRadioLanguages;
	private ProgressDialog mProgressDialog;
	private AlertDialog mErrorDialog;
	private AlertDialog mTryLaterDialog;

	// Activity mode
	private boolean isDownloadOnly = false;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mLanguageDialog != null && mLanguageDialog.isShowing())
			mLanguageDialog.dismiss();
		if(mProgressDialog != null && mProgressDialog.isShowing())
			mProgressDialog.dismiss();
		if(mErrorDialog != null && mErrorDialog.isShowing())
			mErrorDialog.dismiss();
		if(mTryLaterDialog != null && mTryLaterDialog.isShowing())
			mTryLaterDialog.dismiss();
	}



	// Current local language
//	private String mCurrLocaleLang;

	// Current language id
	private String mCurrLangCode;
	private DictionaryItem mDicItem;
	
	private String[] mDicts;

	private static DownloadDictionaryTask ddTask = null;
	private static OnResultListener mResultListener = null;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get parameter
		Bundle bundle = getIntent().getBundleExtra(BUNDLE_KEY);
		if (bundle != null) {
			isDownloadOnly = bundle.getBoolean(DOWNLOAD_ONLY, false);
			mDicts = bundle.containsKey(LANG_LIST)  ? bundle.getStringArray(LANG_LIST) :  null;
		}

		// Load language
		mLangNames = getResources().getStringArray(R.array.language_names);
		mLangCodes = getResources().getStringArray(R.array.language_codes);

		Assert.assertTrue(mLangNames != null);
		Assert.assertTrue(mLangCodes != null);

		// Retrieve system language
//		Locale currLocale = java.util.Locale.getDefaultIndex();
//		mCurrLocaleLang = currLocale.getLanguage().toString();
		
		// Load language preference
		mCurrLangCode = getLanguagePreference(this);
		
		if(ddTask != null) {
			showProgressDialog();
			ddTask.setActivity(this);
		}
		else if(!isDownloadOnly)
			showLanguageDialog();
		else
			startDownload();
	}




	/**
	 * Show selecting dialog
	 */
	public void showLanguageDialog() {
//		boolean isChecked = false;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View languageLayout = inflater.inflate(R.layout.language,
				(ViewGroup) findViewById(R.id.llLanguage));

		builder.setTitle(R.string.install_select_language_title);
		builder.setView(languageLayout);
		builder.setCancelable(false);

		mLanguageDialog = builder.create();
		mLanguageDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					if (mDicItem != null && DictionaryUpdater.isDictionaryExist(LanguageSelector.this, mDicItem)) {
						success();
						mLanguageDialog.dismiss();
						LanguageSelector.this.finish();
						return true;
					} else {
						mLanguageDialog.dismiss();
						showTryLaterDialog();
					}

					return true;
				}
				return false;
			}
		});

		// Get component from dialog resource
		mRadioLanguages = (RadioGroup) languageLayout.findViewById(R.id.rgLanguage);
		// Load language options
		for (int i = 0 ; i < mLangNames.length ; i++) {
			RadioButton newOption = new RadioButton(this);
			newOption.setId(i);
			newOption.setText(mLangNames[i]);
			newOption.setTag(mLangCodes[i]);
			newOption.setPadding(
					getResources().getDimensionPixelSize(R.dimen.lang_option_padding_left), 
					0, 0, 0);
			newOption.setOnClickListener(this);
			mRadioLanguages.addView(newOption);

//			if (mCurrLocaleLang.equals(mLangCodes[i])) {
//				mRadioLanguages.check(i);
//				isChecked = true;
//			}
		}

//		if (isChecked != true)
//			mRadioLanguages.check(mLangNames.length - 1);

		getWindow().setBackgroundDrawableResource(R.drawable.page_background);
		mLanguageDialog.show();
	}



	/**
	 * Create the alert dialog to notice the fail to downloading
	 */
	private void showErrorDialog() {
		// Create Alert dialog
		mErrorDialog = new AlertDialog.Builder(this).create();
		mErrorDialog.setCancelable(false);
		mErrorDialog.setTitle(R.string.install_downloading_retry_title);
		mErrorDialog.setMessage(getResources().getString(R.string.install_downloading_retry_description));
		// mErrorDialog.setIcon(R.drawable.icon);
		mErrorDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
			getResources().getString(R.string.install_downloading_retry_yes), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mErrorDialog.dismiss();
					// restart downloading
					startDownload();
				}
			}
		);
		mErrorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
			getResources().getString(R.string.install_downloading_retry_no), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mErrorDialog.dismiss();

					if (!isDownloadOnly) {
						showTryLaterDialog();
					} else {
						fail();
						LanguageSelector.this.finish();
					}
				}
			}
		);
		mErrorDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					showLanguageDialog();
					return true;
				}
				return false;
			}
		});

		mErrorDialog.show();
	}

	/**
	 * Create the alert dialog to notice the information
	 */
	private void showTryLaterDialog() {
		mTryLaterDialog = new AlertDialog.Builder(this).create();
		mTryLaterDialog.setCancelable(false);
		mTryLaterDialog.setTitle(R.string.install_select_language_notice_title);
		mTryLaterDialog.setMessage(getResources().getString(R.string.install_select_language_notice_description));
		// mErrorDialog.setIcon(R.drawable.icon);
		mTryLaterDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getString(R.string.install_select_language_notice_ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mTryLaterDialog.dismiss();
				fail();
				finish();
			}
		}
				);
		mTryLaterDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					fail();
					mTryLaterDialog.dismiss();
					finish();
					return true;
				}
				return false;
			}
		});

		mTryLaterDialog.show();
	}



	private void showProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		
		Resources resource = getResources();

		mProgressDialog = new ProgressDialog(LanguageSelector.this);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setTitle(resource.getText(R.string.install_downloading_title));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgress(0);
		mProgressDialog.setMessage("");
		mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					mProgressDialog.dismiss();
					if(ddTask != null)
						ddTask.cancel(false);
					showErrorDialog();
					return true;
				}
				return false;
			}
		});
		
		mProgressDialog.show();
	}
	
	/**
	 * Gets dialog message 
	 * 
	 * @return
	 */
	private String getDialogMessage(String langCode){
		String description = String.format(getResources().getString(R.string.install_downloading_description), getLangNameFromCode(langCode));
		return description;
	}
	
	private void updateDialogMessage(String langCode){
		if(mProgressDialog != null && langCode != null){
			mProgressDialog.setMessage(getDialogMessage(langCode));
		}
	}
	

	@Override
	public void onClick(View arg0) {
		int selectedID;

		Assert.assertTrue(mLanguageDialog != null);
		Assert.assertTrue(mLanguageDialog.isShowing());

		selectedID = mRadioLanguages.getCheckedRadioButtonId();
		mCurrLangCode = (String)(((RadioButton)mRadioLanguages.findViewById(selectedID)).getTag());

		// Close selecting dialog
		mLanguageDialog.dismiss();
		mLanguageDialog = null;

		// Set the default language to other
		putLanguagePreference();

		// Return when user click the other option
		if (mCurrLangCode.equals(mLangCodes[mLangCodes.length - 1])) {
			fail();
			finish();
		} else {
			downloadDictionary();
		}
	}


	public void startDownload() {
		// Show progress dialog
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		
		if(mDicts != null)
			downloadDictionaries();
		else
			downloadDictionary();
	}
	
	public void downloadDictionaries(){		
		if(mDicts.length > 0){
			Log.d(KeyboardApp.LOG_TAG, "downloadDictionaries(): downloading dictionaries are " + Arrays.toString(mDicts));
			(ddTask = new DownloadDictionaryTask(this)).execute(mDicts);		
		} else {
			// no dictionaries to download
			AlertDialog dialog = new AlertDialog.Builder(LanguageSelector.this).create();
			dialog.setCancelable(false);
			dialog.setMessage(getString(R.string.dictionary_nothing_to_download));
			dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.dismiss();
						finish();
						return true;
					}
					return false;
				}
			});
			dialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			
			dialog.show();
			
			KeyboardApp.getApp().removeNotificationTry();
		}
	}

	
	public void downloadDictionary() {

		KeyboardApp.getApp().getUpdater().refreshDiclistFromDb();
		
		if(mCurrLangCode != null && mCurrLangCode.length() > 0){
			Log.d(KeyboardApp.LOG_TAG, "downloadDictionary(): downloading dictionary is " + mCurrLangCode);
			(ddTask = new DownloadDictionaryTask(this)).execute(new String[]{mCurrLangCode});
		}
		else {
			Log.v(KeyboardApp.LOG_TAG, "downloadDictionary(): Language is null");
		}
	}



	public static String getLanguagePreference(Context context) {
		SharedPreferences preference = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);       
		String language = preference.getString("language", context.getResources().getString(R.string.lang_code_default));

		return language;
	}



	// Save currency preference
	public void putLanguagePreference() {
		SharedPreferences.Editor preferenceEditor = getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE).edit();

		preferenceEditor.putString("language", mCurrLangCode);
		preferenceEditor.commit();
	}



	/**
	 * Get language name from value
	 */
	public String getLangNameFromCode(String prefix) {
		Assert.assertTrue(mLangCodes != null);
		
		return getLangNameFromCode(this, mLangNames, mLangCodes, prefix);
	}
	
	/**
	 * Get language name from value
	 */
	public static String getLangNameFromCode(Context context, String code) {
		String[] mLangNames;
		String[] mLangCodes;
		
		mLangNames = context.getResources().getStringArray(R.array.language_names);
		mLangCodes = context.getResources().getStringArray(R.array.language_codes);
		
		return getLangNameFromCode(context, mLangNames, mLangCodes, code);
	}
	
	public static String getLangNameFromCode(Context context, String[] mLangNames, String[] mLangCodes, String code) {
		for (int i = 0 ; i < mLangCodes.length ; i++) {
			if (code.equals(mLangCodes[i])) {
				return mLangNames[i];
			}
		}

		Log.d(KeyboardApp.LOG_TAG, "Unknown language code '" + code + "' in " + Arrays.toString(mLangCodes) + " - " + Arrays.toString(mLangNames));
		
		Assert.assertTrue(false);
		return null;
	}

	public static void setOnResultHandler(OnResultListener handler) {
		mResultListener = handler;
	}
	
	
	private static void success() {
		if(mResultListener != null)
			mResultListener.onSuccess();
	}


	private static void fail() {
		if(mResultListener != null)
			mResultListener.onFail();
	}
	
	/**
	 * Check connection to internet
	 */
	public boolean isDeviceOnline(){
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (cm.getActiveNetworkInfo() == null) {
	      return false;
	    }
	    return cm.getActiveNetworkInfo().isConnected();
	}


	private static class DownloadDictionaryTask extends AsyncTask<String, Integer, Boolean> {
		
		private DictionaryItem mCurrentItem;
		private LanguageSelector mActivity;
		
		private DownloadDictionaryTask(LanguageSelector activity) {
			mActivity = activity;
		}
		
		
		private void setActivity(LanguageSelector activity) {
			mActivity = activity;
		}

		protected void onPreExecute() {
			mActivity.showProgressDialog();
		}


		protected Boolean doInBackground(String... dicItems) {
			
			DictionaryUpdater updater = KeyboardApp.getApp().getUpdater();
			if(updater.getDictionaryList().size() == 0)
				updater.loadDictionaryList();
				
			for(String lang : dicItems){
				
				if(lang == null)
					continue;
				
				boolean isOnline = mActivity.isDeviceOnline();

				mCurrentItem = updater.getDictionaryItem(lang);
				
				publishProgress(0);

				try {
					// wait until we get the dictionary.xml
	
					if (mCurrentItem != null) {
						Log.v(KeyboardApp.LOG_TAG, "dictionary downloader: is need update " + mCurrentItem.isNeedUpdate + " ; is installed " + mCurrentItem.isInstalled + " ; is exist " + DictionaryUpdater.isDictionaryExist(mActivity, mCurrentItem));
						// Check dictionary existing
						if ((!mCurrentItem.isNeedUpdate && mCurrentItem.isInstalled && DictionaryUpdater.isDictionaryExist(mActivity, mCurrentItem) )) {
							fail();
							break;
						}

						// Calculate all size
						long totalSize = mCurrentItem.getDicTotalSize();

						DictionaryFileItem fileItem;
						float startPercent = 0, weight;

						// Download files to temp location
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							fileItem = mCurrentItem.fileItems.get(i);

							weight = ((float) fileItem.size / totalSize) * 100;

							// Download in temporary file
							downloadFile(fileItem, (int) startPercent, (int) weight);

							startPercent += weight;
						}

						// Move from temp location to databases filter
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							fileItem = mCurrentItem.fileItems.get(i);

							// Delete old file
							File destFile = new File(Utils.getInternalFilePath(mActivity, "databases/" + fileItem.filename));
							boolean result = destFile.exists();
							if(result)
								result = destFile.delete();

							// Move temp file
							File tempFile = new File(Utils.getInternalFilePath(mActivity, "files/" + fileItem.filename));
							result = tempFile.exists();
							result = tempFile.renameTo(destFile);
							if (!result) {
								throw new Exception("Rename failed");
							}
						}

						// Temporary
						// Delete old dictionaries
						File oldFile = new File(Utils.getInternalFilePath(mActivity, "files/en1.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/en2.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/en2.idx"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/es1.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/es2.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/es2.idx"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/de1.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/de2.dic"));
						oldFile.delete();
						oldFile = new File(Utils.getInternalFilePath(mActivity, "files/de2.idx"));
						oldFile.delete();
						// Temporary
						
	
						// Mark download complete
						publishProgress(100);
	
						mCurrentItem.isNeedUpdate = false;
						mCurrentItem.isInstalled = true;
						
						// Update database
						DatabaseHelper.safeGetDatabaseHelper(mActivity).saveDicItem(mCurrentItem, true);
						
						// Update notification tray
						//DictionaryUpdater.updateNotificationTray(LanguageSelector.this);
					} else {
						if(isOnline)
							throw new Exception("There is no dictionary item " + lang);
						else
							Log.e(KeyboardApp.LOG_TAG, "There is no dictionary item " + lang);
					}
				} catch (Exception e) {
					ErrorReport.reportShortError(e, mActivity, "dic_download_exception", debugDetails.toString());
					
					if(e != null)
						Log.e(KeyboardApp.LOG_TAG, e.getMessage());
					
					if(mCurrentItem != null) {
						// Delete temporary files
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							DictionaryFileItem fileItem;
							fileItem = mCurrentItem.fileItems.get(i);
							File tempFile = new File(fileItem.filename);
							tempFile.deleteOnExit();
						}
					}
					return false;
				}
			
			}
			
			

			return true;
		}



		/**
		 * Download remote file to mobile device
		 * @param url - remote file path
		 * @param destPath - destination mobile path
		 * @throws IOException - when network error occured
		 */
		private void downloadFile(DictionaryFileItem fileItem, int baseRate, int totalRate) throws IOException {
			// Open download stream
			URL url = new URL(getDownloadURL(fileItem.filename, ""));
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
			InputStream inStream = urlConn.getInputStream();
			
			debugLog("Downloading " + fileItem.filename);
			
			// Stop estimating for the calculation
			mActivity.mProgressDialog.setProgress(baseRate);

			// Create destination file
			FileOutputStream fout = mActivity.openFileOutput(fileItem.filename, MODE_PRIVATE);
			if (fout == null)
				throw new IOException("Cannot open output file");

			// Start download
			int readLength = 0, allLength = 0, currProgress = 0;
			final int BUFFER_SIZE = 1024 * 32;
			final byte[] mBuffer = new byte[BUFFER_SIZE];
			try {
				while (!isCancelled()) {
					// Reset buffer
					// Read segment
					readLength = inStream.read(mBuffer);
					if (readLength == -1)
						break;

					allLength += readLength;
					// Write segment to destination file
					fout.write(mBuffer, 0, readLength);

					debugLog("Read " + readLength + " bytes, total " + allLength + " bytes");

					currProgress = (int) (baseRate + (totalRate * allLength) / fileItem.size);

					publishProgress(currProgress);
				}
				debugLog("Finished reading " + fileItem.filename);

			} catch (IOException e) {
				ErrorReport.reportShortError(e, mActivity, "dic_download_file_exception", debugDetails.toString());
				Log.e(KeyboardApp.LOG_TAG, e.getMessage());

				// Remove downloaded file
				mActivity.deleteFile(fileItem.filename);

				throw e;
			} finally {
				urlConn.disconnect();
				inStream.close();
				fout.close();
				System.gc();
			}

			if (isCancelled())
				throw new IOException("Stop download requested (" + fileItem.filename + ")");

			if (allLength != fileItem.size)
				throw new IOException("Download incomplete (" + fileItem.filename + "): " + allLength + " != " + fileItem.size);
		}



		/**
		 * Update progress on UI thread
		 */
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(mActivity.mProgressDialog == null || !mActivity.mProgressDialog.isShowing() || isCancelled())
				return;

			mActivity.mProgressDialog.setIndeterminate(false);
			mActivity.mProgressDialog.setProgress(progress[0].intValue());
			if(progress[0].intValue() == 0){
				if(mCurrentItem != null){
					Log.d(KeyboardApp.LOG_TAG, "mCurrentItem index = " + mCurrentItem.id + ",  lang = " + mCurrentItem.lang + ", version " + mCurrentItem.version);
					mActivity.updateDialogMessage(mCurrentItem.lang);					
				}
			}
			
		}



		/**
		 * Retrieve download url from specified language code
		 * @param 
		 * @return
		 */
		private String getDownloadURL(String langPrefix, String extension) {
			String url;

			url = mActivity.getResources().getString(R.string.install_base_url);

			Assert.assertTrue(langPrefix != null);
			Assert.assertTrue(extension != null);
			url += langPrefix;
			url += extension;

			return url;
		}


		protected void onPostExecute(Boolean result) {
			if(mActivity.mProgressDialog != null && mActivity.mProgressDialog.isShowing())
				mActivity.mProgressDialog.dismiss();
			
			ddTask = null;

			if(result == false)
				// TODO: Check if Activity is still visible
				mActivity.showErrorDialog();
			else {
				if(!isCancelled()) 
					success();
				
				mActivity.finish();
			}
		}
		
		
		StringBuilder debugDetails = new StringBuilder();
		private void debugLog(String detail) {
			String now = (new SimpleDateFormat("dd-MMM-yyyy kk:mm")).format(new Date());
			String entry = now + ": " + detail + "\n";
			debugDetails.append(entry);
		}
	}
	
	
	
	public static String getNameFromCode(Context context, String langCode) {
		String[] names = context.getResources().getStringArray(R.array.language_names); 
		String[] codes = context.getResources().getStringArray(R.array.language_codes);

		for (int i = 0 ; i < codes.length ; i++) {
			String code = codes[i];
			if (code.equals(langCode)) {
				return names[i];
			}
		}

		return null;
	}
	
	
	
	public static String getCodeFromName(Context context, String langName) {
		String[] names = context.getResources().getStringArray(R.array.language_names); 
		String[] codes = context.getResources().getStringArray(R.array.language_codes);

		for (int i = 0 ; i < names.length ; i++) {
			String name = names[i];
			if (name.equals(langName)) {
				return codes[i];
			}
		}

		return null;
	}
}