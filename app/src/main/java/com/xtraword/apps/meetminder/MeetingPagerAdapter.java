package com.xtraword.apps.meetminder;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.xtraword.apps.meetminder.fragments.CalenderFragment;
import com.xtraword.apps.meetminder.fragments.OngoingMeetingsFragment;
import com.xtraword.apps.meetminder.fragments.PassedMeetingsFragment;
import com.xtraword.apps.meetminder.fragments.PendingMeetingsFragment;

public class MeetingPagerAdapter extends FragmentStateAdapter {
    public MeetingPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new PendingMeetingsFragment();
        }
        if (position == 1) {
            return new OngoingMeetingsFragment();
        }
        if (position == 2) {
            return new PassedMeetingsFragment();
        }
        return new CalenderFragment();
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
