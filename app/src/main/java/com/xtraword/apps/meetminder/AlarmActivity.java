package com.xtraword.apps.meetminder;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.xtraword.apps.meetminder.foreground.ReminderService;

public class AlarmActivity extends AppCompatActivity {
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_MEETING_ID = "meeting_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_TITLE = "title";
    private String description;
    private int meetingId = -1;
    private int notificationId = -1;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String str;
        super.onCreate(savedInstanceState);
        turnScreenOnAndShowOverLockScreen();
        setContentView(R.layout.activity_alarm);
        setVolumeControlStream(4);
        Intent intent = getIntent();
        this.meetingId = intent.getIntExtra("meeting_id", -1);
        this.notificationId = intent.getIntExtra("notification_id", -1);
        this.title = intent.getStringExtra("title");
        this.description = intent.getStringExtra("description");
        TextView tvTitle = (TextView) findViewById(R.id.tvAlarmTitle);
        TextView tvDesc = (TextView) findViewById(R.id.tvAlarmDescription);
        Button btnDismiss = (Button) findViewById(R.id.btnDismissAlarm);
        Button btnSnooze = (Button) findViewById(R.id.btnSnoozeAlarm);
        tvTitle.setText((this.title == null || this.title.trim().isEmpty()) ? "Meeting Reminder" : this.title);
        if (this.description == null || this.description.trim().isEmpty()) {
            str = "Your meeting is starting now.";
        } else {
            str = this.description;
        }
        tvDesc.setText(str);
        btnDismiss.setOnClickListener(v -> dismissAlarm());
        btnSnooze.setOnClickListener(v -> snoozeAlarm());
    }

    private void dismissAlarm() {
        Intent dismissIntent = new Intent(this, (Class<?>) ReminderService.class);
        dismissIntent.setAction(ReminderService.ACTION_DISMISS);
        dismissIntent.putExtra("meeting_id", this.meetingId);
        dismissIntent.putExtra("notification_id", this.notificationId);
        startService(dismissIntent);
        finish();
    }

    private void snoozeAlarm() {
        Intent snoozeIntent = new Intent(this, (Class<?>) ReminderService.class);
        snoozeIntent.setAction(ReminderService.ACTION_SNOOZE_10);
        snoozeIntent.putExtra("meeting_id", this.meetingId);
        snoozeIntent.putExtra("notification_id", this.notificationId);
        startService(snoozeIntent);
        finish();
    }

    private void turnScreenOnAndShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService("keyguard");
            if (km != null) {
                km.requestDismissKeyguard(this, null);
                return;
            }
            return;
        }
        getWindow().addFlags(6816896);
    }
}
