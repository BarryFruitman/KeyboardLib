package com.comet.keyboard.announcements;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;

import com.comet.keyboard.R;
import com.comet.keyboard.settings.Settings;

public class AnnouncementsManager {

	private static final int mNotificationId = 0;


	public static void postAnnouncements(Context context) {
		String message = getAnnouncements(context);
		if(message.length() > 0) {
			// Post a notification

			// Create explicit and pending intents for this activity
			Intent intent = new Intent(context, Announcements.class);
			intent.putExtra("message", message);

			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

			// Build the notification
			Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("New features in " + context.getString(R.string.ime_name))
				.setContentText("Press here to see what's new in " + context.getString(R.string.ime_name))
				.setContentIntent(pendingIntent)
				.build();

			// Post it
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
				.notify(mNotificationId, notification);
		}
	}



	private static String getAnnouncements(Context context) {
		String[] announcementIds = context.getResources().getStringArray(R.array.announcement_ids);
		String[] announcementMsgs = context.getResources().getStringArray(R.array.announcement_messages);
		SharedPreferences sharedPrefs = context.getSharedPreferences(Settings.SETTINGS_FILE, Context.MODE_PRIVATE);
		int iAnnouncementNum = 1;
		StringBuilder message = new StringBuilder();
		for(int iAnnouncement = 0; iAnnouncement < announcementIds.length; iAnnouncement++) {
			String announcement = announcementIds[iAnnouncement];
			if(!sharedPrefs.getString(announcement, "").equals(""))
				continue;

			message.append(iAnnouncementNum++);
			message.append(". ");
			message.append(announcementMsgs[iAnnouncement]);
			message.append("\n");

			sharedPrefs.edit().putString(announcementIds[iAnnouncement], "true").commit();
		}

		return message.toString();
	}
}
