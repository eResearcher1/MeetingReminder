package com.xtraword.apps.meetminder.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import java.util.Calendar;
import java.util.List;

public class BootReceiverMM extends BroadcastReceiver {
    private static final String TAG = "BootReceiverMM";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "Received: " + action);
        if ("android.intent.action.BOOT_COMPLETED".equals(action) || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) || "android.intent.action.MY_PACKAGE_REPLACED".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
            try {
                DatabaseHelper db = new DatabaseHelper(context);
                List<Meeting> activeMeetings = db.getAllActiveMeetings();
                Log.d(TAG, "Rescheduling alarms for active meetings: " + activeMeetings.size());
                for (Meeting m : activeMeetings) {
                    Calendar startCal = AlarmHelper.parseMeetingDateTime(m.getDate(), m.getStartTime());
                    if (startCal != null) {
                        AlarmHelper.scheduleReminderForMeeting(context, m, startCal);
                        Log.d(TAG, "Rescheduled on-time alarm for meeting ID=" + m.getId());
                    } else {
                        Log.w(TAG, "Could not parse start time for meeting ID=" + m.getId());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to reschedule alarms after " + action, e);
            }
        }
    }
}
