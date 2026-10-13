package com.xtraword.apps.meetminder.firstrun;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PermissionSlideAdapter extends FragmentStateAdapter {
    public PermissionSlideAdapter(FragmentActivity fa) {
        super(fa);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SlideFragment("Notifications Permission", "We need this permission to keep you updated on important events.", "Grant Permission", "android.permission.POST_NOTIFICATIONS");
            case 1:
                return new SlideFragment("Exact Alarms Permission", "We need this permission to schedule critical tasks, ensuring smooth operation.", "Grant Permission", "android.permission.SCHEDULE_EXACT_ALARM");
            case 2:
                return new SlideFragment("Boot Completed Permission", "This permission allows the app to start background tasks when your device restarts.", "Grant Permission", "android.permission.RECEIVE_BOOT_COMPLETED");
            case 3:
                return new BatteryOptimizationFragment();
            default:
                return new SlideFragment("", "", "", "");
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
