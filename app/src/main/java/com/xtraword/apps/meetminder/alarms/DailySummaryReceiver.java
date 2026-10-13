package com.xtraword.apps.meetminder.alarms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import java.util.Calendar;
import java.util.List;

public class DailySummaryReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "daily_summary_channel";
    private static final int NOTIF_ID = 6600;

    @Override
    public void onReceive(Context context, Intent intent) {
        String text;
        ensureChannel(context);
        Calendar start = Calendar.getInstance();
        start.set(11, 0);
        start.set(12, 0);
        start.set(13, 0);
        start.set(14, 0);
        Calendar end = (Calendar) start.clone();
        end.add(6, 1);
        DatabaseHelper db = new DatabaseHelper(context);
        List<String> lines = db.getTodaysPendingMeetingLines();
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        if (lines == null || lines.isEmpty()) {
            text = "You have no pending meetings for today.";
            style.addLine("You have no pending meetings for today.");
        } else {
            text = "You have " + lines.size() + " pending meeting(s) today.";
            int max = Math.min(lines.size(), 6);
            for (int i = 0; i < max; i++) {
                style.addLine(lines.get(i));
            }
            int i2 = lines.size();
            if (i2 > max) {
                style.addLine("…and more");
            }
        }
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle("Today's meetings").setContentText(text).setStyle(style).setAutoCancel(true).setPriority(0);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        if (nm != null) {
            nm.notify(NOTIF_ID, nb.build());
        }
        DailySummaryScheduler.scheduleNext(context);
    }

    private void ensureChannel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Daily Meeting Summary", 3);
            Uri sound = RingtoneManager.getDefaultUri(2);
            ch.setSound(sound, null);
            nm.createNotificationChannel(ch);
        }
    }
}
