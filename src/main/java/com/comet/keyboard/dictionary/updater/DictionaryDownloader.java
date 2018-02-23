package com.comet.keyboard.dictionary.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import junit.framework.Assert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.settings.OnResultListener;
import com.comet.keyboard.settings.Settings;
import com.comet.keyboard.util.DatabaseHelper;
import com.comet.keyboard.util.ErrorReport;
import com.comet.keyboard.util.Utils;

public class DictionaryDownloader extends Activity {
	public static String LANG_LIST = "lang_list";

	private String[] mLangNames;
	private String[] mLangCodes;

	private ProgressDialog mProgressDialog;
	private AlertDialog mErrorDialog;

	// Current language id
	private String mCurrLangCode;
	private String[] mDicts;

	private static DownloadDictionaryTask mddTask = null;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get dictionaries to download
		final Bundle bundle = getIntent().getBundleExtra(Settings.BUNDLE_KEY);
		if (bundle != null) {
			mDicts = bundle.containsKey(LANG_LIST) ? bundle.getStringArray(LANG_LIST) :  null;
		}

		// Load language
		mLangNames = getResources().getStringArray(R.array.language_names);
		mLangCodes = getResources().getStringArray(R.array.language_codes);

		// Load language preference
		mCurrLangCode = getLanguagePreference(this);
		
		if(mddTask != null) {
			showProgressDialog();
			mddTask.setActivity(this);
		} else {
			startDownload();
		}
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		if(mErrorDialog != null && mErrorDialog.isShowing()) {
			mErrorDialog.dismiss();
		}
	}


	private static OnResultListener mResultListener;
	public static void setOnResultListener(OnResultListener listener) {
		mResultListener = listener;
	}


	/**
	 * Create the alert dialog to notice the fail to downloading
	 */
	private void showErrorDialog() {
		// Create error dialog
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mErrorDialog = new AlertDialog.Builder(
						this,
						AlertDialog.THEME_HOLO_DARK)
					.create();
		} else {
			mErrorDialog = new AlertDialog.Builder(this).create();
		}
		mErrorDialog.setCancelable(false);
		mErrorDialog.setTitle(R.string.install_downloading_retry_title);
		mErrorDialog.setMessage(
				getResources().getString(R.string.install_downloading_retry_description));
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
					fail();
				}
			}
		);

		mErrorDialog.show();
	}


	private void showProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}

		Resources resource = getResources();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mProgressDialog = new ProgressDialog(
					DictionaryDownloader.this,
					AlertDialog.THEME_HOLO_DARK);
		} else {
			mProgressDialog = new ProgressDialog(DictionaryDownloader.this);
		}
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
					if(mddTask != null) {
						mddTask.cancel(false);
					}
					mddTask = null;
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
	 */
	private String getDialogMessage(String langCode){
		return String.format(
				getResources().getString(R.string.install_downloading_description),
				getLangNameFromCode(langCode));
	}
	

	private void updateDialogMessage(String langCode){
		if(mProgressDialog != null && langCode != null){
			mProgressDialog.setMessage(getDialogMessage(langCode));
		}
	}
	

	public void startDownload() {
		// Show progress dialog
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}

		if(mDicts != null) {
			downloadDictionaries();
		} else {
			downloadDictionary();
		}
	}
	

	public void downloadDictionaries(){
		if(mDicts.length > 0){
			Log.d(
					KeyboardApp.LOG_TAG,
					"downloadDictionaries(): downloading dictionaries are "
							+ Arrays.toString(mDicts));
			(mddTask = new DownloadDictionaryTask(this)).execute(mDicts);		
		} else {
			// no dictionaries to download
			final AlertDialog dialog
					= new AlertDialog.Builder(
							DictionaryDownloader.this)
					.create();
			dialog.setCancelable(false);
			dialog.setMessage(getString(R.string.dictionary_nothing_to_download));
			dialog.setButton(DialogInterface.BUTTON_POSITIVE,
					getString(R.string.ok), new DialogInterface.OnClickListener() {
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
		KeyboardApp.getApp().getUpdater().refreshDictionaryListFromDb();
		
		if(mCurrLangCode != null && mCurrLangCode.length() > 0) {
			Log.d(KeyboardApp.LOG_TAG,
					"downloadDictionary(): downloading dictionary is " + mCurrLangCode);
			(mddTask = new DownloadDictionaryTask(this)).execute(mCurrLangCode);
		} else {
			Log.v(KeyboardApp.LOG_TAG, "downloadDictionary(): Language is null");
		}
	}


	public static String getLanguagePreference(final Context context) {
		return context.getSharedPreferences(
					Settings.SETTINGS_FILE,
					Context.MODE_PRIVATE)
				.getString(
						"language",
						context.getResources().getString(R.string.lang_code_default));
	}


	/**
	 * Get language name from value
	 */
	public String getLangNameFromCode(final String code) {
		Assert.assertTrue(mLangCodes != null);
		return getLangNameFromCode(mLangNames, mLangCodes, code);
	}
	

	public static String getLangNameFromCode(
			final String[] mLangNames,
			final String[] mLangCodes,
			final String code) {
		for (int i = 0 ; i < mLangCodes.length ; i++) {
			if (code.equals(mLangCodes[i])) {
				return mLangNames[i];
			}
		}

		Log.d(KeyboardApp.LOG_TAG,
				"Unknown language code '"
						+ code + "' in "
						+ Arrays.toString(mLangCodes)
						+ " - " + Arrays.toString(mLangNames));
		
		return null;
	}


	private void success() {
		if(mResultListener != null) {
			mResultListener.onSuccess();
		}

		setResult(1);
		
		finish();
	}


	private void fail() {
		if(mResultListener != null) {
			mResultListener.onFail();
		}

		setResult(0);

		finish();
	}
	

	/**
	 * Check connection to internet
	 */
	public boolean isDeviceOnline(){
	    final ConnectivityManager cm
				= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    return cm != null
				&& cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnected();
	}


	private static class DownloadDictionaryTask extends AsyncTask<String, Integer, Boolean> {
		private DictionaryItem mCurrentItem;
		private DictionaryDownloader mActivity;
		
		private DownloadDictionaryTask(final DictionaryDownloader activity) {
			mActivity = activity;
		}
		
		
		private void setActivity(final DictionaryDownloader activity) {
			mActivity = activity;
		}


		protected void onPreExecute() {
			mActivity.showProgressDialog();
		}


		protected Boolean doInBackground(final String... dicItems) {

			final DictionaryUpdater updater = KeyboardApp.getApp().getUpdater();
			updater.loadDictionaryList();

			for(String lang : dicItems){
				if(lang == null) {
					continue;
				}

				final boolean isOnline = mActivity.isDeviceOnline();

				mCurrentItem = updater.getDictionaryItem(lang);
				
				publishProgress(0);

				try {
					// wait until we get the dictionary.xml
					if (mCurrentItem != null) {
						Log.v(
								KeyboardApp.LOG_TAG,
								"dictionary downloader: is need update "
										+ mCurrentItem.isNeedUpdate
										+ " ; is installed "
										+ mCurrentItem.isInstalled
										+ " ; is exist "
										+ DictionaryUpdater.isDictionaryExist(
												mActivity,
												mCurrentItem));
						// Check dictionary existing
						if ((!mCurrentItem.isNeedUpdate
								&& mCurrentItem.isInstalled
								&& DictionaryUpdater.isDictionaryExist(mActivity, mCurrentItem) )) {
							mActivity.success();
							break;
						}

						// Calculate all size
						final long totalSize = mCurrentItem.getDicTotalSize();

						// Download files to temp location
						float startPercent = 0;
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							final DictionaryFileItem fileItem = mCurrentItem.fileItems.get(i);

							final float weight = ((float) fileItem.size / totalSize) * 100;

							// Download in temporary file
							downloadFile(fileItem, (int) startPercent, (int) weight);

							startPercent += weight;
						}

						// Move from temp location to databases filter
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							final DictionaryFileItem fileItem = mCurrentItem.fileItems.get(i);

							// Delete old file
							final File destFile
									= new File(
											Utils.getInternalFilePath(
													mActivity,
													"databases/" + fileItem.filename));
							if(destFile.exists()) {
								destFile.delete();
							}

							// Move temp file
							final File tempFile
									= new File(
											Utils.getInternalFilePath(
													mActivity,
													"files/" + fileItem.filename));
							if (!tempFile.exists() || !tempFile.renameTo(destFile)) {
								throw new Exception("Rename failed");
							}
						}

						// Mark download complete
						publishProgress(100);
	
						mCurrentItem.isNeedUpdate = false;
						mCurrentItem.isInstalled = true;
						
						// Update database
						DatabaseHelper.safeGetDatabaseHelper(mActivity)
								.saveDicItem(mCurrentItem, true);
					} else {
						if(isOnline) {
							throw new Exception("There is no dictionary item " + lang);
						} else {
							Log.e(KeyboardApp.LOG_TAG, "There is no dictionary item " + lang);
						}
					}
				} catch (Exception e) {
					ErrorReport.reportShortError(
							e,
							mActivity,
							"dic_download_exception",
							debugDetails.toString());

					String message = e.getMessage();
					if(message == null)
						message = "(no message)";
					Log.e(KeyboardApp.LOG_TAG, message);

					if(mCurrentItem != null) {
						// Delete temporary files
						for (int i = 0 ; i < mCurrentItem.fileItems.size() ; i++) {
							final DictionaryFileItem fileItem = mCurrentItem.fileItems.get(i);
							final File tempFile = new File(fileItem.filename);
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
		 * @throws IOException - when network error occured
		 */
		private void downloadFile(
				final DictionaryFileItem fileItem,
				final int baseRate,
				final int totalRate) throws IOException {

			// Open download stream
			final URL url = new URL(getDownloadURL(fileItem.filename));
			HttpURLConnection.setFollowRedirects(true);
			final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			final InputStream inStream = urlConn.getInputStream();
			
			debugLog("Downloading " + fileItem.filename);
			
			// Stop estimating for the calculation
			if(mActivity.mProgressDialog != null) {
				mActivity.mProgressDialog.setProgress(baseRate);
			}

			// Create destination file
			final FileOutputStream fout
					= mActivity.openFileOutput(fileItem.filename, MODE_PRIVATE);
			if (fout == null) {
				throw new IOException("Cannot open output file");
			}

			// Start download
			int allLength = 0;
			final int BUFFER_SIZE = 1024 * 32;
			final byte[] mBuffer = new byte[BUFFER_SIZE];
			try {
				while (!isCancelled()) {
					// Reset buffer
					// Read segment
					final int readLength = inStream.read(mBuffer);
					if (readLength == -1) {
						break;
					}

					allLength += readLength;
					// Write segment to destination file
					fout.write(mBuffer, 0, readLength);

					debugLog("Read " + readLength + " bytes, total " + allLength + " bytes");

					final int currProgress
							= (int) (baseRate + (totalRate * allLength) / fileItem.size);
					publishProgress(currProgress);
				}
				debugLog("Finished reading " + fileItem.filename);

			} catch (final IOException e) {
				ErrorReport.reportShortError(
						e,
						mActivity,
						"dic_download_file_exception",
						debugDetails.toString());
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

			if (isCancelled()) {
				throw new IOException(
						"Stop download requested ("
								+ fileItem.filename
								+ ")");
			}

			if (allLength != fileItem.size) {
				throw new IOException(
						"Download incomplete (" + fileItem.filename + "): "
								+ allLength
								+ " != "
								+ fileItem.size);
			}
		}


		/**
		 * Update progress on UI thread
		 */
		@Override
		protected void onProgressUpdate(final Integer... progress) {
			if(mActivity.mProgressDialog == null
					|| !mActivity.mProgressDialog.isShowing()
					|| isCancelled()) {
				return;
			}

			mActivity.mProgressDialog.setIndeterminate(false);
			mActivity.mProgressDialog.setProgress(progress[0]);
			if(progress[0] == 0){
				if(mCurrentItem != null){
					Log.d(
							KeyboardApp.LOG_TAG,
							"mCurrentItem index = "
									+ mCurrentItem.id
									+ ",  lang = "
									+ mCurrentItem.lang
									+ ", version "
									+ mCurrentItem.version);
					mActivity.updateDialogMessage(mCurrentItem.lang);					
				}
			}
			
		}


		/**
		 * Retrieve download url from specified language code
		 */
		private String getDownloadURL(final String langCode) {
			String url = mActivity.getResources().getString(R.string.install_base_url);

			Assert.assertTrue(langCode != null);
			url += langCode;

			return url;
		}


		protected void onPostExecute(final Boolean result) {
			if(mActivity.mProgressDialog != null && mActivity.mProgressDialog.isShowing()) {
				mActivity.mProgressDialog.dismiss();
			}

			mddTask = null;

			if(!result) {
				// TODO: Check if Activity is still visible
				mActivity.showErrorDialog();
			} else {
				if(!isCancelled()) {
					mActivity.success();
				}
			}
		}
		

		final StringBuilder debugDetails = new StringBuilder();
		private void debugLog(String detail) {
			String now = (new SimpleDateFormat(
						"dd-MMM-yyyy kk:mm",
						Locale.getDefault()))
					.format(new Date());
			String entry = now + ": " + detail + "\n";
			debugDetails.append(entry);
		}
	}
}