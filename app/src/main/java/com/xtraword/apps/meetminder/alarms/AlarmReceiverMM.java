
package com.xtraword.apps.meetminder.alarms;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.xtraword.apps.meetminder.AlarmActivity;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.foreground.ReminderService;

public class AlarmReceiverMM extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiverMM";

    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_MEETING_ID = "meeting_id";
    public static final String EXTRA_MEETING_START_MILLIS =
            "meeting_start_time_millis";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    public static final String EXTRA_TITLE = "title";

    public static final String REMINDER_CHANNEL_ID =
            "meeting_reminder_channel";

    public static final String REMINDER_TYPE_ON_TIME = "on_time";
    public static final String REMINDER_TYPE_SNOOZE = "snooze";

    private static final String PREFS_APP_SETTINGS = "AppPreferences";
    private static final String KEY_NOTIFICATION_TONE = "notification_tone";

    private static final int ON_TIME_NOTIFICATION_OFFSET = 200000;
    private static final int SNOOZE_NOTIFICATION_OFFSET = 300000;

    private static final int SNOOZE_ACTION_OFFSET = 400000;
    private static final int DISMISS_ACTION_OFFSET = 500000;
    private static final int DELETE_ACTION_OFFSET = 600000;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            Log.e(TAG, "Received null alarm intent");
            return;
        }

        PowerManager.WakeLock wakeLock = null;

        try {
            PowerManager powerManager =
                    (PowerManager) context.getSystemService(
                            Context.POWER_SERVICE
                    );

            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "meetminder:alarm_wakelock"
                );

                wakeLock.acquire(10_000L);
            }

            int meetingId = intent.getIntExtra(
                    EXTRA_MEETING_ID,
                    -1
            );

            String title = intent.getStringExtra(EXTRA_TITLE);
            String description = intent.getStringExtra(EXTRA_DESCRIPTION);

            String reminderType = intent.getStringExtra(
                    EXTRA_REMINDER_TYPE
            );

            long meetingStartMillis = intent.getLongExtra(
                    EXTRA_MEETING_START_MILLIS,
                    0L
            );

            if (meetingId < 0 || title == null) {
                Log.e(
                        TAG,
                        "Invalid alarm data. Meeting ID=" + meetingId
                );
                return;
            }

            if (reminderType == null) {
                reminderType = REMINDER_TYPE_ON_TIME;
            }

            int notificationId =
                    REMINDER_TYPE_SNOOZE.equals(reminderType)
                            ? SNOOZE_NOTIFICATION_OFFSET + meetingId
                            : ON_TIME_NOTIFICATION_OFFSET + meetingId;

            Log.d(
                    TAG,
                    "Alarm triggered. Meeting ID="
                            + meetingId
                            + ", type="
                            + reminderType
            );

            startReminderService(
                    context,
                    meetingId,
                    title,
                    description,
                    reminderType,
                    meetingStartMillis,
                    notificationId
            );

            showReminderNotification(
                    context,
                    meetingId,
                    title,
                    description,
                    reminderType,
                    meetingStartMillis,
                    notificationId
            );

        } catch (Exception e) {
            Log.e(TAG, "Error handling meeting alarm", e);

        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to release WakeLock", e);
                }
            }
        }
    }

    private void startReminderService(
            Context context,
            int meetingId,
            String title,
            String description,
            String reminderType,
            long meetingStartMillis,
            int notificationId) {

        Intent serviceIntent = new Intent(
                context,
                ReminderService.class
        );

        serviceIntent.setAction(
                ReminderService.ACTION_START_RINGING
        );

        serviceIntent.putExtra(EXTRA_MEETING_ID, meetingId);
        serviceIntent.putExtra(EXTRA_TITLE, title);
        serviceIntent.putExtra(EXTRA_DESCRIPTION, description);
        serviceIntent.putExtra(EXTRA_REMINDER_TYPE, reminderType);

        serviceIntent.putExtra(
                EXTRA_MEETING_START_MILLIS,
                meetingStartMillis
        );

        serviceIntent.putExtra(
                EXTRA_NOTIFICATION_ID,
                notificationId
        );

        try {
            ContextCompat.startForegroundService(
                    context,
                    serviceIntent
            );

        } catch (Exception e) {
            Log.e(
                    TAG,
                    "Failed to start ReminderService. Meeting ID="
                            + meetingId,
                    e
            );
        }
    }

    private void showReminderNotification(
            Context context,
            int meetingId,
            String title,
            String description,
            String reminderType,
            long meetingStartMillis,
            int notificationId) {

        Uri ringtoneUri = getSavedRingtoneUri(context);

        ensureChannel(
                context,
                REMINDER_CHANNEL_ID,
                "Meeting Reminders",
                ringtoneUri
        );

        Intent openIntent = new Intent(
                context,
                AlarmActivity.class
        );

        openIntent.putExtra(EXTRA_MEETING_ID, meetingId);
        openIntent.putExtra(EXTRA_TITLE, title);
        openIntent.putExtra(EXTRA_DESCRIPTION, description);
        openIntent.putExtra(EXTRA_REMINDER_TYPE, reminderType);
        openIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        openIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(
                context,
                ReminderService.class
        );

        snoozeIntent.setAction(
                ReminderService.ACTION_SNOOZE_10
        );

        snoozeIntent.putExtra(EXTRA_MEETING_ID, meetingId);
        snoozeIntent.putExtra(EXTRA_TITLE, title);
        snoozeIntent.putExtra(EXTRA_DESCRIPTION, description);
        snoozeIntent.putExtra(EXTRA_REMINDER_TYPE, reminderType);
        snoozeIntent.putExtra(
                EXTRA_MEETING_START_MILLIS,
                meetingStartMillis
        );
        snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent snoozePendingIntent = PendingIntent.getService(
                context,
                SNOOZE_ACTION_OFFSET + meetingId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(
                context,
                ReminderService.class
        );

        dismissIntent.setAction(
                ReminderService.ACTION_DISMISS
        );

        dismissIntent.putExtra(EXTRA_MEETING_ID, meetingId);
        dismissIntent.putExtra(EXTRA_REMINDER_TYPE, reminderType);
        dismissIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent dismissPendingIntent = PendingIntent.getService(
                context,
                DISMISS_ACTION_OFFSET + meetingId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent deletePendingIntent = PendingIntent.getService(
                context,
                DELETE_ACTION_OFFSET + meetingId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        String displayTitle;
        String displayText;

        if (REMINDER_TYPE_SNOOZE.equals(reminderType)) {
            displayTitle = title + " - Snoozed Reminder";

            displayText = description == null
                    || description.trim().isEmpty()
                    ? "This meeting reminder is ringing again."
                    : description;
        } else {
            displayTitle = title + " - Starting Now";

            displayText = description == null
                    || description.trim().isEmpty()
                    ? "Your meeting is beginning."
                    : description;
        }

        Notification notification = new NotificationCompat.Builder(
                context,
                REMINDER_CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(displayTitle)
                .setContentText(displayText)
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(displayText)
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, true)
                .setDeleteIntent(deletePendingIntent)
                .addAction(
                        R.drawable.ic_notification,
                        "Snooze 10m",
                        snoozePendingIntent
                )
                .addAction(
                        R.drawable.ic_notification,
                        "Dismiss",
                        dismissPendingIntent
                )
                .setLights(0xFF0000FF, 500, 500)
                .setVibrate(new long[]{0, 400, 200, 400})
                .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            Log.w(
                    TAG,
                    "Notification permission not granted"
            );
            return;
        }

        try {
            notificationManager.notify(
                    notificationId,
                    notification
            );

        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission denied", e);
        }
    }

    private void ensureChannel(
            Context context,
            String channelId,
            String channelName,
            Uri soundUri) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (manager == null) {
            return;
        }

        NotificationChannel existing =
                manager.getNotificationChannel(channelId);

        if (existing != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.enableLights(true);
        channel.setLightColor(0xFF0000FF);

        channel.enableVibration(true);
        channel.setVibrationPattern(
                new long[]{0, 400, 200, 400}
        );

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        channel.setSound(soundUri, attributes);

        manager.createNotificationChannel(channel);
    }

    private Uri getSavedRingtoneUri(Context context) {

        Uri defaultUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
        );

        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_APP_SETTINGS,
                Context.MODE_PRIVATE
        );

        String savedUri = preferences.getString(
                KEY_NOTIFICATION_TONE,
                defaultUri != null ? defaultUri.toString() : ""
        );

        if (savedUri == null || savedUri.trim().isEmpty()) {
            return defaultUri;
        }

        try {
            return Uri.parse(savedUri);
        } catch (Exception e) {
            Log.e(TAG, "Invalid saved ringtone URI", e);
            return defaultUri;
        }
    }
}
