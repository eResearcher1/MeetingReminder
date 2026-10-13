
package com.xtraword.apps.meetminder.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ImmediateMeetingRolloverWorker extends Worker {

    public static final String KEY_MEETING_ID_TO_ROLLOVER =
            "KEY_MEETING_ID_TO_ROLLOVER";

    private static final String TAG = "ImmMeetingRolloverWrk";

    private static final int MAX_ROLLOVER_ITERATIONS = 10000;

    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
            );

    public static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
            );

    public static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
            );

    public ImmediateMeetingRolloverWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        int meetingId = getInputData().getInt(
                KEY_MEETING_ID_TO_ROLLOVER,
                -1
        );

        if (meetingId < 0) {
            Log.e(TAG, "Invalid meeting ID for rollover");
            return Result.failure();
        }

        Context context = getApplicationContext();

        Log.d(
                TAG,
                "Starting meeting rollover. ID=" + meetingId
        );

        DatabaseHelper databaseHelper = null;

        try {
            databaseHelper = new DatabaseHelper(context);

            Meeting currentMeeting =
                    databaseHelper.getMeetingById(meetingId);

            if (currentMeeting == null) {
                Log.w(
                        TAG,
                        "Meeting not found. ID=" + meetingId
                );

                return Result.failure();
            }

            if (currentMeeting.getIsActive() != 1
                    || currentMeeting.getRecurrenceType() == 0) {

                Log.d(
                        TAG,
                        "Meeting inactive or non-recurring. ID="
                                + meetingId
                );

                AlarmHelper.cancelAllAlarmsForMeeting(
                        context,
                        meetingId
                );

                return Result.success();
            }

            Calendar currentStart =
                    AlarmHelper.parseMeetingDateTime(
                            currentMeeting.getDate(),
                            currentMeeting.getStartTime()
                    );

            Calendar currentEnd =
                    AlarmHelper.parseMeetingDateTime(
                            currentMeeting.getDate(),
                            currentMeeting.getEndTime()
                    );

            if (currentStart == null || currentEnd == null) {
                Log.e(
                        TAG,
                        "Invalid meeting date/time. ID=" + meetingId
                );

                return Result.failure();
            }

            // An end time equal to or earlier than the start
            // represents a meeting ending on the following day.
            if (!currentEnd.after(currentStart)) {
                currentEnd.add(Calendar.DAY_OF_MONTH, 1);
            }

            long durationMillis =
                    currentEnd.getTimeInMillis()
                            - currentStart.getTimeInMillis();

            if (durationMillis <= 0) {
                Log.e(
                        TAG,
                        "Invalid meeting duration. ID=" + meetingId
                );

                return Result.failure();
            }

            Calendar nextStart =
                    (Calendar) currentStart.clone();

            int recurrenceType =
                    currentMeeting.getRecurrenceType();

            long now = System.currentTimeMillis();

            boolean advanced = false;

            // Advance at least once, then skip fully expired
            // occurrences if WorkManager was delayed.
            for (int iteration = 0;
                 iteration < MAX_ROLLOVER_ITERATIONS;
                 iteration++) {

                if (!advanceCalendarToNextOccurrence(
                        nextStart,
                        recurrenceType)) {

                    Log.e(
                            TAG,
                            "Unsupported recurrence type: "
                                    + recurrenceType
                    );

                    return Result.failure();
                }

                advanced = true;

                Calendar candidateEnd =
                        (Calendar) nextStart.clone();

                candidateEnd.setTimeInMillis(
                        nextStart.getTimeInMillis()
                                + durationMillis
                );

                if (candidateEnd.getTimeInMillis() > now) {
                    break;
                }

                if (iteration == MAX_ROLLOVER_ITERATIONS - 1) {
                    Log.e(
                            TAG,
                            "Exceeded rollover iteration limit. ID="
                                    + meetingId
                    );

                    return Result.failure();
                }
            }

            if (!advanced) {
                return Result.failure();
            }

            Calendar nextEnd =
                    (Calendar) nextStart.clone();

            nextEnd.setTimeInMillis(
                    nextStart.getTimeInMillis()
                            + durationMillis
            );

            String newDate = formatDate(nextStart);
            String newStartTime = formatTime(nextStart);
            String newEndTime = formatTime(nextEnd);

            Log.d(
                    TAG,
                    "Rolling over meeting ID="
                            + meetingId
                            + ", old date="
                            + currentMeeting.getDate()
                            + ", new date="
                            + newDate
            );

            int rows = databaseHelper.updateMeeting(
                    meetingId,
                    currentMeeting.getTitle(),
                    currentMeeting.getDescription(),
                    newDate,
                    newStartTime,
                    newEndTime,
                    recurrenceType,
                    currentMeeting.getIsActive()
            );

            if (rows <= 0) {
                Log.e(
                        TAG,
                        "Database update failed. ID=" + meetingId
                );

                return Result.failure();
            }

            Meeting nextMeeting = new Meeting();

            nextMeeting.setId(meetingId);
            nextMeeting.setTitle(currentMeeting.getTitle());
            nextMeeting.setDescription(
                    currentMeeting.getDescription()
            );
            nextMeeting.setDate(newDate);
            nextMeeting.setStartTime(newStartTime);
            nextMeeting.setEndTime(newEndTime);
            nextMeeting.setRecurrenceType(recurrenceType);
            nextMeeting.setIsActive(
                    currentMeeting.getIsActive()
            );

            AlarmHelper.cancelAllAlarmsForMeeting(
                    context,
                    meetingId
            );

            if (nextStart.getTimeInMillis()
                    > System.currentTimeMillis()) {

                AlarmHelper.scheduleReminderForMeeting(
                        context,
                        nextMeeting,
                        nextStart
                );

            } else {
                Log.d(
                        TAG,
                        "Next meeting occurrence already started. ID="
                                + meetingId
                );
            }

            // Schedule rollover at the end of this occurrence.
            if (nextEnd.getTimeInMillis()
                    > System.currentTimeMillis()) {

                MeetingScheduler.scheduleImmediateRollover(
                        context,
                        nextEnd,
                        meetingId
                );

            } else {
                Log.w(
                        TAG,
                        "Next rollover end is already past. ID="
                                + meetingId
                );
            }

            Log.i(
                    TAG,
                    "Rollover completed. Meeting ID="
                            + meetingId
                            + ", next date="
                            + newDate
            );

            return Result.success();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Meeting rollover failed. ID=" + meetingId,
                    e
            );

            return Result.failure();

        } finally {

            if (databaseHelper != null) {
                try {
                    databaseHelper.close();
                } catch (Exception e) {
                    Log.e(
                            TAG,
                            "Error closing database",
                            e
                    );
                }
            }
        }
    }

    private boolean advanceCalendarToNextOccurrence(
            Calendar calendar,
            int recurrenceType) {

        switch (recurrenceType) {

            case 1:
                // Daily
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                return true;

            case 2:
                // Weekly
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                return true;

            case 3:
                // Monthly
                calendar.add(Calendar.MONTH, 1);
                return true;

            case 4:
                // Yearly
                calendar.add(Calendar.YEAR, 1);
                return true;

            default:
                Log.w(
                        TAG,
                        "Unknown recurrence type: "
                                + recurrenceType
                );
                return false;
        }
    }

    private static String formatDate(Calendar calendar) {

        SimpleDateFormat formatter = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        );

        return formatter.format(calendar.getTime());
    }

    private static String formatTime(Calendar calendar) {

        SimpleDateFormat formatter = new SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
        );

        return formatter.format(calendar.getTime());
    }

    public static void cancelExistingRolloverTrigger(
            Context context,
            int meetingId) {

        if (context == null || meetingId < 0) {
            return;
        }

        String uniqueWorkName =
                "rollover_trigger_" + meetingId;

        WorkManager.getInstance(
                context.getApplicationContext()
        ).cancelUniqueWork(uniqueWorkName);

        Log.d(
                TAG,
                "Cancelled rollover trigger: " + uniqueWorkName
        );
    }
}
