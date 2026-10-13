package com.xtraword.apps.meetminder.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xtraword.apps.meetminder.MainActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.TodayMeetingAdapter;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalenderFragment extends Fragment {
    private TodayMeetingAdapter adapter;
    private CalendarView calendarView;
    private DatabaseHelper databaseHelper;
    private TextView dateToday;
    private TextView hiddenTextView;
    private RecyclerView recyclerViewTodayMeetingsList;
    private String todayDate;
    private List<Meeting> allMeetings = new ArrayList();
    private List<Meeting> meetingsForToday = new ArrayList();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calender, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.dateToday = (TextView) view.findViewById(R.id.dateToday);
        this.hiddenTextView = (TextView) view.findViewById(R.id.hiddenTextView);
        this.hiddenTextView.setVisibility(8);
        this.calendarView = (CalendarView) view.findViewById(R.id.calendarView);
        this.recyclerViewTodayMeetingsList = (RecyclerView) view.findViewById(R.id.recyclerViewTodayMeetingsList);
        this.databaseHelper = new DatabaseHelper(getContext());
        this.allMeetings = this.databaseHelper.getAllMeetings();
        this.todayDate = this.dateFormat.format(new Date());
        this.dateToday.setText("Meetings on: " + this.todayDate);
        this.recyclerViewTodayMeetingsList.setLayoutManager(new LinearLayoutManager(getContext()));
        this.adapter = new TodayMeetingAdapter(this.meetingsForToday);
        this.recyclerViewTodayMeetingsList.setAdapter(this.adapter);
        displayMeetingsForToday();
        this.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public final void onSelectedDayChange(CalendarView calendarView, int i, int i2, int i3) {
                CalenderFragment.this.m325x13b9d73b(calendarView, i, i2, i3);
            }
        });
    }

    void m325x13b9d73b(CalendarView view1, int year, int month, int dayOfMonth) {
        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", Integer.valueOf(year), Integer.valueOf(month + 1), Integer.valueOf(dayOfMonth));
        displayMeetingsForDate(selectedDate);
    }

    private void displayMeetingsForToday() {
        List<Meeting> pendingMeetings = getPendingMeetings(this.allMeetings);
        this.meetingsForToday.clear();
        for (Meeting meeting : pendingMeetings) {
            if (meeting.getDate().equals(this.todayDate)) {
                this.meetingsForToday.add(meeting);
            }
        }
        updateRecyclerView();
    }

    private void displayMeetingsForDate(String selectedDate) {
        List<Meeting> pendingMeetings = getPendingMeetings(this.allMeetings);
        this.meetingsForToday.clear();
        for (Meeting meeting : pendingMeetings) {
            if (meeting.getDate().equals(selectedDate)) {
                this.meetingsForToday.add(meeting);
            }
        }
        updateRecyclerView();
        this.dateToday.setText("Pending Meetings on: " + selectedDate);
        this.hiddenTextView.setVisibility(8);
        if (this.meetingsForToday.isEmpty()) {
            this.hiddenTextView.setVisibility(0);
            this.hiddenTextView.setText("No Pending Meetings for this Day");
        }
    }

    private void updateRecyclerView() {
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        } else {
            this.adapter = new TodayMeetingAdapter(this.meetingsForToday);
            this.recyclerViewTodayMeetingsList.setAdapter(this.adapter);
            this.hiddenTextView.setVisibility(8);
        }
        this.hiddenTextView.setVisibility(8);
        if (this.meetingsForToday.isEmpty()) {
            this.hiddenTextView.setVisibility(0);
            this.hiddenTextView.setText("No Pending Meetings for Today");
        }
    }

    public List<Meeting> getPendingMeetings(List<Meeting> allMeetings) {
        List<Meeting> pendingMeetings = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date currentDate = new Date();
        for (Meeting meeting : allMeetings) {
            try {
                String meetingDateTime = meeting.getDate() + " " + meeting.getStartTime();
                Date meetingStartDate = dateFormat.parse(meetingDateTime);
                if (meetingStartDate != null && meetingStartDate.after(currentDate)) {
                    pendingMeetings.add(meeting);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return pendingMeetings;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.allMeetings = this.databaseHelper.getAllMeetings();
        displayMeetingsForToday();
        this.dateToday.setText("Pending Meetings on: " + this.todayDate);
        long todayMillis = System.currentTimeMillis();
        this.calendarView.setDate(todayMillis, true, true);
        ((MainActivity) requireActivity()).setToolbarVisibility(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity) requireActivity()).setToolbarVisibility(true);
    }
}
