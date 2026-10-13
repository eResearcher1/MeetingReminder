package com.xtraword.apps.meetminder.worker;

import android.content.Context;
import android.util.Log;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MeetingScheduler {
    private static final String TAG = "MeetingScheduler";

    private MeetingScheduler() {
    }

    public static void scheduleImmediateRollover(Context context, Calendar meetingEndTime, int meetingId) {
        if (context == null || meetingEndTime == null) {
            Log.w(TAG, "scheduleImmediateRollover: context or meetingEndTime is null.");
            return;
        }
        Calendar now = Calendar.getInstance();
        long delayMillis = meetingEndTime.getTimeInMillis() - now.getTimeInMillis();
        Data workData = new Data.Builder().putInt(ImmediateMeetingRolloverWorker.KEY_MEETING_ID_TO_ROLLOVER, meetingId).build();
        String uniqueWorkName = getRolloverUniqueWorkName(meetingId);
        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(ImmediateMeetingRolloverWorker.class).setInputData(workData).addTag(getRolloverTag(meetingId));
        if (delayMillis > 0) {
            builder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Rollover scheduled with delay for meetingId=" + meetingId + ", delay=" + delayMillis + "ms");
        } else {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
            Log.d(TAG, "Rollover scheduled ASAP for meetingId=" + meetingId);
        }
        OneTimeWorkRequest request = builder.build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request);
        Log.d(TAG, "Rollover work enqueued. meetingId=" + meetingId + ", uniqueWorkName=" + uniqueWorkName);
    }

    public static void cancelImmediateRollover(Context context, int meetingId) {
        if (context == null) {
            return;
        }
        String uniqueWorkName = getRolloverUniqueWorkName(meetingId);
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(uniqueWorkName);
        Log.d(TAG, "Cancelled rollover work. meetingId=" + meetingId + ", uniqueWorkName=" + uniqueWorkName);
    }

    private static String getRolloverUniqueWorkName(int meetingId) {
        return "rollover_trigger_" + meetingId;
    }

    private static String getRolloverTag(int meetingId) {
        return "MEETING_ROLLOVER_TAG_" + meetingId;
    }
}
