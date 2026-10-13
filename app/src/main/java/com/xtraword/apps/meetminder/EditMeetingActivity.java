
package com.xtraword.apps.meetminder;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.worker.ImmediateMeetingRolloverWorker;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EditMeetingActivity extends BaseActivity {

    private static final String TAG = "EditMeetingActivity";

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";

    private Spinner spinnerRecurrence;
    private Spinner activeOrNot;

    private EditText editTitle;
    private EditText editDescription;
    private EditText editDuration;

    private DatePicker datePickerEdit;
    private TimePicker timePickerFromEdit;

    private Button btnSave;
    private AdView adView;

    private DatabaseHelper databaseHelper;
    private Meeting currentMeeting;

    private int meetingId = -1;

    private Meeting pendingMeetingForAlarmScheduling;
    private Calendar pendingCalendarForAlarmScheduling;
    private Calendar pendingEndCalendarForRollover;

    private ActivityResultLauncher<Intent> exactAlarmPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_meeting);

        registerExactAlarmPermissionLauncher();
        initializeViews();

        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();

        if (intent == null || !intent.hasExtra("meetingId")) {
            Toast.makeText(
                    this,
                    "No Meeting ID Selected",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        meetingId = intent.getIntExtra("meetingId", -1);

        if (meetingId < 0) {
            Toast.makeText(
                    this,
                    "Invalid Meeting ID",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        loadMeetingData(meetingId);

        btnSave.setOnClickListener(v -> updateMeeting());
    }

    private void initializeViews() {

        spinnerRecurrence = findViewById(R.id.spinnerRecurrence);
        activeOrNot = findViewById(R.id.activeOrNot);

        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editDuration = findViewById(R.id.editDuration);

        datePickerEdit = findViewById(R.id.datePickerEdit);
        timePickerFromEdit = findViewById(R.id.timePickerFromEdit);

        btnSave = findViewById(R.id.btnSave);
        adView = findViewById(R.id.adView);

        timePickerFromEdit.setIs24HourView(true);

        setupSpinners();

        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads initialized");
        });

        updateAdVisibility();
    }

    private void setupSpinners() {

        ArrayAdapter<CharSequence> recurrenceAdapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.recurrence_options_with_yearly,
                        android.R.layout.simple_spinner_item
                );

        recurrenceAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerRecurrence.setAdapter(recurrenceAdapter);

        ArrayAdapter<CharSequence> activeAdapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.active_options,
                        android.R.layout.simple_spinner_item
                );

        activeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        activeOrNot.setAdapter(activeAdapter);
    }

    private void loadMeetingData(int id) {

        currentMeeting = databaseHelper.getMeetingById(id);

        if (currentMeeting == null) {
            Toast.makeText(
                    this,
                    "Meeting not found",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        editTitle.setText(
                currentMeeting.getTitle() != null
                        ? currentMeeting.getTitle()
                        : ""
        );

        editDescription.setText(
                currentMeeting.getDescription() != null
                        ? currentMeeting.getDescription()
                        : ""
        );

        Calendar startCalendar = AlarmHelper.parseMeetingDateTime(
                currentMeeting.getDate(),
                currentMeeting.getStartTime()
        );

        if (startCalendar == null) {
            startCalendar = Calendar.getInstance();

            Log.w(
                    TAG,
                    "Invalid stored meeting start date/time. ID=" + id
            );
        }

        datePickerEdit.updateDate(
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
        );

        timePickerFromEdit.setHour(
                startCalendar.get(Calendar.HOUR_OF_DAY)
        );

        timePickerFromEdit.setMinute(
                startCalendar.get(Calendar.MINUTE)
        );

        loadDuration();
        loadSpinnerSelections();
    }

    private void loadDuration() {

        Calendar start = AlarmHelper.parseMeetingDateTime(
                currentMeeting.getDate(),
                currentMeeting.getStartTime()
        );

        Calendar end = AlarmHelper.parseMeetingDateTime(
                currentMeeting.getDate(),
                currentMeeting.getEndTime()
        );

        if (start == null || end == null) {
            editDuration.setText("1.0");
            return;
        }

        if (!end.after(start)) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }

        long differenceMillis =
                end.getTimeInMillis() - start.getTimeInMillis();

        long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(
                differenceMillis
        );

        if (durationMinutes < 1) {
            durationMinutes = 60;
        }

        double durationHours = durationMinutes / 60.0;

        editDuration.setText(
                String.format(Locale.US, "%.1f", durationHours)
        );
    }

    private void loadSpinnerSelections() {

        int recurrenceType = currentMeeting.getRecurrenceType();

        if (recurrenceType < 0
                || recurrenceType >= spinnerRecurrence.getCount()) {
            recurrenceType = 0;
        }

        spinnerRecurrence.setSelection(recurrenceType);

        activeOrNot.setSelection(
                currentMeeting.getIsActive() == 1 ? 0 : 1
        );
    }

    private void updateMeeting() {

        String title = editTitle.getText().toString().trim();
        String description =
                editDescription.getText().toString().trim();

        String durationText =
                editDuration.getText().toString().trim();

        int recurrenceType =
                spinnerRecurrence.getSelectedItemPosition();

        int activeStatus =
                activeOrNot.getSelectedItemPosition() == 0 ? 1 : 0;

        if (title.isEmpty()) {
            editTitle.setError("Title cannot be empty");
            editTitle.requestFocus();
            return;
        }

        if (durationText.isEmpty()) {
            editDuration.setError("Duration cannot be empty");
            editDuration.requestFocus();
            return;
        }

        int durationMinutes;

        try {
            durationMinutes = parseDurationMinutesOrThrow(
                    durationText
            );
        } catch (NumberFormatException e) {
            editDuration.setError(
                    "Enter a valid duration, e.g. 1 or 1.5"
            );
            editDuration.requestFocus();
            return;
        }

        Calendar startCalendar =
                buildMeetingStartCalendarFromEdit();

        Calendar endCalendar =
                (Calendar) startCalendar.clone();

        endCalendar.add(
                Calendar.MINUTE,
                durationMinutes
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        );

        SimpleDateFormat timeFormat = new SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
        );

        String date = dateFormat.format(
                startCalendar.getTime()
        );

        String startTime = timeFormat.format(
                startCalendar.getTime()
        );

        String endTime = timeFormat.format(
                endCalendar.getTime()
        );

        int result = databaseHelper.updateMeeting(
                meetingId,
                title,
                description,
                date,
                startTime,
                endTime,
                recurrenceType,
                activeStatus
        );

        if (result <= 0) {
            Toast.makeText(
                    this,
                    "Failed to update meeting",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Meeting updatedMeeting = new Meeting();

        updatedMeeting.setId(meetingId);
        updatedMeeting.setTitle(title);
        updatedMeeting.setDescription(description);
        updatedMeeting.setDate(date);
        updatedMeeting.setStartTime(startTime);
        updatedMeeting.setEndTime(endTime);
        updatedMeeting.setRecurrenceType(recurrenceType);
        updatedMeeting.setIsActive(activeStatus);

        // Cancel reminders associated with the previous schedule.
        AlarmHelper.cancelAllAlarmsForMeeting(
                this,
                meetingId
        );

        // Always cancel the previous rollover before scheduling a new one.
        MeetingScheduler.cancelImmediateRollover(
                this,
                meetingId
        );

        ImmediateMeetingRolloverWorker
                .cancelExistingRolloverTrigger(
                        this,
                        meetingId
                );

        setResult(RESULT_OK);

        if (activeStatus == 0) {
            completeUpdate();
            return;
        }

        long now = System.currentTimeMillis();

        if (startCalendar.getTimeInMillis() > now) {

            if (canScheduleExactAlarms()) {
                scheduleUpdatedMeetingAlarm(
                        updatedMeeting,
                        startCalendar
                );

                scheduleRolloverIfNeeded(
                        updatedMeeting,
                        endCalendar
                );

                completeUpdate();
            } else {
                showExactAlarmPermissionRationale(
                        updatedMeeting,
                        startCalendar,
                        endCalendar
                );
            }

        } else {
            scheduleRolloverIfNeeded(
                    updatedMeeting,
                    endCalendar
            );

            completeUpdate();
        }
    }

    private void completeUpdate() {

        Toast.makeText(
                this,
                "Meeting updated successfully",
                Toast.LENGTH_SHORT
        ).show();

        setResult(RESULT_OK);
        finish();
    }

    private void scheduleUpdatedMeetingAlarm(
            Meeting meeting,
            Calendar startCalendar) {

        if (meeting == null || startCalendar == null) {
            return;
        }

        if (meeting.getIsActive() != 1) {
            return;
        }

        if (startCalendar.getTimeInMillis()
                <= System.currentTimeMillis()) {
            return;
        }

        AlarmHelper.scheduleReminderForMeeting(
                this,
                meeting,
                startCalendar
        );
    }

    private void scheduleRolloverIfNeeded(
            Meeting meeting,
            Calendar endCalendar) {

        if (meeting == null || endCalendar == null) {
            return;
        }

        if (meeting.getIsActive() != 1
                || meeting.getRecurrenceType() == 0) {
            return;
        }

        if (endCalendar.getTimeInMillis()
                <= System.currentTimeMillis()) {

            Log.w(
                    TAG,
                    "Recurring meeting end has passed. ID="
                            + meeting.getId()
            );
            return;
        }

        MeetingScheduler.scheduleImmediateRollover(
                this,
                endCalendar,
                meeting.getId()
        );
    }

    private Calendar buildMeetingStartCalendarFromEdit() {

        Calendar calendar = Calendar.getInstance();

        calendar.set(
                datePickerEdit.getYear(),
                datePickerEdit.getMonth(),
                datePickerEdit.getDayOfMonth(),
                timePickerFromEdit.getHour(),
                timePickerFromEdit.getMinute(),
                0
        );

        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private int parseDurationMinutesOrThrow(String durationText) {

        double hours = Double.parseDouble(durationText);

        if (!Double.isFinite(hours) || hours <= 0) {
            throw new NumberFormatException(
                    "Duration must be a positive finite number"
            );
        }

        double minutes = hours * 60.0;

        if (minutes > Integer.MAX_VALUE) {
            throw new NumberFormatException(
                    "Duration is too large"
            );
        }

        return Math.max(1, (int) Math.round(minutes));
    }

    private boolean canScheduleExactAlarms() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(
                        Context.ALARM_SERVICE
                );

        return alarmManager != null
                && alarmManager.canScheduleExactAlarms();
    }

    private void registerExactAlarmPermissionLauncher() {

        exactAlarmPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> handleExactAlarmPermissionReturn()
                );
    }

    private void showExactAlarmPermissionRationale(
            Meeting meeting,
            Calendar startCalendar,
            Calendar endCalendar) {

        pendingMeetingForAlarmScheduling = meeting;
        pendingCalendarForAlarmScheduling =
                (Calendar) startCalendar.clone();
        pendingEndCalendarForRollover =
                (Calendar) endCalendar.clone();

        new AlertDialog.Builder(this)
                .setTitle("Permission Needed for Reminders")
                .setMessage(
                        "The meeting has been updated, but exact alarm " +
                                "permission is required to schedule its reminder. " +
                                "Please allow Alarms & reminders in app settings."
                )
                .setPositiveButton(
                        "Go to Settings",
                        (dialog, which) -> openExactAlarmSettings()
                )
                .setNegativeButton(
                        "Don't Update Reminders",
                        (dialog, which) -> {
                            Toast.makeText(
                                    this,
                                    "Meeting saved without a reminder",
                                    Toast.LENGTH_LONG
                            ).show();

                            clearPendingAlarmDataAndFinish();
                        }
                )
                .setCancelable(false)
                .show();
    }

    private void openExactAlarmSettings() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            handleExactAlarmPermissionReturn();
            return;
        }

        Intent intent = new Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        );

        intent.setData(
                Uri.parse("package:" + getPackageName())
        );

        try {
            exactAlarmPermissionLauncher.launch(intent);

        } catch (Exception e) {
            Log.e(
                    TAG,
                    "Could not open exact alarm settings",
                    e
            );

            Toast.makeText(
                    this,
                    "Could not open settings. Grant permission manually.",
                    Toast.LENGTH_LONG
            ).show();

            clearPendingAlarmDataAndFinish();
        }
    }

    private void handleExactAlarmPermissionReturn() {

        if (pendingMeetingForAlarmScheduling == null
                || pendingCalendarForAlarmScheduling == null) {
            return;
        }

        if (canScheduleExactAlarms()) {

            scheduleUpdatedMeetingAlarm(
                    pendingMeetingForAlarmScheduling,
                    pendingCalendarForAlarmScheduling
            );

            scheduleRolloverIfNeeded(
                    pendingMeetingForAlarmScheduling,
                    pendingEndCalendarForRollover
            );

            Toast.makeText(
                    this,
                    "Reminder permission granted",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Meeting saved, but reminder permission was not granted",
                    Toast.LENGTH_LONG
            ).show();
        }

        clearPendingAlarmDataAndFinish();
    }

    private void clearPendingAlarmDataAndFinish() {

        pendingMeetingForAlarmScheduling = null;
        pendingCalendarForAlarmScheduling = null;
        pendingEndCalendarForRollover = null;

        if (!isFinishing() && !isDestroyed()) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void updateAdVisibility() {

        SharedPreferences prefs = getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        boolean adsRemoved = prefs.getBoolean(
                PREF_ADS_REMOVED,
                false
        );

        if (adView == null) {
            return;
        }

        if (adsRemoved) {
            adView.setVisibility(View.GONE);
            return;
        }

        adView.setVisibility(View.VISIBLE);

        AdRequest request = new AdRequest.Builder().build();
        adView.loadAd(request);
    }

    @Override
    protected void onDestroy() {

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }

        super.onDestroy();
    }
}
