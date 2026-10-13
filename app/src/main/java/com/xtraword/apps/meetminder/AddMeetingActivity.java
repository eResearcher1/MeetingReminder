package com.xtraword.apps.meetminder;

import android.app.AlarmManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.firstrunfragment.PermissionActivity;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMeetingActivity extends BaseActivity {
    private static final String PREFS_NAME2 = "app_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 123;
    private static final String TAG = "AddMeetingActivity";
    private Spinner activeOrNot;
    private AdView adView;
    private DatePicker datePicker;
    private DatabaseHelper dbHelper;
    private EditText descriptionEditText;
    private EditText editDuration;
    private Button saveButton;
    private Spinner spinnerRecurrence;
    private TextView statusPermission;
    private TimePicker timePickerFrom;
    private EditText titleEditText;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Meeting pendingMeetingForAlarmScheduling = null;
    private Calendar pendingCalendarForAlarmScheduling = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meeting);
        this.dbHelper = new DatabaseHelper(this);
        initializeViews();
        initializeAds();
        this.statusPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AddMeetingActivity.this.m280x63a64fe1(view);
            }
        });
        checkBatteryOptimization();
        this.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AddMeetingActivity.this.m281x632fe9e2(view);
            }
        });
    }

    void m280x63a64fe1(View v) {
        Intent intent = new Intent(this, (Class<?>) PermissionActivity.class);
        startActivity(intent);
    }

    void m281x632fe9e2(View v) {
        saveMeeting();
    }

    private void initializeViews() {
        this.spinnerRecurrence = (Spinner) findViewById(R.id.spinnerRecurrence);
        this.activeOrNot = (Spinner) findViewById(R.id.activeOrNot);
        this.titleEditText = (EditText) findViewById(R.id.editTitle);
        this.descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        this.datePicker = (DatePicker) findViewById(R.id.datePicker);
        this.timePickerFrom = (TimePicker) findViewById(R.id.timePickerFrom);
        this.editDuration = (EditText) findViewById(R.id.editDuration);
        this.saveButton = (Button) findViewById(R.id.btnSaveMeeting);
        this.statusPermission = (TextView) findViewById(R.id.statusPermission);
        this.timePickerFrom.setIs24HourView(true);
    }

    private void saveMeeting() {
        AlarmManager alarmManager;
        String title = this.titleEditText.getText().toString().trim();
        String description = this.descriptionEditText.getText().toString().trim();
        String durationStr = this.editDuration.getText().toString().trim();
        int recurrenceType = this.spinnerRecurrence.getSelectedItemPosition();
        int isActiveSelection = this.activeOrNot.getSelectedItemPosition() == 0 ? 1 : 0;
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", 0).show();
            return;
        }
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "Duration cannot be empty", 0).show();
            return;
        }
        try {
            int durationMinutes = parseDurationMinutesOrThrow(durationStr);
            Calendar calendarFrom = buildMeetingStartCalendar();
            if (calendarFrom.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(this, "Meeting start time must be in the future.", 0).show();
                return;
            }
            Calendar calendarTo = (Calendar) calendarFrom.clone();
            calendarTo.add(12, durationMinutes);
            if (calendarTo.getTimeInMillis() <= calendarFrom.getTimeInMillis()) {
                Toast.makeText(this, "Calculated end time must be after start time. Check duration.", 1).show();
                return;
            }
            String dateString = DATE_FORMAT.format(calendarFrom.getTime());
            String startTimeString = TIME_FORMAT.format(calendarFrom.getTime());
            String endTimeString = TIME_FORMAT.format(calendarTo.getTime());
            Meeting newMeeting = new Meeting();
            newMeeting.setTitle(title);
            newMeeting.setDescription(description);
            newMeeting.setDate(dateString);
            newMeeting.setStartTime(startTimeString);
            newMeeting.setEndTime(endTimeString);
            newMeeting.setRecurrenceType(recurrenceType);
            newMeeting.setIsActive(isActiveSelection);
            long meetingId = this.dbHelper.addMeeting(title, description, dateString, startTimeString, endTimeString, recurrenceType, isActiveSelection);
            if (meetingId == -1) {
                Toast.makeText(this, "Error saving meeting to database.", 0).show();
                return;
            }
            newMeeting.setId((int) meetingId);
            if (newMeeting.getIsActive() == 1) {
                Log.d(TAG, "Attempting to schedule on-time alarm for new active meeting ID: " + newMeeting.getId());
                if (Build.VERSION.SDK_INT >= 31 && (alarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM)) != null && !alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted. Requesting...");
                    showExactAlarmPermissionRationale(newMeeting, calendarFrom);
                    Toast.makeText(this, "Meeting saved. Please grant permission for reminders.", 1).show();
                    return;
                }
                scheduleMeetingAlarmWithAlarmHelper(this, newMeeting, calendarFrom);
            } else {
                Log.d(TAG, "New meeting saved as inactive. Cancelling any alarms.");
                AlarmHelper.cancelAllAlarmsForMeeting(this, newMeeting.getId());
            }
            if (newMeeting.getIsActive() == 1 && newMeeting.getRecurrenceType() != 0) {
                Calendar endCalendarForWorker = (Calendar) calendarTo.clone();
                if (endCalendarForWorker.getTimeInMillis() > System.currentTimeMillis()) {
                    Log.d(TAG, "Scheduling rollover worker for meeting ID: " + newMeeting.getId() + " at " + endCalendarForWorker.getTime());
                    MeetingScheduler.scheduleImmediateRollover(this, endCalendarForWorker, newMeeting.getId());
                } else {
                    Log.w(TAG, "Rollover time is in the past. Not scheduling rollover worker.");
                }
            }
            Toast.makeText(this, "Meeting saved successfully!", 0).show();
            if (this.pendingMeetingForAlarmScheduling == null) {
                finish();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid duration. Example: 1 or 1.5", 0).show();
        }
    }

    private void scheduleMeetingAlarmWithAlarmHelper(Context context, Meeting meeting, Calendar calendarFrom) {
        if (context == null) {
            return;
        }
        if (meeting == null) {
            Log.w(TAG, "Meeting is null. Cannot schedule alarm.");
            return;
        }
        if (calendarFrom == null) {
            Log.w(TAG, "calendarFrom is null for meeting ID: " + meeting.getId());
            return;
        }
        if (meeting.getIsActive() != 1) {
            Log.d(TAG, "Meeting " + meeting.getId() + " inactive. Cancelling alarms.");
            AlarmHelper.cancelAllAlarmsForMeeting(context, meeting.getId());
        } else if (calendarFrom.getTimeInMillis() <= System.currentTimeMillis()) {
            Log.w(TAG, "Meeting start time is in the past. Not scheduling. ID=" + meeting.getId());
            AlarmHelper.cancelAllAlarmsForMeeting(context, meeting.getId());
        } else {
            AlarmHelper.cancelAllAlarmsForMeeting(context, meeting.getId());
            Log.i(TAG, "Scheduling on-time alarm for meeting ID: " + meeting.getId() + " @ " + calendarFrom.getTime());
            AlarmHelper.scheduleReminderForMeeting(context, meeting, calendarFrom);
        }
    }

    private void showExactAlarmPermissionRationale(Meeting meetingToSchedule, Calendar calendarForMeeting) {
        this.pendingMeetingForAlarmScheduling = meetingToSchedule;
        this.pendingCalendarForAlarmScheduling = calendarForMeeting;
        new AlertDialog.Builder(this).setTitle("Permission Needed for Reminders").setMessage("This app needs permission to schedule exact alarms for timely meeting reminders. Please grant the 'Alarms & reminders' permission in the app settings.").setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                AddMeetingActivity.this.m282x2c1bff47(dialogInterface, i);
            }
        }).setNegativeButton("Cancel Scheduling", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                AddMeetingActivity.this.m283x2ba59948(dialogInterface, i);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public final void onCancel(DialogInterface dialogInterface) {
                AddMeetingActivity.this.m284x2b2f3349(dialogInterface);
            }
        }).setCancelable(false).show();
    }

    void m282x2c1bff47(DialogInterface dialog, int which) {
        if (Build.VERSION.SDK_INT >= 31) {
            Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
            try {
                startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM);
            } catch (Exception e) {
                Log.e(TAG, "Could not open exact alarm settings: " + e.getMessage());
                Toast.makeText(this, "Could not open settings. Please grant 'Alarms & reminders' permission manually.", 1).show();
                clearPendingAlarmDataAndFinish();
            }
        }
    }

    void m283x2ba59948(DialogInterface dialog, int which) {
        Toast.makeText(this, "Meeting saved, but reminders won't be scheduled without permission.", 1).show();
        clearPendingAlarmDataAndFinish();
    }

    void m284x2b2f3349(DialogInterface dialog) {
        Toast.makeText(this, "Meeting saved, but reminders won't be scheduled without permission.", 1).show();
        clearPendingAlarmDataAndFinish();
    }

    private void clearPendingAlarmDataAndFinish() {
        this.pendingMeetingForAlarmScheduling = null;
        this.pendingCalendarForAlarmScheduling = null;
        if (!isFinishing() && !isDestroyed()) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM && Build.VERSION.SDK_INT >= 31) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "SCHEDULE_EXACT_ALARM permission GRANTED after returning from settings.");
                if (this.pendingMeetingForAlarmScheduling != null && this.pendingCalendarForAlarmScheduling != null) {
                    Toast.makeText(this, "Permission granted! Scheduling reminder...", 0).show();
                    scheduleMeetingAlarmWithAlarmHelper(this, this.pendingMeetingForAlarmScheduling, this.pendingCalendarForAlarmScheduling);
                } else {
                    Log.w(TAG, "Permission granted, but no pending meeting data to schedule.");
                    Toast.makeText(this, "Permission granted. If reminder was not set, please edit the meeting.", 1).show();
                }
            } else {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission still NOT granted after returning from settings.");
                Toast.makeText(this, "Reminders cannot be scheduled without 'Alarms & reminders' permission.", 1).show();
            }
            clearPendingAlarmDataAndFinish();
        }
    }

    private Calendar buildMeetingStartCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(this.datePicker.getYear(), this.datePicker.getMonth(), this.datePicker.getDayOfMonth(), this.timePickerFrom.getHour(), this.timePickerFrom.getMinute(), 0);
        cal.set(14, 0);
        return cal;
    }

    private int parseDurationMinutesOrThrow(String durationStr) throws NumberFormatException {
        float hours = Float.parseFloat(durationStr);
        if (hours <= 0.0f) {
            throw new NumberFormatException("Duration must be > 0");
        }
        return Math.max(1, Math.round(60.0f * hours));
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService("power");
        if (pm == null) {
            this.statusPermission.setVisibility(8);
            return;
        }
        boolean isBatteryOptimizationIgnored = pm.isIgnoringBatteryOptimizations(getPackageName());
        if (isBatteryOptimizationIgnored) {
            this.statusPermission.setVisibility(8);
            return;
        }
        this.statusPermission.setVisibility(0);
        this.statusPermission.setText("Battery optimization is enabled. Please CLICK HERE to disable it to allow the app to send alerts for meetings even when the app is closed.");
        this.statusPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AddMeetingActivity.this.m279x31b8825d(view);
            }
        });
    }

    void m279x31b8825d(View v) {
        Intent intent = new Intent();
        intent.setAction("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
        intent.setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open battery optimization settings.", 1).show();
            Intent permissionIntent = new Intent(this, (Class<?>) PermissionActivity.class);
            startActivity(permissionIntent);
        }
    }

    private void initializeAds() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public final void onInitializationComplete(InitializationStatus initializationStatus) {
                AddMeetingActivity.lambda$initializeAds$6(initializationStatus);
            }
        });
        this.adView = (AdView) findViewById(R.id.adView);
        updateAdVisibility();
    }

    static void lambda$initializeAds$6(InitializationStatus initializationStatus) {
    }

    private void updateAdVisibility() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME2, 0);
        boolean adsRemoved = prefs.getBoolean(PREF_ADS_REMOVED, false);
        if (this.adView != null) {
            if (adsRemoved) {
                this.adView.setVisibility(8);
                return;
            }
            this.adView.setVisibility(0);
            AdRequest adRequest = new AdRequest.Builder().build();
            this.adView.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBatteryOptimization();
        updateAdVisibility();
    }
}
