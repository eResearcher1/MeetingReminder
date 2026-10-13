//package com.xtraword.apps.meetminder.worker;
//
//import android.content.Context;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.work.Constraints;
//import androidx.work.ExistingPeriodicWorkPolicy;
//import androidx.work.NetworkType;
//import androidx.work.PeriodicWorkRequest;
//import androidx.work.WorkManager;
//import androidx.work.Worker;
//import androidx.work.WorkerParameters;
//
//import com.xtraword.apps.meetminder.Meeting; // Your Meeting model
//import com.xtraword.apps.meetminder.databases.DatabaseHelper; // Your DB Helper
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.concurrent.TimeUnit;
//
//public class MeetingsSyncWorker extends Worker {
//
//    private static final String TAG = "MeetingsSyncWorker";
//    public static final String UNIQUE_WORK_NAME = "meetings_sync_worker";
//    private static final long EARLY_REMINDER_OFFSET_MINUTES = -10; // e.g., 10 minutes before
//
//    public MeetingsSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
//        super(context, workerParams);
//    }
//
//    @NonNull
//    @Override
//    public Result doWork() {
//        Log.d(TAG, "MeetingsSyncWorker started.");
//        Context context = getApplicationContext();
//        DatabaseHelper dbHelper = new DatabaseHelper(context);
//        List<Meeting> allMeetings = dbHelper.getAllMeetings(); // Make sure this gets ALL meetings
//
//        if (allMeetings == null || allMeetings.isEmpty()) {
//            Log.d(TAG, "No meetings found to sync.");
//            return Result.success();
//        }
//
//        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
//        Calendar now = Calendar.getInstance();
//
//        for (Meeting meeting : allMeetings) {
//            try {
//                Date meetingStartDate = dateTimeFormat.parse(meeting.getDate() + " " + meeting.getStartTime());
//                Calendar onTimeCalendar = Calendar.getInstance();
//                onTimeCalendar.setTime(meetingStartDate);
//
//                Calendar earlyCalendar = Calendar.getInstance();
//                earlyCalendar.setTime(meetingStartDate);
//                earlyCalendar.add(Calendar.MINUTE, (int) EARLY_REMINDER_OFFSET_MINUTES);
//
//                if (meeting.getIsActive() == 1) { // Meeting is Active
//                    // Check if meeting is in the future
//                    if (onTimeCalendar.after(now)) {
//                        // --- Schedule Early Reminder if it's in the future ---
//                        if (earlyCalendar.after(now)) {
//                            Log.d(TAG, "Sync: Scheduling early reminder for active meeting ID: " + meeting.getId());
//                            MeetingScheduler.scheduleSpecificReminder(
//                                    context, earlyCalendar, meeting.getId(),
//                                    "Upcoming: " + meeting.getTitle(), // Customize
//                                    "Starts in " + Math.abs(EARLY_REMINDER_OFFSET_MINUTES) + " minutes.", // Customize
//                                    MeetingScheduler.TYPE_EARLY_REMINDER, "_early");
//                        } else {
//                            // Early reminder time has passed, potentially cancel it if it was enqueued and shouldn't fire
//                            // MeetingScheduler.cancelSpecificReminder(context, meeting.getId(), "_early"); // Optional: more granular cancel
//                            Log.d(TAG, "Sync: Early reminder time passed for meeting ID: " + meeting.getId());
//                        }
//
//                        // --- Schedule On-Time Reminder if it's in the future ---
//                        // (onTimeCalendar.after(now) is already checked)
//                        Log.d(TAG, "Sync: Scheduling on-time reminder for active meeting ID: " + meeting.getId());
//                        MeetingScheduler.scheduleSpecificReminder(
//                                context, onTimeCalendar, meeting.getId(),
//                                meeting.getTitle() + " Starting Now", // Customize
//                                "Your meeting is beginning.", // Customize
//                                MeetingScheduler.TYPE_ON_TIME_REMINDER, "_on_time");
//                    } else {
//                        // Meeting start time is in the past, ensure all its reminders are cancelled
//                        Log.d(TAG, "Sync: Active meeting ID: " + meeting.getId() + " is in the past. Cancelling all reminders.");
//                        MeetingScheduler.cancelAllRemindersForMeeting(context, meeting.getId());
//                    }
//                } else { // Meeting is Inactive
//                    Log.d(TAG, "Sync: Meeting ID: " + meeting.getId() + " is inactive. Cancelling all reminders.");
//                    MeetingScheduler.cancelAllRemindersForMeeting(context, meeting.getId());
//                }
//
//            } catch (ParseException e) {
//                Log.e(TAG, "Error parsing date for meeting ID: " + meeting.getId() + " during sync.", e);
//            }
//        }
//
//        Log.d(TAG, "MeetingsSyncWorker finished.");
//        return Result.success();
//    }
//
//
//    /**
//     * Call this method (e.g., from your Application class or MainActivity) to set up the periodic worker.
//     */
//    public static void schedulePeriodicSync(Context context) {
//        // Define constraints (optional, but good for battery)
//        Constraints constraints = new Constraints.Builder()
//                // .setRequiredNetworkType(NetworkType.CONNECTED) // If sync needs network
//                // .setRequiresCharging(true)                  // Example: only run when charging
//                .build();
//
//        PeriodicWorkRequest syncWorkRequest =
//                new PeriodicWorkRequest.Builder(MeetingsSyncWorker.class,
//                        15, TimeUnit.MINUTES) // Example: Run every 6 hours
//                        // Use at least PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS (15 minutes)
//                        .setConstraints(constraints) // Optional constraints
//                        // .setInitialDelay(1, TimeUnit.MINUTES) // Optional initial delay
//                        .build();
//
//        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
//                UNIQUE_WORK_NAME,
//                ExistingPeriodicWorkPolicy.KEEP, // KEEP: If work already exists, do nothing. REPLACE: Replace existing.
//                syncWorkRequest);
//
//        Log.d(TAG, "Periodic MeetingsSyncWorker enqueued.");
//    }
//}
//
