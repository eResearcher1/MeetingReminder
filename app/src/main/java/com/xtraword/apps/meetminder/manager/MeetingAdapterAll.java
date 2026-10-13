
package com.xtraword.apps.meetminder.manager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.xtraword.apps.meetminder.EditMeetingActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.worker.ImmediateMeetingRolloverWorker;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeetingAdapterAll
        extends RecyclerView.Adapter<MeetingAdapterAll.MeetingViewHolder> {

    private static final String TAG = "MeetingAdapterAll";
    private static final String PREFS_NAME = "MeetMinderPrefs";

    private final Context context;

    private List<Meeting> meetings;
    private final List<Meeting> allMeetings = new ArrayList<>();

    private final List<Meeting> selectedMeetings = new ArrayList<>();
    private final List<Meeting> selectedPendingMeetings = new ArrayList<>();
    private final List<Meeting> selectedOngoingMeetings = new ArrayList<>();
    private final List<Meeting> selectedPastMeetings = new ArrayList<>();

    private boolean selectAllState = false;

    public MeetingAdapterAll(
            Context context,
            List<Meeting> meetings) {

        this.context = context;

        this.meetings = meetings != null
                ? new ArrayList<>(meetings)
                : new ArrayList<>();

        this.allMeetings.addAll(this.meetings);
        synchronizeSelections();
    }

    public List<Meeting> getSelectedMeetings() {
        return selectedMeetings;
    }

    public void updateData(List<Meeting> newMeetings) {

        allMeetings.clear();

        if (newMeetings != null) {
            allMeetings.addAll(newMeetings);
        }

        meetings = new ArrayList<>(allMeetings);

        synchronizeSelections();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_meeting2,
                        parent,
                        false
                );

        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MeetingViewHolder holder,
            int position) {

        Meeting meeting = meetings.get(position);

        holder.title.setText(meeting.getTitle());
        holder.startTime.setText("Start: " + meeting.getStartTime());
        holder.endTime.setText("End: " + meeting.getEndTime());
        holder.description.setText(meeting.getDescription());
        holder.date.setText("Date: " + meeting.getDate());

        String[] recurrenceOptions = context.getResources()
                .getStringArray(R.array.recurrence_options_with_yearly);

        int recurrenceIndex = meeting.getRecurrenceType();

        holder.reccurring.setText(
                recurrenceIndex >= 0
                        && recurrenceIndex < recurrenceOptions.length
                        ? recurrenceOptions[recurrenceIndex]
                        : "Unknown"
        );

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        int drawableResId = prefs.getInt(
                "meeting_" + meeting.getId() + "_background",
                R.drawable.card_background2
        );

        holder.itemView.setBackground(
                ContextCompat.getDrawable(context, drawableResId)
        );

        updateStatus(holder, meeting);

        ViewGroup.LayoutParams params =
                holder.itemView.getLayoutParams();

        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(params);
        }

        // Remove previous listener before updating recycled checkbox.
        holder.checkBox.setOnCheckedChangeListener(null);

        boolean checked = isSelected(meeting);

        meeting.setChecked(checked);
        holder.checkBox.setChecked(checked);

        updateSelectionBackground(holder, checked);

        holder.checkBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    setMeetingSelected(meeting, isChecked);
                    updateSelectionBackground(holder, isChecked);
                }
        );

        holder.itemView.setOnClickListener(
                v -> showOptionsDialog(meeting, holder.itemView)
        );
    }

    private void updateStatus(
            MeetingViewHolder holder,
            Meeting meeting) {

        String status;
        int color;

        long start = parseDateTime(
                meeting.getDate(),
                meeting.getStartTime()
        );

        long end = parseDateTimeConsideringOvernight(
                meeting.getDate(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );

        long now = System.currentTimeMillis();

        if (meeting.getIsActive() == 0) {
            status = "Inactive";
            color = R.color.inactiveColor;
        } else if (start < 0 || end < 0) {
            status = "Time Error";
            color = R.color.errorColor;
        } else if (now < start) {
            status = "Pending";
            color = R.color.pendingColor;
        } else if (now <= end) {
            status = "Ongoing";
            color = R.color.ongoingColor;
        } else {
            status = "Passed";
            color = R.color.passedColor;
        }

        holder.status.setText(status);
        holder.status.setTextColor(
                ContextCompat.getColor(context, color)
        );
    }

    private void updateSelectionBackground(
            MeetingViewHolder holder,
            boolean checked) {

        holder.layoutLinear.setBackground(
                ContextCompat.getDrawable(
                        context,
                        checked
                                ? R.drawable.background1
                                : R.drawable.background2
                )
        );
    }

    private boolean isSelected(Meeting meeting) {
        return findById(selectedMeetings, meeting.getId()) != null;
    }

    private Meeting findById(List<Meeting> list, int meetingId) {
        for (Meeting item : list) {
            if (item.getId() == meetingId) {
                return item;
            }
        }
        return null;
    }

    private void setMeetingSelected(
            Meeting meeting,
            boolean selected) {

        meeting.setChecked(selected);

        removeById(selectedMeetings, meeting.getId());

        if (selected) {
            selectedMeetings.add(meeting);
        }

        synchronizeCategorySelections();
    }

    private void removeById(List<Meeting> list, int meetingId) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getId() == meetingId) {
                list.remove(i);
            }
        }
    }

    private void synchronizeSelections() {

        List<Meeting> previous = new ArrayList<>(selectedMeetings);
        selectedMeetings.clear();

        for (Meeting meeting : allMeetings) {
            boolean checked = meeting.isChecked()
                    || findById(previous, meeting.getId()) != null;

            meeting.setChecked(checked);

            if (checked) {
                selectedMeetings.add(meeting);
            }
        }

        synchronizeCategorySelections();
    }

    private void synchronizeCategorySelections() {

        selectedPendingMeetings.clear();
        selectedOngoingMeetings.clear();
        selectedPastMeetings.clear();

        long now = System.currentTimeMillis();

        for (Meeting meeting : selectedMeetings) {

            long start = parseDateTime(
                    meeting.getDate(),
                    meeting.getStartTime()
            );

            long end = parseDateTimeConsideringOvernight(
                    meeting.getDate(),
                    meeting.getStartTime(),
                    meeting.getEndTime()
            );

            if (start < 0 || end < 0) {
                continue;
            }

            if (now < start) {
                selectedPendingMeetings.add(meeting);
            } else if (now <= end) {
                selectedOngoingMeetings.add(meeting);
            } else {
                selectedPastMeetings.add(meeting);
            }
        }
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    private void showOptionsDialog(
            Meeting meeting,
            View itemView) {

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_meeting_options, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        LinearLayout drawableButtonsLayout =
                dialogView.findViewById(R.id.drawableButtonsLayout);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();

                Intent intent = new Intent(
                        context,
                        EditMeetingActivity.class
                );

                intent.putExtra("meetingId", meeting.getId());
                context.startActivity(intent);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();

                new AlertDialog.Builder(context)
                        .setTitle("Delete Meeting")
                        .setMessage("Delete this meeting permanently?")
                        .setPositiveButton(
                                "Delete",
                                (d, which) -> deleteMeeting(meeting.getId())
                        )
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        int[] drawables = {
                R.drawable.background1,
                R.drawable.background2,
                R.drawable.background3,
                R.drawable.background4,
                R.drawable.background5,
                R.drawable.background6,
                R.drawable.background7,
                R.drawable.card_background2
        };

        if (drawableButtonsLayout != null) {

            for (int drawable : drawables) {

                Button button = new Button(context);

                button.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f
                        )
                );

                button.setBackground(
                        ContextCompat.getDrawable(context, drawable)
                );

                button.setOnClickListener(v -> {
                    itemView.setBackground(
                            ContextCompat.getDrawable(context, drawable)
                    );

                    saveSelectedDrawable(meeting.getId(), drawable);
                    dialog.dismiss();
                });

                drawableButtonsLayout.addView(button);
            }
        }

        dialog.show();
    }

    private void saveSelectedDrawable(
            int meetingId,
            int drawableResId) {

        context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                ).edit()
                .putInt(
                        "meeting_" + meetingId + "_background",
                        drawableResId
                )
                .apply();
    }

    private void deleteMeeting(int meetingId) {

        DatabaseHelper db = new DatabaseHelper(context);
        boolean deleted;

        try {
            deleted = db.deleteMeeting(meetingId);
        } finally {
            db.close();
        }

        if (!deleted) {
            Toast.makeText(
                    context,
                    "Failed to delete meeting",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        AlarmHelper.cancelAllAlarmsForMeeting(context, meetingId);

        MeetingScheduler.cancelImmediateRollover(context, meetingId);

        ImmediateMeetingRolloverWorker
                .cancelExistingRolloverTrigger(context, meetingId);

        removeById(selectedMeetings, meetingId);
        removeById(selectedPendingMeetings, meetingId);
        removeById(selectedOngoingMeetings, meetingId);
        removeById(selectedPastMeetings, meetingId);
        removeById(allMeetings, meetingId);

        int position = -1;

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getId() == meetingId) {
                position = i;
                break;
            }
        }

        if (position >= 0) {
            meetings.remove(position);
            notifyItemRemoved(position);
        } else {
            notifyDataSetChanged();
        }

        Toast.makeText(
                context,
                "Meeting deleted",
                Toast.LENGTH_SHORT
        ).show();
    }

    public void selectAll1(boolean selectAll) {
        selectAll(selectAll);
    }

    public void selectAll(boolean selectAll) {

        selectAllState = selectAll;
        selectedMeetings.clear();

        for (Meeting meeting : allMeetings) {
            meeting.setChecked(selectAll);

            if (selectAll) {
                selectedMeetings.add(meeting);
            }
        }

        synchronizeCategorySelections();
        notifyDataSetChanged();
    }

    public void selectAllPending(boolean selectAll) {
        selectMeetingsByStatus(0, selectAll);
    }

    public void selectAllOngoing(boolean selectAll) {
        selectMeetingsByStatus(1, selectAll);
    }

    public void selectAllPast(boolean selectAll) {
        selectMeetingsByStatus(2, selectAll);
    }

    private void selectMeetingsByStatus(
            int status,
            boolean select) {

        long now = System.currentTimeMillis();

        for (Meeting meeting : allMeetings) {

            long start = parseDateTime(
                    meeting.getDate(),
                    meeting.getStartTime()
            );

            long end = parseDateTimeConsideringOvernight(
                    meeting.getDate(),
                    meeting.getStartTime(),
                    meeting.getEndTime()
            );

            if (start < 0 || end < 0) {
                continue;
            }

            boolean matches;

            switch (status) {
                case 0:
                    matches = now < start;
                    break;

                case 1:
                    matches = now >= start && now <= end;
                    break;

                case 2:
                    matches = now > end;
                    break;

                default:
                    matches = false;
            }

            if (matches) {
                setMeetingSelected(meeting, select);
            }
        }

        synchronizeCategorySelections();
        notifyDataSetChanged();
    }

    public void filterMeetings(
            boolean pending,
            boolean ongoing,
            boolean past) {

        List<Meeting> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Meeting meeting : allMeetings) {

            long start = parseDateTime(
                    meeting.getDate(),
                    meeting.getStartTime()
            );

            long end = parseDateTimeConsideringOvernight(
                    meeting.getDate(),
                    meeting.getStartTime(),
                    meeting.getEndTime()
            );

            if (start < 0 || end < 0) {
                continue;
            }

            boolean include =
                    (pending && now < start)
                            || (ongoing && now >= start && now <= end)
                            || (past && now > end);

            if (include) {
                filtered.add(meeting);
            }
        }

        meetings = filtered;
        notifyDataSetChanged();
    }

    private static long parseDateTime(
            String date,
            String time) {

        if (date == null || time == null) {
            return -1L;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
            );
            format.setLenient(false);

            Date parsed = format.parse(date + " " + time);
            return parsed != null ? parsed.getTime() : -1L;

        } catch (ParseException e) {
            Log.e(TAG, "Invalid meeting date/time", e);
            return -1L;
        }
    }

    private static long parseDateTimeConsideringOvernight(
            String date,
            String startTime,
            String endTime) {

        long start = parseDateTime(date, startTime);
        long end = parseDateTime(date, endTime);

        if (start < 0 || end < 0) {
            return -1L;
        }

        if (end <= start) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(end);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTimeInMillis();
        }

        return end;
    }

    public static class MeetingViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final TextView description;
        final TextView date;
        final TextView startTime;
        final TextView endTime;
        final TextView status;
        final TextView reccurring;

        final LinearLayout layoutLinear;
        final CheckBox checkBox;

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.meetingTitle);
            description = itemView.findViewById(R.id.description);
            date = itemView.findViewById(R.id.date);
            startTime = itemView.findViewById(R.id.startTime);
            endTime = itemView.findViewById(R.id.endTime);
            status = itemView.findViewById(R.id.status);
            reccurring = itemView.findViewById(R.id.reccurring);

            layoutLinear = itemView.findViewById(R.id.layoutLinear);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
