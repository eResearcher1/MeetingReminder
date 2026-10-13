package com.xtraword.apps.meetminder.firstrunfragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;
import com.xtraword.apps.meetminder.R;

public class SlideshowDialogFragment extends DialogFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slideshow_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 viewPager = (ViewPager2) view.findViewById(R.id.viewPager);
        SlideshowPagerAdapter adapter = new SlideshowPagerAdapter(this);
        viewPager.setAdapter(adapter);
        view.findViewById(R.id.closeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                SlideshowDialogFragment.this.m319xc56f470b(view2);
            }
        });
    }

    void m319xc56f470b(View v) {
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = -1;
        params.height = -2;
        getDialog().getWindow().setAttributes((WindowManager.LayoutParams) params);
    }
}
