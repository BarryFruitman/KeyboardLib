/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */

package com.comet.keyboard.util;


import com.comet.keyboard.KeyboardApp;
import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.NinePatch;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

public class Utils {
	public static final int DAY_MILISECONDS = 24 * 60 * 1000;
	
	public static String formatStringWithAppName(Context context, int resId) {
		return formatStringWithAppName(context, context.getResources().getString(resId));
	}
	public static String formatStringWithAppName(Context context, String format) {
		return String.format(format, context.getResources().getString(R.string.ime_short_name));
	}
	
//	/*************************************************************************************
//	 * APP & SYSTEM FUNCTIONS
//	 ************************************************************************************/
//	public static String getGmailAcount(Context context) {
//		AccountManager manager = AccountManager.get(context);
//		Account[] accounts = manager.getAccountsByType("com.google");
//		String acount = "";
//
//		if (accounts != null && accounts.length > 0)
//			acount = accounts[0].name;
//
//		return acount;
//	}
	
	/**
	 * Check to see if a recognition activity is present
	 * @param context
	 * @return
	 */
	public static boolean isExistVoiceRecognizeActivity(Context context) {
		boolean isExist;
		
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(
		  new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		isExist = (activities.size() != 0);
		
		return isExist;
	}
	
	public static String getDeviceID(Context context) {
		String androidID = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		
		return androidID;
	}
	
	/*************************************************************************************
	 * STRING FUNCTIONS
	 ************************************************************************************/
	public static int stringToInt(String value){
	    try
	    {
	      // the String to int conversion happens here
	      int i = Integer.parseInt(value.trim());
	      return i;
	    }
	    catch (NumberFormatException nfe)
	    {
	      System.out.println("NumberFormatException: " + nfe.getMessage());
	    }
		return -1;
	}
	
	// Get string from object
	public static String getClassString(Object object, String prefix) {
		StringBuilder result = new StringBuilder();
		Class<? extends Object> cls;
		
		if (object == null)
			return "";
		
		cls = object.getClass();
		result.append("\n");
		result.append(prefix);
		result.append(cls.getName());
		result.append(" = {");
		
		try {
			// Retrieve all variables.
			Field[] fields = cls.getDeclaredFields();
			Field field;
			String name, value;
			
			for (int i = 0 ; i < fields.length ; i++) {
				field = fields[i];
				
				name = field.getName();
				value = "";
				try {
					field.setAccessible(true);
					if ((field.getModifiers() & Modifier.FINAL) > 0)
						continue;
				} catch (Exception e) {
				}
				
				result.append("\n" + prefix + "\t" + prefix);
				result.append(name);
				result.append(" = ");
				result.append(value);
			}
		} catch (Exception e) {
			result.append("exception = " + e.getMessage());
		}
		result.append("\n" + prefix + "}");
		
		return result.toString();
	}
	
	public static void append(StringBuilder buffer, String prefix, String value) {
		buffer.append(prefix);
		buffer.append(value);
	}
	
	public static void appendLine(StringBuilder buffer, String prefix, String value) {
		buffer.append("\n");
		buffer.append(prefix);
		buffer.append(value);
	}
	/*************************************************************************************
	 * TIME & DATE FUNCTIONS
	 ************************************************************************************/
	public static long getTimeMilis() {
		return System.currentTimeMillis();
	}
	
	public static long getDateTimeMilis() {
		Date nowDate = new Date();
		nowDate.setHours(0);
		nowDate.setMinutes(0);
		nowDate.setSeconds(0);

		return nowDate.getTime();
	}
	
	public static String getStringDate(long longDate){
		String resultDate;

		// TimeZone mytimezone = TimeZone.getDefaultIndex();
		// int offset = mytimezone.getRawOffset();
		
		//long tmpPubDate = Long.parseLong(longDate + "000") + offset;
		long tmpPubDate = Long.parseLong(longDate + "000");
		//Long tmpPubDate = Long.valueOf(longDate*1000);

		Date publicationDate = new Date(tmpPubDate);

		SimpleDateFormat formatter_date = new SimpleDateFormat ( "dd MMM yyyy",Locale.ENGLISH );
		resultDate = formatter_date.format(publicationDate);
		
		return resultDate;
	}
	
	public static String getEnStringDate(String dateString){
		
		SimpleDateFormat shortFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat mediumFormat = new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH);
		
		String resultDate = null;
		
		try {
			resultDate = mediumFormat.format(shortFormat.parse(dateString));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return resultDate;
	}
	
	public static String getDateFromEnDate(String dateEnString){
		
		SimpleDateFormat formatter_one = new SimpleDateFormat ( "dd MMM yyyy",Locale.ENGLISH );
		SimpleDateFormat formatter_two = new SimpleDateFormat ( "yyyy-MM-dd" );

		ParsePosition pos = new ParsePosition ( 0 );
		Date frmTime = formatter_one.parse ( dateEnString, pos );
		String returnString = formatter_two.format ( frmTime );
		
		return returnString;
	}
	
	public static String getDateString(long mTime, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat (format);
		String dateString;
		
		dateString = dateFormat.format(new Date(mTime));
		
		return dateString;
	}
	
	/**
	 * Get date from string
	 * @param dateStr
	 * @return
	 */
	public static Date getDate(String dateStr) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date date = null;
		
		try {
			date = dateFormat.parse(dateStr);
			Log.i("date", "year = " + (1900 + date.getYear()) + ", month = " + date.getMonth() + ", date = " + date.getDate());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return date;
	}
	
	/*************************************************************************************
	 * IO FUNCTIONS
	 ************************************************************************************/
	public static boolean existSDCard(Context context) {
		String state;
		
		state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
        
	}
	
	// File IO function
	public static String getFilePath(Context context, String path) {
		String fullPath;
		
		if (existSDCard(context)) {
			fullPath = getSDFilePath(context, path);
		} else {
			fullPath = getInternalFilePath(context, path);
		}
        
		return fullPath;
	}
	
	public static String getSDFilePath(Context context, String path) {
		String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    	String packageName = context.getApplicationContext().getPackageName();
    	sdcardPath += "/" + packageName + "/" + path;
    	
    	return sdcardPath;
	}
	
	public static String getInternalFilePath(Context context, String path) {
    	String packageName = context.getApplicationContext().getPackageName();
    	String fullPath = "/data/data/" + packageName + "/" + path;
    	
    	return fullPath;
	}
	
	public static boolean createDir(String path) {
		boolean ret = true;
		
		File fileIn = new File(path);
		
		if (!fileIn.exists())
			ret = fileIn.mkdirs();
		
		return ret;
	}
	
	/**
	 * Ensure directory existing
	 * @param context
	 * @param path
	 */
	public static void ensureDir(Context context, String path) {
		boolean result;
		
		path = getFilePath(context, path);
		
		File file = new File(path);
		if (!file.exists()) {
			result = file.mkdirs();
			if (result == false) {
				Log.e("Storage Error", "There is no storage to cache app data");
			}
		}
	}
	
	public static boolean ensureAbsDir(Context context, String path) {
		boolean result;
		
		File file = new File(path);
		if (!file.exists()) {
			result = file.mkdirs();
			if (result == false) {
				Log.e("Storage Error", "There is no storage to cache app data");
				return false;
			}
		}
		return true;
	}
	
	// Copy dir from source to dest
	public static boolean copyDir(String srcPath, String dstPath) {
		File srcDir = new File(srcPath);
		String orgSrcPath, orgDstPath;
		
		orgSrcPath = srcPath;
		orgDstPath = dstPath;

		// ensure destination directory
		if (!Utils.createDir(dstPath))
			return false;
		
		// Copy all files from free app to paid app
		File[] srcList = srcDir.listFiles();
		for (File srcFile : srcList) {
			srcPath = srcFile.getAbsolutePath();
			dstPath = orgDstPath
					+ srcPath.substring(srcPath.indexOf(orgSrcPath)
							+ orgSrcPath.length());
			if (srcFile.isDirectory()) {
				copyDir(srcPath, dstPath);
				continue;
			}

			copyFile(srcPath, dstPath);
		}
		
		return true;
	}

	// Copy file from source to dest
	public static boolean copyFile(String srcPath, String dstPath) {
		FileInputStream srcInput;
		FileOutputStream dstOutput;
		
		File destFile;

		try {
			File srcFile = new File(srcPath);
			if (!srcFile.exists())
				return false;
			
			srcInput = new FileInputStream(srcFile);

			destFile = new File(dstPath);
			destFile.createNewFile();
			
			dstOutput = new FileOutputStream(destFile);
			byte buffer[] = new byte[2048];
			do {
				int bytesRead = srcInput.read(buffer);
				if (bytesRead <= 0) {
					break;
				}
				dstOutput.write(buffer, 0, bytesRead);

			} while (true);
			srcInput.close();
			dstOutput.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}


	public static void deleteDir(File dir) {
        Log.d("DeleteRecursive", "DELETEPREVIOUS TOP" + dir.getPath());
        if (dir.exists() && dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) 
            {
               File temp =  new File(dir, children[i]);
               if(temp.isDirectory())
               {
                   Log.d("DeleteRecursive", "Recursive Call" + temp.getPath());
                   deleteDir(temp);
               }
               else
               {
                   Log.d("DeleteRecursive", "Delete File" + temp.getPath());
                   boolean b = temp.delete();
                   if(b == false)
                   {
                       Log.d("DeleteRecursive", "DELETE FAIL");
                   }
               }
            }

            dir.delete();
        }
    }
	/*************************************************************************************
	 * LANGUAGE & IME FUNCTIONS
	 ************************************************************************************/
	/**
	 * Switch language of APP
	 * @param context
	 * @param pLocale
	 */
	public static void switchLanguage(Context context, Locale pLocale) { 
        Resources res = context.getResources(); 
        DisplayMetrics dm = res.getDisplayMetrics(); 
        Configuration conf = res.getConfiguration(); 
        conf.locale = pLocale;
        res.updateConfiguration(conf, dm);
    } 
	
	/**
	 * Check enabled status
	 * @return
	 */
	public static boolean isEnabledIME(Context context) {
		String IMEs = android.provider.Settings.Secure.getString(context.getApplicationContext().getContentResolver(),
				android.provider.Settings.Secure.ENABLED_INPUT_METHODS); 
		
		if(IMEs.contains(context.getString(R.string.ime_servicename))) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if this app is the default IME
	 * @return	true if this app is the default IME
	 */
	public static boolean isSelectedToDefault(Context context) {
		String defaultIME = android.provider.Settings.Secure.getString(context.getApplicationContext()
				.getContentResolver(), android.provider.Settings.Secure.DEFAULT_INPUT_METHOD);

		if (defaultIME.contains(context.getString(R.string.ime_servicename))) {
			return true;
		}

		return false;
	}
	
	/*************************************************************************************
	 * UI FUNCTIONS
	 ************************************************************************************/
	public static boolean isLandscapeScreen(Context context) {
		int orientation = context.getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			return true;
		
		return false;
	}
	/**
	 * Retrieve bitmap drawable object from specified image path
	 */
	/*
	 * This version tries to get a NinePatchDrawable directly from the resource stream
	 */
//	private static int mSuccess = 0;
	public static Drawable getBitmapDrawable(Context context, String imagePath) throws IOException {
		return getBitmapDrawable3(context, imagePath);
		
//		if(mSuccess == 1)
//			return getBitmapDrawable1(context, imagePath);
//		else if(mSuccess == 2)
//			return getBitmapDrawable2(context, imagePath);
//		else if(mSuccess == 3)
//			return getBitmapDrawable3(context, imagePath);
//		else
//			return getBitmapDrawable1(context, imagePath);
	}
	
	
	/**
	 * Retrieves bitmap drawable object from specified image path 
	 * 
	 * @param context
	 * @param imagePath
	 * @param colorize
	 * @return
	 * @throws IOException
	 */
	 public static Drawable getBitmapDrawable(Context context, String imagePath, int colorize) throws IOException {
	    Drawable bitmap = getBitmapDrawable(context, imagePath);
	    bitmap.mutate().setColorFilter(colorize, Mode.MULTIPLY);
	    return bitmap;
	  }
	  
	
	
	public static Drawable getBitmapDrawable3(Context context, String imagePath) {
		String resourceName = context.getString(R.string.ime_packagename) + ":drawable/" + imagePath.replace('/', '_').replace(".9.png", "").replace(".png", "");
		int drawableId = context.getResources().getIdentifier(resourceName, null, null);
		if(drawableId <= 0)
			return null;

		return context.getResources().getDrawable(drawableId, null);
	}

	
	
	public static BitmapDrawable safeGetBitmapDrawable(Context context, String imagePath, int maxWidth, int maxHeight) {
		Assert.assertTrue(imagePath != null);
		
		BitmapDrawable mDrawable = null;
		Options option = getBitmapInfo(imagePath);
		int width = option.outWidth;
		int height = option.outHeight;
		
		
		try {
			if (width > maxWidth || height > maxHeight)
				throw new OutOfMemoryError();
			mDrawable = new BitmapDrawable(context.getResources(), imagePath);
		} catch (OutOfMemoryError error) {
			Bitmap bitmap;
			float rateX = (float)width / maxWidth;
			float rateY = (float)height / maxHeight;
			float rate = Math.max(rateX, rateY);
			
			width = (int)((float)width / rate);
			height = (int)((float)height / rate);
			
			bitmap = makeThumb(imagePath, width, height);
			if (bitmap != null) {
				mDrawable = new BitmapDrawable(context.getResources(), bitmap);
			}
		}
		
		return mDrawable;
	}
	
	public static Options getBitmapInfo(String pathName) {
		Options options = new Options();

		options.inJustDecodeBounds = true;
		options.outWidth = 0;
		options.outHeight = 0;
		options.inSampleSize = 1;

		BitmapFactory.decodeFile(pathName, options);
		
		return options;
	}
	
	public static Bitmap makeThumb(String pathName, int iHeight, int iWidth) {
		Options options;
		
		options = getBitmapInfo(pathName);

		try {
			if (options.outWidth > 0 && options.outHeight > 0) {
				// Now see how much we need to scale it down.
				int widthFactor = (options.outWidth + iWidth - 1) / iWidth;
				int heightFactor = (options.outHeight + iHeight - 1) / iHeight;

				widthFactor = Math.max(widthFactor, heightFactor);
				widthFactor = Math.max(widthFactor, 1);

				// Now turn it into a power of two.
				if (widthFactor > 1) {
					if ((widthFactor & (widthFactor - 1)) != 0) {
						while ((widthFactor & (widthFactor - 1)) != 0) {
							widthFactor &= widthFactor - 1;
						}

						widthFactor <<= 1;
					}
				}
				options.inSampleSize = widthFactor;
				options.inJustDecodeBounds = false;
				Bitmap imageBitmap = BitmapFactory
						.decodeFile(pathName, options);
				BitmapDrawable drawable = new BitmapDrawable(imageBitmap);
				drawable.setGravity(Gravity.CENTER);
				drawable.setBounds(0, 0, iHeight, iWidth);
				return drawable.getBitmap();
			}
		} catch (java.lang.OutOfMemoryError e) {
			// TODO: handle exception
			Log.e(KeyboardApp.appName, e.getMessage());
		}

		return null;
	}
	
//	public static int getScreenWidth(Context context) {
//		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//		int width = display.getWidth();
//
//		return width;
//	}
//
//	public static int getScreenHeight(Context context) {
//		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//		int height = display.getHeight();
//
//		return height;
//	}

	/**
	 * Show out of memory dialog
	 * @param context
	 */
	public static void showOutOfMemoryDlg(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setTitle(R.string.outofmemory_error_title);
		builder.setMessage(R.string.outofmemory_error_message);

		AlertDialog errDlg = builder.create();
		errDlg.setButton(DialogInterface.BUTTON_POSITIVE, 
				context.getResources().getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}
		);
		errDlg.show();
	}

	
	
//	public static boolean isHoneycomb() {
//	    // Can use static final constants like HONEYCOMB, declared in later versions
//	    // of the OS since they are inlined at compile time. This is guaranteed behavior.
//	    return Build.VERSION.SDK_INT >= Utils.VersionCodes.HONEYCOMB;
//	}

	public static boolean isTablet(Context context) {
		/*
		int size1 = (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK);
		int size2 = Configuration.SCREENLAYOUT_SIZE_LARGE;
		*/
		
	    return (context.getResources().getConfiguration().screenLayout
	            & Configuration.SCREENLAYOUT_SIZE_MASK)
	            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

//	public static boolean isHoneycombTablet(Context context) {
//	    return isHoneycomb() && isTablet(context);
//	}
	
	
	/**************************************
	 * TEMPORARY
	 **************************************/
	private static int mErrorReported = 0;
	public static void reportError_getBitmapDrawable(Context context, Throwable e, String imagePath) {

		if(mErrorReported++ > 6)
			return;

		ErrorReport errorReport = new ErrorReport(context, e, "getBitmapDrawable");

		try {
			errorReport.putSharedPrefs(Settings.SETTINGS_FILE);
			errorReport.putParam("image_path", imagePath);
		} catch (Exception e2) {
			errorReport.putParam("meta_error", e2.toString());
		}

		errorReport.post();
	}
}
