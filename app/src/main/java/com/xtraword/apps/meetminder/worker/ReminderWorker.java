//// ReminderWorker.java
//package com.xtraword.apps.meetminder.worker; // Or your preferred package
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.media.RingtoneManager;
//import android.net.Uri;
//import android.os.Build;
//import android.provider.Settings;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//import androidx.work.Data;
//import androidx.work.ForegroundInfo;
//import androidx.work.ListenableWorker;
//import androidx.work.Worker;
//import androidx.work.WorkerParameters;
//
//import com.xtraword.apps.meetminder.MainActivity; // Assuming MainActivity is your launch activity
//import com.xtraword.apps.meetminder.R; // For notification icon
//
//public class ReminderWorker extends Worker {
//
//    private static final String TAG = "ReminderWorker";
//    public static final String KEY_MEETING_ID = "meeting_id";
//    public static final String KEY_MEETING_TITLE = "meeting_title";
//    public static final String KEY_MEETING_DESCRIPTION = "meeting_desc"; // Using this as the main text
//    // KEY_REMINDER_TYPE is already accessed via getInputData().getString(MeetingScheduler.KEY_REMINDER_TYPE)
//
//    private static final String CHANNEL_ID = "MEETING_REMINDERS_CHANNEL";
//    private static final String CHANNEL_NAME = "Meeting Reminders";
//    private static final String CHANNEL_DESCRIPTION = "Notifications for upcoming meeting reminders";
//
//    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
//        super(context, workerParams);
//    }
//
//    @NonNull
//    @Override
//    public ListenableWorker.Result doWork() {
//        Data inputData = getInputData();
//        int meetingId = inputData.getInt(KEY_MEETING_ID, -1); // Use a default that indicates error
//        String title = inputData.getString(KEY_MEETING_TITLE);
//        String description = inputData.getString(KEY_MEETING_DESCRIPTION); // This will be the main text of notification
//        String reminderType = inputData.getString(MeetingScheduler.KEY_REMINDER_TYPE); // Get reminder type
//
//        Log.d(TAG, "doWork started for meetingId: " + meetingId +
//                ", Title: " + title +
//                ", Type: " + reminderType);
//
//        if (meetingId == -1 || title == null) {
//            Log.e(TAG, "Failed to retrieve meeting ID or title from inputData. meetingId: " + meetingId + ", title: " + title);
//            return ListenableWorker.Result.failure();
//        }
//
//        // --- Customization based on reminderType (already in your code) ---
//        String actualNotificationTitle = title; // Default to the meeting title
//        String actualNotificationText = description != null ? description : "Your meeting is approaching."; // Default text if description is null
//
//        if (MeetingScheduler.TYPE_EARLY_REMINDER.equals(reminderType)) {
//            // Modify title/text for early reminder if needed
//            actualNotificationTitle = "Upcoming meeting: " + title;
//            // The title and description passed in should already be customized by MeetingScheduler or SyncWorker
//            Log.d(TAG, "Preparing early reminder content.");
//        } else if (MeetingScheduler.TYPE_ON_TIME_REMINDER.equals(reminderType)) {
//            // Modify title/text for on-time reminder if needed
//            actualNotificationTitle = title + " - Starting Now!";
//            Log.d(TAG, "Preparing on-time reminder content.");
//        }
//
//        try {
//            // Create notification channel (idempotent - safe to call every time)
//            createNotificationChannel(getApplicationContext());
//
//            // Send the notification
//            sendNotification(getApplicationContext(), meetingId, actualNotificationTitle, actualNotificationText);
//
//            Log.d(TAG, "Notification sent successfully for meetingId: " + meetingId);
//            return ListenableWorker.Result.success();
//        } catch (Exception e) {
//            Log.e(TAG, "Error during doWork for meetingId: " + meetingId, e);
//            return ListenableWorker.Result.failure();
//        }
//    }
//
//    private void createNotificationChannel(Context context) {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
//            channel.setDescription(CHANNEL_DESCRIPTION);
//            // Optional: Configure channel properties like lights, vibration, sound
//            channel.enableLights(true);
//            channel.setLightColor(Color.RED);
//            channel.enableVibration(true);
//            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
//
//            // Set default sound for the channel (users can still override this in system settings)
//            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            // You can also use a custom sound:
//            // Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.your_custom_sound);
//            channel.setSound(soundUri, null); // null for default audio attributes
//
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(channel);
//                Log.d(TAG, "Notification channel created or already exists.");
//            } else {
//                Log.e(TAG, "NotificationManager is null, cannot create channel.");
//            }
//        }
//    }
//
//    private void sendNotification(Context context, int meetingId, String title, String text) {
//        // Intent to launch when the notification is tapped
//        // This should typically open the specific meeting details, or the main app screen
//        Intent intent = new Intent(context, MainActivity.class); // Change to your relevant activity
//        intent.putExtra("meeting_id_from_notification", meetingId); // Optional: pass meeting ID to the activity
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context,
//                meetingId, // Use meetingId as request code to make each PendingIntent unique for this notification
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        // Get user-selected ringtone or default
//        // For simplicity, using default here. Integrate SharedPreferences if you allow custom ringtones per reminder.
//        Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        // If you have a user-selected sound stored in SharedPreferences:
//        // String storedSoundUriString = PreferenceManager.getDefaultSharedPreferences(context).getString("selected_ringtone_uri", null);
//        // if (storedSoundUriString != null) {
//        //     notificationSoundUri = Uri.parse(storedSoundUriString);
//        // }
//
//
//        // Build the notification
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher) // TODO: Replace with your actual notification icon (must be B&W for status bar)
//                .setContentTitle(title)
//                .setContentText(text)
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(text)) // Allows for longer text
//                .setPriority(NotificationCompat.PRIORITY_HIGH) // For heads-up notification
//                .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
//                .setAutoCancel(true) // Automatically removes the notification when the user taps it
//                .setSound(notificationSoundUri) // Set the sound for this specific notification
//                .setVibrate(new long[]{100, 200, 300, 400, 500}) // Standard vibration pattern, channel pattern also applies
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Show on lock screen
//
//
//        // For Foreground Service (if this worker needs to be expedited and run longer)
//        // If you mark a worker as expedited using .setExpedited(), WorkManager might run it as a foreground service
//        // in which case you need to provide ForegroundInfo.
////         ForegroundInfo foregroundInfo = new ForegroundInfo(meetingId, notificationBuilder.build());
////         setForegroundAsync(foregroundInfo); // Call this if it's an expedited long-running worker
//
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        // notificationId is a unique int for each notification that you must define
//        // Using meetingId ensures that if another reminder for the SAME meeting comes (e.g. on-time after early),
//        // it updates the existing notification for that meetingId.
//        // If you want separate notifications for early and on-time for the same meeting,
//        // you'll need a more unique notificationId (e.g., based on meetingId + reminderType).
//        // For now, using meetingId means an on-time reminder will replace an early one for the same meeting.
//        try {
//            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
//                notificationManager.notify(meetingId, notificationBuilder.build());
//                Log.d(TAG, "Notification successfully posted with ID: " + meetingId);
//            } else {
//                Log.w(TAG, "Notifications are disabled for this app. Cannot show notification for meetingId: " + meetingId);
//                // Optionally, you could direct the user to settings here, but it might be disruptive from a worker.
//            }
//        } catch (SecurityException se) {
//            // This can happen on some Android versions if POST_NOTIFICATIONS permission is not granted (Android 13+)
//            // Although WorkManager typically handles this by not executing if permissions are missing for foreground services.
//            Log.e(TAG, "SecurityException while trying to post notification. Check POST_NOTIFICATIONS permission. " + se.getMessage());
//            // Consider logging this or handling it in a way that informs the user they need to grant permissions.
//            // For Android 13+, you should request Manifest.permission.POST_NOTIFICATIONS at runtime in your app.
//        }
//    }
//}
