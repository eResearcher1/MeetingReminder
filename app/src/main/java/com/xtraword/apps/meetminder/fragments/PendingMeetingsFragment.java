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

public class PendingMeetingsFragment extends Fragment implements LayoutSwitchable {
    private static final String TAG = "PendingMeetingsFragment";
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
        View view = inflater.inflate(R.layout.fragment_pending_meetings, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        this.hiddenTextView = (TextView) view.findViewById(R.id.hiddenTextView);
        this.databaseHelper = new DatabaseHelper(getContext());
        this.hiddenTextView.setVisibility(8);
        this.handler = new Handler(Looper.getMainLooper());
        this.refreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(PendingMeetingsFragment.TAG, "Runnable running");
                PendingMeetingsFragment.this.refreshData();
                PendingMeetingsFragment.this.handler.postDelayed(this, 4000L);
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
        List<Meeting> allMeetingsFromDb = this.databaseHelper.getAllMeetings();
        List<Meeting> meetingsToShow = getPendingMeetings(allMeetingsFromDb);
        if (meetingsToShow.isEmpty()) {
            this.hiddenTextView.setVisibility(0);
            if (this.adapter == null) {
                this.adapter = new MeetingAdapter(requireContext(), new ArrayList(), this.currentLayoutMode);
                this.recyclerView.setAdapter(this.adapter);
                return;
            } else {
                this.adapter.updateData(new ArrayList());
                return;
            }
        }
        this.hiddenTextView.setVisibility(8);
        if (this.adapter == null) {
            this.adapter = new MeetingAdapter(requireContext(), meetingsToShow, this.currentLayoutMode);
            this.recyclerView.setAdapter(this.adapter);
        } else {
            this.adapter.updateData(meetingsToShow);
            this.adapter.setLayoutMode(this.currentLayoutMode);
        }
    }

    private void startRefreshing() {
        Log.d(TAG, "startRefreshing() called");
        this.handler.removeCallbacks(this.refreshRunnable);
        this.handler.post(this.refreshRunnable);
    }

    private void stopRefreshing() {
        Log.d(TAG, "stopRefreshing() called");
        this.handler.removeCallbacks(this.refreshRunnable);
    }

    public void refreshData() {
        Log.d(TAG, "refreshData() method called");
        List<Meeting> allMeetingsFromDb = this.databaseHelper.getAllMeetings();
        List<Meeting> meetingsToShow = getPendingMeetings(allMeetingsFromDb);
        if (this.adapter != null) {
            this.adapter.updateData(meetingsToShow);
            this.adapter.setLayoutMode(this.currentLayoutMode);
            if (meetingsToShow.isEmpty()) {
                this.hiddenTextView.setVisibility(0);
                return;
            } else {
                this.hiddenTextView.setVisibility(8);
                return;
            }
        }
        setupRecyclerView();
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
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
        refreshData();
        startRefreshing();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
        stopRefreshing();
    }

    public List<Meeting> getPendingMeetings(List<Meeting> allMeetings) {
        List<Meeting> futureActiveMeetings = new ArrayList<>();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date currentDate = new Date();
        for (Meeting meeting : allMeetings) {
            if (meeting.getIsActive() != 0) {
                try {
                    String meetingDateTimeStr = meeting.getDate() + " " + meeting.getStartTime();
                    Date meetingStartDateTime = dateTimeFormat.parse(meetingDateTimeStr);
                    if (meetingStartDateTime != null && meetingStartDateTime.after(currentDate)) {
                        futureActiveMeetings.add(meeting);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing meeting date/time for meeting ID: " + meeting.getId(), e);
                }
            }
        }
        return futureActiveMeetings;
    }

    @Override
    public void onLayoutModeChanged(int layoutMode) {
        setLayoutManager(layoutMode);
        if (this.adapter != null) {
            this.recyclerView.getRecycledViewPool().clear();
            this.adapter.setLayoutMode(layoutMode);
        }
    }
}
