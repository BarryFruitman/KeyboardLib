/*
 * For the full copyright and license information, please view the LICENSE file that was distributed
 * with this source code. (c) 2011
 */

package com.comet.keyboard.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.comet.keyboard.KeyboardApp;

/**
 * HttpLogSender
 * 
 */
public abstract class HttpLogSender extends AsyncTask<File, Integer, Boolean> {
	
	/**
	 * Sending logs to a server
	 */
	protected Boolean doInBackground(File... params) {
//		HttpURLConnection connection = null;
//		DataOutputStream outputStream = null;
//
//		File file = params[0];
//		String urlServer = "http://www.cometapps.com/typesmart/debug/upload_log.php";
//		String lineEnd = "\r\n";
//		String twoHyphens = "--";
//		String boundary =  "*****";
//		String destName = Utils.getDeviceID(KeyboardApp.getApp()) + "-" + System.currentTimeMillis() + ".log";
//
//		int bytesRead, bytesAvailable, bufferSize;
//		byte[] buffer;
//		int maxBufferSize = 1*1024*1024;
//
//		try
//		{
//			FileInputStream fileInputStream = new FileInputStream(file);
//
//			URL url = new URL(urlServer);
//			connection = (HttpURLConnection) url.openConnection();
//
//			// Allow Inputs & Outputs
//			connection.setDoInput(true);
//			connection.setDoOutput(true);
//			connection.setUseCaches(false);
//
//			// Enable POST method
//			connection.setRequestMethod("POST");
//
//			connection.setRequestProperty("Connection", "Keep-Alive");
//			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
//
//			outputStream = new DataOutputStream( connection.getOutputStream() );
//			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
//			outputStream.writeBytes("Content-Disposition: form-data; name=\"log_file\";filename=\"" + destName + "\"" + lineEnd);
//			outputStream.writeBytes(lineEnd);
//
//			bytesAvailable = fileInputStream.available();
//			bufferSize = Math.min(bytesAvailable, maxBufferSize);
//			buffer = new byte[bufferSize];
//
//			// Read file
//			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//
//			while (bytesRead > 0)
//			{
//				outputStream.write(buffer, 0, bufferSize);
//				bytesAvailable = fileInputStream.available();
//				bufferSize = Math.min(bytesAvailable, maxBufferSize);
//				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//			}
//
//			outputStream.writeBytes(lineEnd);
//			outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
//
//			// Responses from the server (code and message)
//			int serverResponseCode = connection.getResponseCode();
//			String serverResponseMessage = connection.getResponseMessage();
//
//	        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
//			String line;
//
//			// Clear buffer
//		    StringBuilder response = new StringBuilder();
//			response.setLength(0);
//			while ((line = reader.readLine()) != null)
//				response.append(line);
//			reader.close();
//
//			fileInputStream.close();
//			outputStream.flush();
//			outputStream.close();
//		} catch (Exception ex) {
//			return Boolean.valueOf(false);
//		}
		
		return Boolean.valueOf(true);
	}

	/**
	 * Dismiss progress bar and show success message
	 */
	protected abstract void onPostExecute(Boolean response);

	@Override
	protected abstract void onPreExecute();

}
