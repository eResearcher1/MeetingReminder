
package com.xtraword.apps.meetminder.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xtraword.apps.meetminder.AlarmActivity;
import com.xtraword.apps.meetminder.Meeting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class AlarmHelper {

    private static final String TAG = "AlarmHelper";

    private static final int REQ_ON_TIME_OFFSET = 200000;
    private static final int REQ_SNOOZE_OFFSET = 300000;
    private static final int REQ_SHOW_ALARM_OFFSET = 400000;
    private static final int REQ_SHOW_SNOOZE_OFFSET = 500000;

    public static final int SNOOZE_MINUTES = 10;

    private AlarmHelper() {
        // Utility class.
    }

    public static Calendar parseMeetingDateTime(
            String dateStr,
            String timeStr) {

        if (dateStr == null || timeStr == null) {
            return null;
        }

        String date = dateStr.trim();
        String time = timeStr.trim();

        if (date.isEmpty() || time.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
            );

            formatter.setLenient(false);

            Date parsed = formatter.parse(date + " " + time);

            if (parsed == null) {
                return null;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar;

        } catch (ParseException e) {
            Log.e(
                    TAG,
                    "Failed to parse meeting date/time: "
                            + date + " " + time,
                    e
            );

            return null;
        }
    }

    public static void scheduleReminderForMeeting(
            Context context,
            Meeting meeting,
            Calendar meetingStartCal) {

        if (context == null
                || meeting == null
                || meetingStartCal == null) {
            return;
        }

        int meetingId = meeting.getId();

        if (meeting.getIsActive() != 1) {
            Log.d(
                    TAG,
                    "Meeting inactive. Cancelling alarms: " + meetingId
            );

            cancelAllAlarmsForMeeting(context, meetingId);
            return;
        }

        long triggerAtMillis = meetingStartCal.getTimeInMillis();

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(
                    TAG,
                    "Meeting start has passed. ID=" + meetingId
            );

            cancelAllAlarmsForMeeting(context, meetingId);
            return;
        }

        cancelAllAlarmsForMeeting(context, meetingId);

        boolean scheduled = setAlarmClockAlarm(
                context,
                meetingId,
                meeting.getTitle(),
                meeting.getDescription(),
                AlarmReceiverMM.REMINDER_TYPE_ON_TIME,
                triggerAtMillis,
                getOnTimeRequestCode(meetingId),
                getShowAlarmRequestCode(meetingId),
                getMeetingStartMillis(meeting)
        );

        if (scheduled) {
            Log.d(
                    TAG,
                    "On-time reminder scheduled. ID="
                            + meetingId
                            + ", trigger="
                            + triggerAtMillis
            );
        } else {
            Log.e(
                    TAG,
                    "Failed to schedule on-time reminder. ID="
                            + meetingId
            );
        }
    }

    public static void scheduleSnoozeAlarm(
            Context context,
            Meeting meeting) {

        if (context == null || meeting == null) {
            return;
        }

        int meetingId = meeting.getId();

        long triggerAtMillis = System.currentTimeMillis()
                + SNOOZE_MINUTES * 60_000L;

        cancelSnoozeAlarm(context, meetingId);

        boolean scheduled = setAlarmClockAlarm(
                context,
                meetingId,
                meeting.getTitle(),
                meeting.getDescription(),
                AlarmReceiverMM.REMINDER_TYPE_SNOOZE,
                triggerAtMillis,
                getSnoozeRequestCode(meetingId),
                getShowSnoozeRequestCode(meetingId),
                getMeetingStartMillis(meeting)
        );

        if (scheduled) {
            Log.d(
                    TAG,
                    "Snooze scheduled. ID="
                            + meetingId
                            + ", trigger="
                            + triggerAtMillis
            );
        } else {
            Log.e(
                    TAG,
                    "Failed to schedule snooze. ID=" + meetingId
            );
        }
    }

    public static void cancelAllAlarmsForMeeting(
            Context context,
            int meetingId) {

        if (context == null) {
            return;
        }

        AlarmManager alarmManager = getAlarmManager(context);

        if (alarmManager != null) {
            cancelOne(
                    alarmManager,
                    context,
                    meetingId,
                    AlarmReceiverMM.REMINDER_TYPE_ON_TIME,
                    getOnTimeRequestCode(meetingId)
            );

            cancelOne(
                    alarmManager,
                    context,
                    meetingId,
                    AlarmReceiverMM.REMINDER_TYPE_SNOOZE,
                    getSnoozeRequestCode(meetingId)
            );
        }

        cancelShowIntent(
                context,
                getShowAlarmRequestCode(meetingId)
        );

        cancelShowIntent(
                context,
                getShowSnoozeRequestCode(meetingId)
        );
    }

    public static void cancelSnoozeAlarm(
            Context context,
            int meetingId) {

        if (context == null) {
            return;
        }

        AlarmManager alarmManager = getAlarmManager(context);

        if (alarmManager != null) {
            cancelOne(
                    alarmManager,
                    context,
                    meetingId,
                    AlarmReceiverMM.REMINDER_TYPE_SNOOZE,
                    getSnoozeRequestCode(meetingId)
            );
        }

        cancelShowIntent(
                context,
                getShowSnoozeRequestCode(meetingId)
        );
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE
        );
    }

    private static int getOnTimeRequestCode(int meetingId) {
        return REQ_ON_TIME_OFFSET + meetingId;
    }

    private static int getSnoozeRequestCode(int meetingId) {
        return REQ_SNOOZE_OFFSET + meetingId;
    }

    private static int getShowAlarmRequestCode(int meetingId) {
        return REQ_SHOW_ALARM_OFFSET + meetingId;
    }

    private static int getShowSnoozeRequestCode(int meetingId) {
        return REQ_SHOW_SNOOZE_OFFSET + meetingId;
    }

    private static long getMeetingStartMillis(Meeting meeting) {

        Calendar calendar = parseMeetingDateTime(
                meeting.getDate(),
                meeting.getStartTime()
        );

        return calendar != null
                ? calendar.getTimeInMillis()
                : 0L;
    }

    private static void cancelOne(
            AlarmManager alarmManager,
            Context context,
            int meetingId,
            String reminderType,
            int requestCode) {

        PendingIntent pendingIntent = buildAlarmPendingIntent(
                context,
                meetingId,
                reminderType,
                requestCode,
                null,
                null,
                0L,
                true
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static void cancelShowIntent(
            Context context,
            int requestCode) {

        PendingIntent pendingIntent = buildShowAlarmPendingIntent(
                context,
                requestCode,
                -1,
                null,
                null,
                -1,
                true
        );

        if (pendingIntent != null) {
            pendingIntent.cancel();
        }
    }

    private static boolean setAlarmClockAlarm(
            Context context,
            int meetingId,
            String title,
            String description,
            String reminderType,
            long triggerAtMillis,
            int requestCode,
            int showIntentRequestCode,
            long meetingStartTimeMillis) {

        AlarmManager alarmManager = getAlarmManager(context);

        if (alarmManager == null) {
            Log.e(
                    TAG,
                    "AlarmManager unavailable. Meeting ID=" + meetingId
            );
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {

            Log.w(
                    TAG,
                    "Exact alarm permission unavailable. Meeting ID="
                            + meetingId
            );

            return false;
        }

        PendingIntent operationIntent = buildAlarmPendingIntent(
                context,
                meetingId,
                reminderType,
                requestCode,
                title,
                description,
                meetingStartTimeMillis,
                false
        );

        PendingIntent showIntent = buildShowAlarmPendingIntent(
                context,
                showIntentRequestCode,
                meetingId,
                title,
                description,
                getNotificationIdForType(meetingId, reminderType),
                false
        );

        if (operationIntent == null || showIntent == null) {
            Log.e(
                    TAG,
                    "Failed to create PendingIntent. ID=" + meetingId
            );
            return false;
        }

        try {
            AlarmManager.AlarmClockInfo alarmClockInfo =
                    new AlarmManager.AlarmClockInfo(
                            triggerAtMillis,
                            showIntent
                    );

            alarmManager.setAlarmClock(
                    alarmClockInfo,
                    operationIntent
            );

            Log.d(
                    TAG,
                    "Alarm scheduled: meetingId="
                            + meetingId
                            + ", type="
                            + reminderType
                            + ", triggerAt="
                            + triggerAtMillis
            );

            return true;

        } catch (SecurityException e) {
            Log.e(
                    TAG,
                    "Permission denied while scheduling alarm. ID="
                            + meetingId,
                    e
            );

        } catch (Exception e) {
            Log.e(
                    TAG,
                    "Unexpected alarm scheduling error. ID="
                            + meetingId,
                    e
            );
        }

        return false;
    }

    private static int getNotificationIdForType(
            int meetingId,
            String reminderType) {

        if (AlarmReceiverMM.REMINDER_TYPE_SNOOZE.equals(reminderType)) {
            return REQ_SNOOZE_OFFSET + meetingId;
        }

        return REQ_ON_TIME_OFFSET + meetingId;
    }

    private static PendingIntent buildAlarmPendingIntent(
            Context context,
            int meetingId,
            String reminderType,
            int requestCode,
            String title,
            String description,
            long meetingStartTimeMillis,
            boolean existingOnly) {

        Intent intent = new Intent(
                context,
                AlarmReceiverMM.class
        );

        intent.setData(
                Uri.parse(
                        "meetminder://alarm/"
                                + meetingId
                                + "/"
                                + reminderType
                )
        );

        intent.putExtra("meeting_id", meetingId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("reminder_type", reminderType);

        intent.putExtra(
                AlarmReceiverMM.EXTRA_MEETING_START_MILLIS,
                meetingStartTimeMillis
        );

        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE;

        if (existingOnly) {
            flags = PendingIntent.FLAG_NO_CREATE
                    | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
        );
    }

    private static PendingIntent buildShowAlarmPendingIntent(
            Context context,
            int requestCode,
            int meetingId,
            String title,
            String description,
            int notificationId,
            boolean existingOnly) {

        Intent intent = new Intent(
                context,
                AlarmActivity.class
        );

        intent.setData(
                Uri.parse(
                        "meetminder://showalarm/" + requestCode
                )
        );

        intent.putExtra("meeting_id", meetingId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("notification_id", notificationId);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE;

        if (existingOnly) {
            flags = PendingIntent.FLAG_NO_CREATE
                    | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flags
        );
    }
}
