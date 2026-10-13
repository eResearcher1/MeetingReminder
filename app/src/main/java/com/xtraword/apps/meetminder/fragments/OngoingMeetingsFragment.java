
package com.xtraword.apps.meetminder.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class OngoingMeetingsFragment
        extends Fragment
        implements LayoutSwitchable {

    private static final String TAG = "OngoingMeetingsFragment";
    private static final long REFRESH_INTERVAL_MS = 5000L;

    private MeetingAdapter adapter;
    private DatabaseHelper databaseHelper;

    private RecyclerView recyclerView;
    private TextView hiddenTextView;

    private int currentLayoutMode = -1;

    private FirstItemTopPaddingDecoration topPaddingDecoration;
    private UniformSpacingDecoration uniformSpacingDecoration;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {

            if (!isAdded() || recyclerView == null) {
                return;
            }

            refreshData();

            handler.postDelayed(
                    this,
                    REFRESH_INTERVAL_MS
            );
        }
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_ongoing_meetings,
                container,
                false
        );

        recyclerView = view.findViewById(R.id.recyclerView);
        hiddenTextView = view.findViewById(R.id.hiddenTextView);

        databaseHelper = new DatabaseHelper(requireContext());

        if (getActivity() instanceof MainActivity) {
            currentLayoutMode =
                    ((MainActivity) requireActivity())
                            .getCurrentLayoutMode();
        } else {
            currentLayoutMode = 1;
        }

        setLayoutManager(currentLayoutMode);
        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {

        adapter = new MeetingAdapter(
                requireContext(),
                new ArrayList<>(),
                currentLayoutMode
        );

        recyclerView.setAdapter(adapter);

        refreshData();
    }

    public void refreshData() {

        if (!isAdded()
                || recyclerView == null
                || hiddenTextView == null
                || databaseHelper == null) {
            return;
        }

        try {
            List<Meeting> allMeetings =
                    databaseHelper.getAllMeetings();

            List<Meeting> ongoingMeetings =
                    getOngoingMeetings(allMeetings);

            if (adapter == null) {
                adapter = new MeetingAdapter(
                        requireContext(),
                        ongoingMeetings,
                        currentLayoutMode
                );

                recyclerView.setAdapter(adapter);

            } else {
                adapter.updateData(ongoingMeetings);
            }

            boolean isEmpty = ongoingMeetings.isEmpty();

            hiddenTextView.setVisibility(
                    isEmpty ? View.VISIBLE : View.GONE
            );

            recyclerView.setVisibility(
                    isEmpty ? View.GONE : View.VISIBLE
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh ongoing meetings", e);
        }
    }

    private void startRefreshing() {

        stopRefreshing();

        handler.postDelayed(
                refreshRunnable,
                REFRESH_INTERVAL_MS
        );
    }

    private void stopRefreshing() {
        handler.removeCallbacks(refreshRunnable);
    }

    private void setLayoutManager(int layoutMode) {

        if (recyclerView == null) {
            currentLayoutMode = layoutMode;
            return;
        }

        if (currentLayoutMode == layoutMode
                && recyclerView.getLayoutManager() != null) {
            return;
        }

        currentLayoutMode = layoutMode;

        if (topPaddingDecoration != null) {
            recyclerView.removeItemDecoration(
                    topPaddingDecoration
            );
            topPaddingDecoration = null;
        }

        if (uniformSpacingDecoration != null) {
            recyclerView.removeItemDecoration(
                    uniformSpacingDecoration
            );
            uniformSpacingDecoration = null;
        }

        if (layoutMode == 1) {

            recyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(
                            2,
                            StaggeredGridLayoutManager.VERTICAL
                    )
            );

        } else {

            recyclerView.setLayoutManager(
                    new LinearLayoutManager(requireContext())
            );
        }

        updateDecoration(layoutMode);
    }

    private void updateDecoration(int layoutMode) {

        if (recyclerView == null || getContext() == null) {
            return;
        }

        int topMargin = getResources().getDimensionPixelSize(
                R.dimen.first_item_margin
        );

        int itemSpacing = getResources().getDimensionPixelSize(
                R.dimen.item_spacing
        );

        boolean applyTopToBothItems = layoutMode == 1;

        topPaddingDecoration =
                new FirstItemTopPaddingDecoration(
                        requireContext(),
                        topMargin,
                        applyTopToBothItems
                );

        uniformSpacingDecoration =
                new UniformSpacingDecoration(
                        layoutMode == 1 ? 2 : 1,
                        itemSpacing
                );

        recyclerView.addItemDecoration(
                topPaddingDecoration
        );

        recyclerView.addItemDecoration(
                uniformSpacingDecoration
        );
    }

    @Override
    public void onLayoutModeChanged(int layoutMode) {

        setLayoutManager(layoutMode);

        if (adapter != null) {
            adapter.setLayoutMode(layoutMode);
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
        stopRefreshing();
        super.onPause();
    }

    @Override
    public void onDestroyView() {

        stopRefreshing();

        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }

        recyclerView = null;
        hiddenTextView = null;
        adapter = null;

        topPaddingDecoration = null;
        uniformSpacingDecoration = null;

        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }

        super.onDestroyView();
    }

    public List<Meeting> getOngoingMeetings(
            List<Meeting> allMeetings) {

        List<Meeting> ongoingMeetings = new ArrayList<>();

        if (allMeetings == null || allMeetings.isEmpty()) {
            return ongoingMeetings;
        }

        long now = System.currentTimeMillis();

        for (Meeting meeting : allMeetings) {

            if (meeting == null || meeting.getIsActive() != 1) {
                continue;
            }

            Calendar startCalendar =
                    parseMeetingDateTime(
                            meeting.getDate(),
                            meeting.getStartTime()
                    );

            Calendar endCalendar =
                    parseMeetingDateTime(
                            meeting.getDate(),
                            meeting.getEndTime()
                    );

            if (startCalendar == null || endCalendar == null) {
                continue;
            }

            // Handle meetings continuing into the following day.
            if (!endCalendar.after(startCalendar)) {
                endCalendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            long start = startCalendar.getTimeInMillis();
            long end = endCalendar.getTimeInMillis();

            if (now >= start && now <= end) {
                ongoingMeetings.add(meeting);
            }
        }

        return ongoingMeetings;
    }

    @Nullable
    private Calendar parseMeetingDateTime(
            String date,
            String time) {

        return com.xtraword.apps.meetminder.alarms.AlarmHelper
                .parseMeetingDateTime(date, time);
    }
}
