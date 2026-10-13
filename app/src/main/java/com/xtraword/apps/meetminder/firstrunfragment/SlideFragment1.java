package com.xtraword.apps.meetminder.firstrunfragment;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.internal.view.SupportMenu;
import androidx.fragment.app.Fragment;
import com.google.common.net.HttpHeaders;
import com.xtraword.apps.meetminder.R;

public class SlideFragment1 extends Fragment {
    private static final String KEY_BATTERY_OPTIMIZATION_SHOWN = "battery_optimization_shown";
    private static final String PREFS_NAME_BATTERY = "battery_optimization_prefs";
    Button btnRequestBatteryOptPermission;
    Button btnRequestExactAlarmPermission;
    Button btnRequestNotificationsPermission;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() {
        @Override
        public final void onActivityResult(Object obj) {
            SlideFragment1.this.m314x5e76784f((Boolean) obj);
        }
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slide1, container, false);
    }

    void m314x5e76784f(Boolean isGranted) {
        if (isGranted.booleanValue()) {
            Toast.makeText(requireContext(), "Notification permission is Granted.", 0).show();
            this.btnRequestNotificationsPermission.setBackgroundColor(-16711936);
        } else {
            Toast.makeText(requireContext(), "Notification permission is required for alarms to work correctly.", 0).show();
            this.btnRequestNotificationsPermission.setBackgroundColor(SupportMenu.CATEGORY_MASK);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.btnRequestNotificationsPermission = (Button) view.findViewById(R.id.btnRequestNotificationsPermission);
        this.btnRequestExactAlarmPermission = (Button) view.findViewById(R.id.btnRequestExactAlarmPermission);
        this.btnRequestBatteryOptPermission = (Button) view.findViewById(R.id.btnRequestBatteryOptPermission);
        this.btnRequestNotificationsPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                SlideFragment1.this.m315xc62007aa(view2);
            }
        });
        this.btnRequestExactAlarmPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                SlideFragment1.this.m316x5a5e7749(view2);
            }
        });
        this.btnRequestBatteryOptPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                SlideFragment1.this.m317xee9ce6e8(view2);
            }
        });
    }

    void m315xc62007aa(View v) {
        requestNotificationPermission();
    }

    void m316x5a5e7749(View v) {
        checkAndRequestExactAlarmPermission();
    }

    void m317xee9ce6e8(View v) {
        showBatteryOptimizationDialog();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getContext(), "android.permission.POST_NOTIFICATIONS") != 0) {
                this.requestNotificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
            } else {
                Toast.makeText(getActivity(), "Notification permission is already granted.", 0).show();
            }
        }
    }

    private void showBatteryOptimizationDialog() {
        PowerManager pm = (PowerManager) requireContext().getSystemService("power");
        if (pm == null) {
            Toast.makeText(requireContext(), "Unable to access Power Manager.", 0).show();
            return;
        }
        if (!pm.isIgnoringBatteryOptimizations(requireActivity().getPackageName())) {
            new AlertDialog.Builder(requireContext()).setTitle("Battery Optimization").setMessage("To ensure alarms and notifications work correctly, please disable battery optimization for this app.").setPositiveButton(HttpHeaders.ALLOW, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    SlideFragment1.this.m318x1e6e0406(dialogInterface, i);
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
            return;
        }
        Button btnRequestBatteryOptPermission = (Button) requireView().findViewById(R.id.btnRequestBatteryOptPermission);
        if (btnRequestBatteryOptPermission != null) {
            btnRequestBatteryOptPermission.setBackgroundColor(-16711936);
        }
        Toast.makeText(requireContext(), "Battery Optimization is already disabled as required.", 0).show();
    }

    void m318x1e6e0406(DialogInterface dialog, int which) {
        Intent intent = new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Could not open settings.", 0).show();
            Log.e("SlideFragment1", "Error opening battery optimization settings", e);
        }
        dialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPermissionStates();
    }

    private void refreshPermissionStates() {
        if (Build.VERSION.SDK_INT >= 33) {
            boolean notifGranted = ContextCompat.checkSelfPermission(requireContext(), "android.permission.POST_NOTIFICATIONS") == 0;
            this.btnRequestNotificationsPermission.setBackgroundColor(notifGranted ? -16711936 : -65536);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            this.btnRequestExactAlarmPermission.setBackgroundColor(isExactAlarmPermissionGranted() ? -16711936 : -65536);
        }
        PowerManager pm = (PowerManager) requireContext().getSystemService("power");
        boolean batteryOk = pm != null && pm.isIgnoringBatteryOptimizations(requireActivity().getPackageName());
        this.btnRequestBatteryOptPermission.setBackgroundColor(batteryOk ? -16711936 : -65536);
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < 31) {
            this.btnRequestExactAlarmPermission.setBackgroundColor(-16711936);
            Toast.makeText(getActivity(), "Exact alarm permission is not required on this Android version.", 0).show();
        } else if (isExactAlarmPermissionGranted()) {
            this.btnRequestExactAlarmPermission.setBackgroundColor(-16711936);
            Toast.makeText(getActivity(), "Alarms & reminders permission is granted.", 0).show();
        } else {
            this.btnRequestExactAlarmPermission.setBackgroundColor(SupportMenu.CATEGORY_MASK);
            requestExactAlarmPermission();
        }
    }

    private boolean isExactAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(NotificationCompat.CATEGORY_ALARM);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    private void requestExactAlarmPermission() {
        AlarmManager alarmManager;
        if (Build.VERSION.SDK_INT >= 31 && (alarmManager = (AlarmManager) requireContext().getSystemService(NotificationCompat.CATEGORY_ALARM)) != null && !alarmManager.canScheduleExactAlarms()) {
            try {
                Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Could not open exact alarm settings.", 0).show();
            }
        }
    }
}
