package com.xtraword.apps.meetminder.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;
import java.util.Calendar;

public class BootReceiverMM extends BroadcastReceiver {
    private static final String TAG = "BootReceiverMM";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) return;
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try { restoreSchedules(app); }
            catch (Exception e) { Log.e(TAG, "Recovery failed", e); }
            finally { pending.finish(); }
        }, "MeetMinderBootRecovery").start();
    }

    public static void restoreSchedules(Context context) {
        try (DatabaseHelper db = new DatabaseHelper(context)) {
            long now = System.currentTimeMillis();
            for (Meeting meeting : db.getAllMeetings()) {
                int id = meeting.getId();
                AlarmHelper.cancelAllAlarmsForMeeting(context, id);
                MeetingScheduler.cancelImmediateRollover(context, id);
                if (meeting.getIsActive() != 1) continue;
                Calendar start = AlarmHelper.parseMeetingDateTime(meeting.getDate(), meeting.getStartTime());
                Calendar end = AlarmHelper.parseMeetingDateTime(meeting.getDate(), meeting.getEndTime());
                if (start == null || end == null) continue;
                if (!end.after(start)) end.add(Calendar.DAY_OF_MONTH, 1);
                if (start.getTimeInMillis() > now)
                    AlarmHelper.scheduleReminderForMeeting(context, meeting, start);
                if (meeting.getRecurrenceType() != 0)
                    MeetingScheduler.scheduleImmediateRollover(context, end, id);
            }
        }
    }
}
