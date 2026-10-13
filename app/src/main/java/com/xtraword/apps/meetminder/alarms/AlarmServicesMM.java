package com.xtraword.apps.meetminder.alarms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.xtraword.apps.meetminder.MainActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AlarmServicesMM extends Service {
    private static final String CHANNEL_ID_FUTURE_MEETINGS_NOTIF = "FutureMeetingChannel";
    private static final String CHANNEL_ID_SERVICE_OPS = "AlarmServiceChannel";
    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 1;
    private static final int SUMMARY_NOTIFICATION_ID = 2;
    private static final String TAG = "AlarmServicesMM";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DB_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FULL_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created.");
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started.");
        Notification foregroundNotification = new NotificationCompat.Builder(this, CHANNEL_ID_SERVICE_OPS).setContentTitle(getString(R.string.app_name) + " Service").setContentText("Updating meeting reminders...").setSmallIcon(R.drawable.logo2).setPriority(-1).build();
        startForeground(1, foregroundNotification);
        new Thread(new Runnable() {
            @Override
            public final void run() {
                AlarmServicesMM.this.m304x6fbe6309();
            }
        }).start();
        return 2;
    }

    void m304x6fbe6309() {
        AlarmServicesMM alarmServicesMM = this;
        Log.d(TAG, "Background thread started for re-scheduling alarms.");
        DatabaseHelper dbHelper = new DatabaseHelper(alarmServicesMM.getApplicationContext());
        List<Meeting> allMeetingsFromDb = dbHelper.getAllMeetings();
        List<Meeting> meetingsForSummaryNotification = new ArrayList<>();
        Calendar currentTime = Calendar.getInstance();
        Iterator<Meeting> it = allMeetingsFromDb.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Meeting meeting = it.next();
            if (meeting.getIsActive() != 1) {
                Log.d(TAG, "Meeting ID " + meeting.getId() + " ('" + meeting.getTitle() + "') is inactive. Skipping.");
                AlarmHelper.cancelAllAlarmsForMeeting(alarmServicesMM.getApplicationContext(), meeting.getId());
            } else {
                Log.d(TAG, "Processing active meeting ID: " + meeting.getId() + " ('" + meeting.getTitle() + "'), Recurrence: " + meeting.getRecurrenceType());
                Calendar meetingStoredStartCal = alarmServicesMM.parseMeetingDateTime(meeting.getDate(), meeting.getStartTime());
                if (meetingStoredStartCal == null) {
                    Log.e(TAG, "Could not parse date/time for meeting ID " + meeting.getId() + ". Skipping.");
                } else {
                    boolean isRecurring = meeting.getRecurrenceType() != 0;
                    if (meetingStoredStartCal.before(currentTime)) {
                        if (isRecurring) {
                            Log.d(TAG, "Meeting ID " + meeting.getId() + " is recurring and its stored start time is in the past. Calculating next future occurrence.");
                            Calendar nextFutureOccurrenceStart = alarmServicesMM.calculateNextFutureOccurrence(meetingStoredStartCal, meeting.getRecurrenceType());
                            if (nextFutureOccurrenceStart != null) {
                                Log.d(TAG, "Next future occurrence for meeting ID " + meeting.getId() + " is: " + FULL_DATETIME_FORMAT.format(nextFutureOccurrenceStart.getTime()));
                                String newDateStr = DB_DATE_FORMAT.format(nextFutureOccurrenceStart.getTime());
                                String originalTimeStr = DB_TIME_FORMAT.format(meetingStoredStartCal.getTime());
                                meeting.setDate(newDateStr);
                                meeting.setStartTime(originalTimeStr);
                                int rowsAffected = dbHelper.updateMeeting(meeting.getId(), meeting.getTitle(), meeting.getDescription(), meeting.getDate(), meeting.getStartTime(), meeting.getEndTime(), meeting.getRecurrenceType(), meeting.getIsActive());
                                if (rowsAffected > 0) {
                                    Log.d(TAG, "Updated meeting ID " + meeting.getId() + " in DB with new date: " + newDateStr + " and time: " + originalTimeStr);
                                    scheduleAlarmAndOptionallyRollover(getApplicationContext(), meeting, nextFutureOccurrenceStart, currentTime, meetingsForSummaryNotification);
                                } else {
                                    Log.e(TAG, "Failed to update meeting ID " + meeting.getId() + " in DB with new future date.");
                                }
                            } else {
                                Log.w(TAG, "Could not calculate a future occurrence for past recurring meeting ID " + meeting.getId() + ".");
                                AlarmHelper.cancelAllAlarmsForMeeting(getApplicationContext(), meeting.getId());
                            }
                            alarmServicesMM = this;
                        } else {
                            Log.d(TAG, "Meeting ID " + meeting.getId() + " is not recurring and its start time is in the past. Cancelling alarms.");
                            AlarmHelper.cancelAllAlarmsForMeeting(getApplicationContext(), meeting.getId());
                            alarmServicesMM = this;
                        }
                    } else {
                        Log.d(TAG, "Meeting ID " + meeting.getId() + " is scheduled in the future or now. Re-scheduling its alarm/rollover.");
                        alarmServicesMM = this;
                        alarmServicesMM.scheduleAlarmAndOptionallyRollover(getApplicationContext(), meeting, meetingStoredStartCal, currentTime, meetingsForSummaryNotification);
                    }
                }
            }
        }
        if (!meetingsForSummaryNotification.isEmpty()) {
            alarmServicesMM.sendFutureMeetingsSummaryNotification(alarmServicesMM.getApplicationContext(), meetingsForSummaryNotification);
        } else {
            Log.d(TAG, "No future meetings were scheduled or re-scheduled.");
        }
        Log.d(TAG, "Background alarm re-scheduling finished. Stopping service.");
        alarmServicesMM.stopForeground(true);
        alarmServicesMM.stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannels() {
        NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
        if (manager == null) {
            Log.e(TAG, "NotificationManager is null, cannot create channels.");
            return;
        }
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID_SERVICE_OPS, "Alarm Service Operations", 2);
        serviceChannel.setDescription("Channel for background alarm service operations and status.");
        manager.createNotificationChannel(serviceChannel);
        NotificationChannel futureMeetingChannel = new NotificationChannel(CHANNEL_ID_FUTURE_MEETINGS_NOTIF, "Upcoming Meeting Reminders Status", 3);
        futureMeetingChannel.setDescription("Notifications about successfully scheduled future meetings.");
        manager.createNotificationChannel(futureMeetingChannel);
        Log.d(TAG, "Notification channels created.");
    }

    private Calendar parseMeetingDateTime(String dateStr, String timeStr) {
        if (dateStr != null && timeStr != null && !dateStr.trim().isEmpty() && !timeStr.trim().isEmpty()) {
            try {
                Date dateTime = FULL_DATETIME_FORMAT.parse(dateStr + " " + timeStr);
                if (dateTime != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dateTime);
                    calendar.set(13, 0);
                    calendar.set(14, 0);
                    return calendar;
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing meeting date/time: '" + dateStr + " " + timeStr + "'", e);
            }
            return null;
        }
        Log.w(TAG, "parseMeetingDateTime: date or time string is null or empty. Date: [" + dateStr + "], Time: [" + timeStr + "]");
        return null;
    }

    private void scheduleAlarmAndOptionallyRollover(Context context, Meeting meeting, Calendar meetingStartCalendar, Calendar currentTime, List<Meeting> meetingsForSummaryNotificationList) {
        String endTimeStr;
        if (meeting == null) {
            Log.w(TAG, "scheduleAlarmAndOptionallyRollover: Meeting object is null. Skipping.");
            return;
        }
        if (meetingStartCalendar == null) {
            Log.w(TAG, "scheduleAlarmAndOptionallyRollover: meetingStartCalendar is null for meeting ID " + meeting.getId() + ". Skipping.");
            return;
        }
        if (!meetingStartCalendar.after(currentTime)) {
            Log.w(TAG, "scheduleAlarmAndOptionallyRollover: Attempted to schedule for meeting ID " + meeting.getId() + " but its calculated start time (" + FULL_DATETIME_FORMAT.format(meetingStartCalendar.getTime()) + ") is not after current time (" + FULL_DATETIME_FORMAT.format(currentTime.getTime()) + "). Skipping.");
            AlarmHelper.cancelAllAlarmsForMeeting(context, meeting.getId());
            return;
        }
        Log.d(TAG, "Calling AlarmHelper.scheduleReminderForMeeting for meeting ID " + meeting.getId() + " with start time " + FULL_DATETIME_FORMAT.format(meetingStartCalendar.getTime()));
        AlarmHelper.scheduleReminderForMeeting(context, meeting, meetingStartCalendar);
        if (meeting.getIsActive() == 1 && !meetingsForSummaryNotificationList.contains(meeting)) {
            meetingsForSummaryNotificationList.add(meeting);
            Log.d(TAG, "Meeting ID " + meeting.getId() + " added to summary notification list.");
        }
        if (meeting.getRecurrenceType() != 0 && meeting.getIsActive() == 1) {
            Calendar meetingEndCalendar = calculateMeetingEndCalendar(meeting, meetingStartCalendar);
            if (meetingEndCalendar != null && meetingEndCalendar.after(currentTime)) {
                MeetingScheduler.scheduleImmediateRollover(context, meetingEndCalendar, meeting.getId());
                Log.d(TAG, "Rollover worker scheduled for recurring meeting ID " + meeting.getId() + " to run at " + FULL_DATETIME_FORMAT.format(meetingEndCalendar.getTime()));
                return;
            } else {
                if (meetingEndCalendar != null) {
                    endTimeStr = FULL_DATETIME_FORMAT.format(meetingEndCalendar.getTime());
                } else {
                    endTimeStr = "null";
                }
                Log.w(TAG, "Rollover not scheduled for meeting ID " + meeting.getId() + ". Calculated end time (" + endTimeStr + ") is not in the future or could not be calculated.");
                return;
            }
        }
        if (meeting.getRecurrenceType() != 0) {
            Log.d(TAG, "Rollover not scheduled for meeting ID " + meeting.getId() + " because it is recurring but inactive.");
        }
    }

    private Calendar calculateMeetingEndCalendar(Meeting meeting, Calendar currentInstanceStartCalendar) {
        try {
            Date originalStartTimeOfDay = DB_TIME_FORMAT.parse(meeting.getOriginalStartTimeForRollover());
            Date originalEndTimeOfDay = DB_TIME_FORMAT.parse(meeting.getOriginalEndTimeForRollover());
            if (originalStartTimeOfDay != null && originalEndTimeOfDay != null) {
                long durationMillis = originalEndTimeOfDay.getTime() - originalStartTimeOfDay.getTime();
                if (originalEndTimeOfDay.before(originalStartTimeOfDay)) {
                    durationMillis += 86400000;
                }
                Calendar meetingEndCalendar = (Calendar) currentInstanceStartCalendar.clone();
                meetingEndCalendar.add(14, (int) durationMillis);
                return meetingEndCalendar;
            }
            Log.e(TAG, "calculateMeetingEndCalendar: Original start or end time of day is null for meeting ID " + meeting.getId());
            return null;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing original start/end time for calculating duration for meeting ID " + meeting.getId(), e);
            return null;
        }
    }

    private Calendar calculateNextFutureOccurrence(Calendar lastKnownStart, int recurrenceType) {
        Calendar nextOccurrence = (Calendar) lastKnownStart.clone();
        Calendar currentTime = Calendar.getInstance();
        int hour = lastKnownStart.get(11);
        int minute = lastKnownStart.get(12);
        int second = lastKnownStart.get(13);
        int maxAdvances = (recurrenceType == 3 || recurrenceType == 4) ? 120 : 730;
        int advances = 0;
        while (!nextOccurrence.after(currentTime) && advances < maxAdvances) {
            switch (recurrenceType) {
                case 1:
                    nextOccurrence.add(6, 1);
                    break;
                case 2:
                    nextOccurrence.add(3, 1);
                    break;
                case 3:
                    nextOccurrence.add(2, 1);
                    break;
                case 4:
                    nextOccurrence.add(1, 1);
                    break;
                default:
                    Log.e(TAG, "Unknown recurrence type: " + recurrenceType + " for meeting starting at " + FULL_DATETIME_FORMAT.format(lastKnownStart.getTime()));
                    return null;
            }
            nextOccurrence.set(11, hour);
            nextOccurrence.set(12, minute);
            nextOccurrence.set(13, second);
            nextOccurrence.set(14, 0);
            advances++;
        }
        if (nextOccurrence.after(currentTime)) {
            return nextOccurrence;
        }
        Log.w(TAG, "Could not find a future occurrence for meeting (recurrence " + recurrenceType + ") starting " + FULL_DATETIME_FORMAT.format(lastKnownStart.getTime()) + " after " + advances + " advances.");
        return null;
    }

    private void sendFutureMeetingsSummaryNotification(Context context, List<Meeting> meetings) {
        String contentTitle;
        if (meetings.isEmpty()) {
            Log.d(TAG, "sendFutureMeetingsSummaryNotification: No meetings to notify about.");
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        if (notificationManager == null) {
            Log.e(TAG, "sendFutureMeetingsSummaryNotification: NotificationManager is null.");
            return;
        }
        if (meetings.size() == 1) {
            contentTitle = "1 Meeting Reminder Updated";
        } else {
            contentTitle = meetings.size() + " Meeting Reminders Updated";
        }
        StringBuilder contentTextBuilder = new StringBuilder();
        int count = 0;
        for (Meeting meeting : meetings) {
            contentTextBuilder.append(meeting.getTitle()).append(" on ").append(meeting.getDate()).append(" at ").append(meeting.getStartTime()).append("\n");
            count++;
            if (count >= 3 && meetings.size() > 3) {
                contentTextBuilder.append("...and ").append(meetings.size() - count).append(" more.");
                break;
            }
        }
        Intent mainActivityIntent = new Intent(context, (Class<?>) MainActivity.class);
        mainActivityIntent.addFlags(603979776);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, mainActivityIntent, 201326592);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_FUTURE_MEETINGS_NOTIF).setSmallIcon(R.drawable.logo2).setContentTitle(contentTitle).setContentText(contentTextBuilder.toString().trim()).setStyle(new NotificationCompat.BigTextStyle().bigText(contentTextBuilder.toString().trim())).setContentIntent(pendingIntent).setAutoCancel(true);
        notificationManager.notify(2, builder.build());
        Log.d(TAG, "Summary notification sent for " + meetings.size() + " meetings.");
    }
}
