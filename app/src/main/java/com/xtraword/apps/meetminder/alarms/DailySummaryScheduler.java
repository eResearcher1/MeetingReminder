package com.xtraword.apps.meetminder.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class DailySummaryScheduler {
    public static void scheduleNext(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        if (am == null) {
            return;
        }
        Calendar c = Calendar.getInstance();
        c.set(11, 6);
        c.set(12, 0);
        c.set(13, 0);
        c.set(14, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(6, 1);
        }
        Intent i = new Intent(context, (Class<?>) DailySummaryReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 6600, i, 201326592);
        long triggerAt = c.getTimeInMillis();
        try {
            am.setExactAndAllowWhileIdle(0, triggerAt, pi);
        } catch (SecurityException e) {
        }
    }
}
