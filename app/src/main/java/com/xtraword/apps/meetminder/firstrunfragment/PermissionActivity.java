package com.xtraword.apps.meetminder.firstrunfragment;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.common.net.HttpHeaders;
import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.R;

public class PermissionActivity extends BaseActivity {
    private static final String KEY_BATTERY_OPTIMIZATION_SHOWN = "battery_optimization_shown";
    private static final String PREFS_NAME_BATTERY = "battery_optimization_prefs";
    private Button btnRequestBatteryOptPermission;
    private Button btnRequestExactAlarmPermission;
    private Button btnRequestNotificationPermission;
    private CheckBox chkBatteryOptPermission;
    private CheckBox chkExactAlarmPermission;
    private CheckBox chkNotificationPermission;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() {
        @Override
        public final void onActivityResult(Object obj) {
            PermissionActivity.this.m308xe2f48d9d((Boolean) obj);
        }
    });
    private TextView txtPermissionGuidance;

    void m308xe2f48d9d(Boolean isGranted) {
        updatePermissionCheckboxes();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        this.txtPermissionGuidance = (TextView) findViewById(R.id.txtPermissionGuidance);
        this.chkNotificationPermission = (CheckBox) findViewById(R.id.chkNotificationPermission);
        this.chkExactAlarmPermission = (CheckBox) findViewById(R.id.chkExactAlarmPermission);
        this.chkBatteryOptPermission = (CheckBox) findViewById(R.id.chkBatteryOptPermission);
        this.btnRequestNotificationPermission = (Button) findViewById(R.id.btnRequestNotificationPermission);
        this.btnRequestExactAlarmPermission = (Button) findViewById(R.id.btnRequestExactAlarmPermission);
        this.btnRequestBatteryOptPermission = (Button) findViewById(R.id.btnRequestBatteryOptPermission);
        this.chkNotificationPermission.setClickable(false);
        this.chkExactAlarmPermission.setClickable(false);
        this.chkBatteryOptPermission.setClickable(false);
        updatePermissionCheckboxes();
        this.btnRequestNotificationPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PermissionActivity.this.m309xda3294e7(view);
            }
        });
        this.btnRequestExactAlarmPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PermissionActivity.this.m310x44621d06(view);
            }
        });
        this.btnRequestBatteryOptPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PermissionActivity.this.m311xae91a525(view);
            }
        });
    }

    void m309xda3294e7(View v) {
        requestNotificationPermission();
    }

    void m310x44621d06(View v) {
        checkAndRequestExactAlarmPermission();
    }

    void m311xae91a525(View v) {
        showBatteryOptimizationDialog();
    }

    private void updatePermissionCheckboxes() {
        if (Build.VERSION.SDK_INT >= 33) {
            boolean isNotificationPermissionGranted = ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == 0;
            this.chkNotificationPermission.setChecked(isNotificationPermissionGranted);
            if (isNotificationPermissionGranted) {
                this.chkNotificationPermission.setText("Notification Permission Granted");
            } else {
                this.chkNotificationPermission.setText("Notification Permission Not Granted");
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
            boolean isExactAlarmPermissionGranted = alarmManager != null && alarmManager.canScheduleExactAlarms();
            this.chkExactAlarmPermission.setChecked(isExactAlarmPermissionGranted);
            if (isExactAlarmPermissionGranted) {
                this.chkExactAlarmPermission.setText("Exact Alarm Permission Granted");
            } else {
                this.chkExactAlarmPermission.setText("Exact Alarm Permission Not Granted");
            }
        }
        PowerManager pm = (PowerManager) getSystemService("power");
        boolean isBatteryOptimizationIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        this.chkBatteryOptPermission.setChecked(isBatteryOptimizationIgnored);
        if (isBatteryOptimizationIgnored) {
            this.chkBatteryOptPermission.setText("Baterry Optimization Disabled (Recommended)");
        } else {
            this.chkBatteryOptPermission.setText("Baterry Optimization Not Disabled");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.POST_NOTIFICATIONS") != 0) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.POST_NOTIFICATIONS")) {
                    new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This app requires notification permission to send you important alerts.").setPositiveButton(HttpHeaders.ALLOW, new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            PermissionActivity.this.m312x6f237e50(dialogInterface, i);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
                    return;
                } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.POST_NOTIFICATIONS")) {
                    this.txtPermissionGuidance.setVisibility(0);
                    this.txtPermissionGuidance.setText("Notification permission was denied the first time. To enable it, go to Settings > Apps > MeetMinder App > Permissions, and make sure to select allow notifications.");
                    return;
                } else {
                    this.requestNotificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
                    return;
                }
            }
            Toast.makeText(this, "Notification permission is already granted.", 0).show();
        }
    }

    void m312x6f237e50(DialogInterface dialog, int which) {
        this.requestNotificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (isExactAlarmPermissionGranted()) {
                Toast.makeText(this, "Set Alarm permission is Granted.", 0).show();
            } else {
                requestExactAlarmPermission();
            }
        }
    }

    private boolean isExactAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(NotificationCompat.CATEGORY_ALARM);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    private void requestExactAlarmPermission() {
        AlarmManager alarmManager;
        if (Build.VERSION.SDK_INT >= 31 && (alarmManager = (AlarmManager) getApplicationContext().getSystemService(NotificationCompat.CATEGORY_ALARM)) != null && !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
            startActivity(intent);
        }
    }

    private void showBatteryOptimizationDialog() {
        PowerManager pm = (PowerManager) getSystemService("power");
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            new AlertDialog.Builder(this).setTitle("Battery Optimization").setMessage("To ensure alarms and notifications work correctly, please disable battery optimization for this app.").setPositiveButton(HttpHeaders.ALLOW, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PermissionActivity.this.m313x54a07612(dialogInterface, i);
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
        } else {
            Toast.makeText(this, "Battery Optimization is already disabled as required.", 0).show();
        }
    }

    void m313x54a07612(DialogInterface dialog, int which) {
        Intent intent = new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Could not open settings.", 0).show();
            Log.e("PermissionActivity", "Error opening battery optimization settings", e);
        }
        dialog.dismiss();
    }
}
