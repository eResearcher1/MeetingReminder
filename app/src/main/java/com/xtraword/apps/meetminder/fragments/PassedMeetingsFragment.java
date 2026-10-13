package com.xtraword.apps.meetminder.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.xtraword.apps.meetminder.MainActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.MeetingAdapter;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.cosmetics.FirstItemTopPaddingDecoration;
import com.xtraword.apps.meetminder.cosmetics.UniformSpacingDecoration;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassedMeetingsFragment extends Fragment implements LayoutSwitchable {
    private static final String TAG = "PassedMeetingsFragment";
    private MeetingAdapter adapter;
    private int currentLayoutMode = -1;
    private DatabaseHelper databaseHelper;
    private Handler handler;
    private TextView hiddenTextView;
    private RecyclerView recyclerView;
    private Runnable refreshRunnable;
    private RecyclerView.ItemDecoration spacingDecoration;
    private FirstItemTopPaddingDecoration topPaddingDecoration;
    private UniformSpacingDecoration uniformSpacingDecoration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passed_meetings, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        this.hiddenTextView = (TextView) view.findViewById(R.id.hiddenTextView);
        this.databaseHelper = new DatabaseHelper(getContext());
        this.hiddenTextView.setVisibility(8);
        this.handler = new Handler(Looper.getMainLooper());
        this.refreshRunnable = new Runnable() {
            @Override
            public void run() {
                PassedMeetingsFragment.this.refreshData();
                PassedMeetingsFragment.this.handler.postDelayed(this, 4000L);
            }
        };
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            this.currentLayoutMode = activity.getCurrentLayoutMode();
        } else {
            this.currentLayoutMode = 1;
        }
        setLayoutManager(this.currentLayoutMode);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        List<Meeting> allMeetings = this.databaseHelper.getAllMeetings();
        List<Meeting> passedMeetings = getPassedMeetings(allMeetings);
        if (this.adapter == null) {
            this.adapter = new MeetingAdapter(requireContext(), passedMeetings, this.currentLayoutMode);
            this.recyclerView.setAdapter(this.adapter);
        } else {
            this.adapter.updateData(passedMeetings);
            this.adapter.setLayoutMode(this.currentLayoutMode);
        }
        if (passedMeetings.isEmpty()) {
            this.hiddenTextView.setVisibility(0);
            this.recyclerView.setVisibility(8);
        } else {
            this.hiddenTextView.setVisibility(8);
            this.recyclerView.setVisibility(0);
        }
    }

    private void startRefreshing() {
        this.handler.removeCallbacks(this.refreshRunnable);
        this.handler.post(this.refreshRunnable);
    }

    private void stopRefreshing() {
        this.handler.removeCallbacks(this.refreshRunnable);
    }

    public void refreshData() {
        List<Meeting> allMeetings = this.databaseHelper.getAllMeetings();
        List<Meeting> passedMeetings = getPassedMeetings(allMeetings);
        if (this.adapter != null) {
            this.adapter.updateData(passedMeetings);
            this.adapter.setLayoutMode(this.currentLayoutMode);
        } else {
            this.adapter = new MeetingAdapter(requireContext(), passedMeetings, this.currentLayoutMode);
            this.recyclerView.setAdapter(this.adapter);
        }
        if (passedMeetings.isEmpty()) {
            this.hiddenTextView.setVisibility(0);
            this.recyclerView.setVisibility(8);
        } else {
            this.hiddenTextView.setVisibility(8);
            this.recyclerView.setVisibility(0);
        }
    }

    private void setLayoutManager(int layoutMode) {
        if (this.currentLayoutMode == layoutMode && this.recyclerView.getLayoutManager() != null) {
            return;
        }
        this.currentLayoutMode = layoutMode;
        if (this.topPaddingDecoration != null) {
            this.recyclerView.removeItemDecoration(this.topPaddingDecoration);
        }
        if (this.uniformSpacingDecoration != null) {
            this.recyclerView.removeItemDecoration(this.uniformSpacingDecoration);
        }
        if (layoutMode == 1) {
            this.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, 1));
        } else {
            this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        updateDecoration(layoutMode);
    }

    private void updateDecoration(int layoutMode) {
        if (getContext() == null) {
            return;
        }
        int topMargin = getResources().getDimensionPixelSize(R.dimen.first_item_margin);
        int itemSpacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        boolean applyTopToBothItems = layoutMode == 1;
        this.topPaddingDecoration = new FirstItemTopPaddingDecoration(getContext(), topMargin, applyTopToBothItems);
        if (layoutMode == 1) {
            this.uniformSpacingDecoration = new UniformSpacingDecoration(2, itemSpacing);
        } else {
            this.uniformSpacingDecoration = new UniformSpacingDecoration(1, itemSpacing);
        }
        this.recyclerView.addItemDecoration(this.topPaddingDecoration);
        this.recyclerView.addItemDecoration(this.uniformSpacingDecoration);
    }

    @Override
    public void onLayoutModeChanged(int layoutMode) {
        setLayoutManager(layoutMode);
        if (this.adapter != null) {
            this.adapter.setLayoutMode(layoutMode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
        startRefreshing();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRefreshing();
    }

    public List<Meeting> getPassedMeetings(List<Meeting> allMeetings) {
        List<Meeting> passedMeetings = new ArrayList<>();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date currentDate = new Date();
        for (Meeting meeting : allMeetings) {
            if (meeting.getIsActive() == 0) {
                passedMeetings.add(meeting);
            } else {
                try {
                    String meetingEndDateTimeStr = meeting.getDate() + " " + meeting.getEndTime();
                    Date meetingEndDateTime = dateTimeFormat.parse(meetingEndDateTimeStr);
                    if (meetingEndDateTime != null && currentDate.after(meetingEndDateTime)) {
                        passedMeetings.add(meeting);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing end date/time for meeting ID: " + meeting.getId(), e);
                }
            }
        }
        return passedMeetings;
    }
}
