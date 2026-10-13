package com.xtraword.apps.meetminder;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;
import com.xtraword.apps.meetminder.alarms.AlarmReceiverMM;
import com.xtraword.apps.meetminder.alarms.DailySummaryScheduler;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        createReminderChannels();
        DailySummaryScheduler.scheduleNext(this);
    }

    private void createReminderChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
        if (nm == null) {
            Log.e(TAG, "NotificationManager is null. Cannot create channels.");
        } else {
            createChannelIfMissing(nm, AlarmReceiverMM.REMINDER_CHANNEL_ID, "Meeting Reminders", "Notifications for meeting reminders and snoozed reminders.");
        }
    }

    private void createChannelIfMissing(NotificationManager nm, String channelId, String name, String description) {
        NotificationChannel existing = nm.getNotificationChannel(channelId);
        if (existing != null) {
            Log.d(TAG, "Channel already exists: " + channelId);
            return;
        }
        NotificationChannel ch = new NotificationChannel(channelId, name, 4);
        ch.setDescription(description);
        ch.enableLights(true);
        ch.setLightColor(-16776961);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 400, 200, 400});
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);
        Log.d(TAG, "Created channel: " + channelId);
    }
}
