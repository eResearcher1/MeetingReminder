package com.xtraword.apps.meetminder.firstrunfragment;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SlideshowPagerAdapter extends FragmentStateAdapter {
    public SlideshowPagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                break;
        }
        return new SlideFragment1();
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
