
package com.xtraword.apps.meetminder;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.worker.ImmediateMeetingRolloverWorker;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeetingAdapter
        extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {

    private static final String TAG = "MeetingAdapter";
    private static final String PREFS_NAME = "MeetMinderPrefs";

    private final Context context;
    private List<Meeting> meetings;
    private int layoutMode;

    public MeetingAdapter(
            Context context,
            List<Meeting> meetings,
            int layoutMode) {

        this.context = context;
        this.meetings = meetings != null
                ? new ArrayList<>(meetings)
                : new ArrayList<>();
        this.layoutMode = layoutMode;
    }

    public void updateData(List<Meeting> newMeetings) {
        this.meetings = newMeetings != null
                ? new ArrayList<>(newMeetings)
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    public void setLayoutMode(int layoutMode) {
        if (this.layoutMode != layoutMode) {
            this.layoutMode = layoutMode;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return layoutMode;
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        int layoutResId = viewType == 0
                ? R.layout.item_meeting_linear
                : R.layout.item_meeting;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutResId, parent, false);

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
                        : "None"
        );

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int drawableResId = prefs.getInt(
                "meeting_" + meeting.getId() + "_background",
                R.drawable.card_background2
        );

        holder.itemView.setBackground(
                ContextCompat.getDrawable(context, drawableResId)
        );

        updateMeetingStatus(holder, meeting);

        holder.itemView.setOnClickListener(
                v -> showOptionsDialog(meeting, holder.itemView)
        );

        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(params);
        }
    }

    private void updateMeetingStatus(
            MeetingViewHolder holder,
            Meeting meeting) {

        String status;
        int color;

        if (meeting.getIsActive() == 0) {
            status = "Inactive";
            color = R.color.inactiveColor;
        } else {
            long start = parseDateTime(
                    meeting.getDate(),
                    meeting.getStartTime()
            );

            long end = parseDateTime(
                    meeting.getDate(),
                    meeting.getEndTime()
            );

            if (start < 0 || end < 0) {
                status = "Time Error";
                color = R.color.errorColor;
            } else {
                if (end < start) {
                    end += 24L * 60L * 60L * 1000L;
                }

                long now = System.currentTimeMillis();

                if (now < start) {
                    status = "Pending";
                    color = R.color.pendingColor;
                } else if (now <= end) {
                    status = "Ongoing";
                    color = R.color.ongoingColor;
                } else {
                    status = "Passed";
                    color = R.color.passedColor;
                }
            }
        }

        holder.status.setText(status);
        holder.status.setTextColor(
                ContextCompat.getColor(context, color)
        );
    }

    private long parseDateTime(String date, String time) {
        if (date == null || time == null) {
            return -1L;
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
            );
            formatter.setLenient(false);

            Date parsed = formatter.parse(date + " " + time);
            return parsed != null ? parsed.getTime() : -1L;

        } catch (ParseException e) {
            Log.e(TAG, "Invalid date/time: " + date + " " + time, e);
            return -1L;
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

        SwitchMaterial switchActive =
                dialogView.findViewById(R.id.switchMeetingActive);

        LinearLayout backgrounds =
                dialogView.findViewById(R.id.drawableButtonsLayout);

        if (switchActive != null) {
            switchActive.setChecked(meeting.getIsActive() == 1);

            switchActive.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        dialog.dismiss();
                        changeMeetingActiveStatus(meeting, isChecked);
                    }
            );
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();

                Intent intent = new Intent(
                        context,
                        EditMeetingActivity.class
                );

                // Retain the APK's original extra key.
                intent.putExtra("meetingId", meeting.getId());
                context.startActivity(intent);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();

                new AlertDialog.Builder(context)
                        .setTitle("Delete Meeting")
                        .setMessage("Are you sure you want to delete this meeting?")
                        .setPositiveButton("Delete", (d, which) ->
                                deleteMeeting(meeting))
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

        if (backgrounds != null) {
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
                    saveSelectedDrawable(meeting.getId(), drawable);
                    itemView.setBackground(
                            ContextCompat.getDrawable(context, drawable)
                    );
                    dialog.dismiss();
                });

                backgrounds.addView(button);
            }
        }

        dialog.show();
    }

    private void changeMeetingActiveStatus(
            Meeting meeting,
            boolean active) {

        DatabaseHelper db = new DatabaseHelper(context);
        boolean updated;

        try {
            updated = db.updateMeetingActiveStatus(
                    meeting.getId(),
                    active ? 1 : 0
            );
        } finally {
            db.close();
        }

        if (!updated) {
            Toast.makeText(
                    context,
                    "Failed to update meeting status",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        meeting.setIsActive(active ? 1 : 0);

        int position = findMeetingPosition(meeting.getId());
        if (position >= 0) {
            notifyItemChanged(position);
        }

        if (active) {
            if (canScheduleExactAlarms()) {
                handleMeetingActivation(meeting);
            } else {
                showPermissionNeededDialogForAlarms();
            }
        } else {
            AlarmHelper.cancelAllAlarmsForMeeting(
                    context,
                    meeting.getId()
            );

            ImmediateMeetingRolloverWorker
                    .cancelExistingRolloverTrigger(
                            context,
                            meeting.getId()
                    );
        }

        Toast.makeText(
                context,
                active ? "Meeting activated" : "Meeting deactivated",
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        AlarmManager manager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        return manager != null && manager.canScheduleExactAlarms();
    }

    private void showPermissionNeededDialogForAlarms() {
        new AlertDialog.Builder(context)
                .setTitle("Permission Required for Reminders")
                .setMessage(
                        "Allow Alarms & reminders permission to receive " +
                                "meeting reminders at the scheduled time."
                )
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        intent = new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        );
                    } else {
                        intent = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        );
                    }

                    intent.setData(
                            Uri.parse("package:" + context.getPackageName())
                    );

                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to open alarm settings", e);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleMeetingActivation(Meeting meeting) {

        long startMillis = parseDateTime(
                meeting.getDate(),
                meeting.getStartTime()
        );

        if (startMillis < 0) {
            Toast.makeText(
                    context,
                    "Invalid meeting date or time",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(startMillis);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        if (startCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Log.d(TAG, "Meeting start time has already passed");
            return;
        }

        AlarmHelper.scheduleReminderForMeeting(
                context,
                meeting,
                startCalendar
        );

        if (meeting.getRecurrenceType() != 0) {
            Calendar endCalendar = calculateMeetingEndCalendar(meeting);

            if (endCalendar != null
                    && endCalendar.getTimeInMillis()
                    > System.currentTimeMillis()) {

                MeetingScheduler.scheduleImmediateRollover(
                        context,
                        endCalendar,
                        meeting.getId()
                );
            }
        }
    }

    private Calendar calculateMeetingEndCalendar(Meeting meeting) {

        long start = parseDateTime(
                meeting.getDate(),
                meeting.getStartTime()
        );

        long end = parseDateTime(
                meeting.getDate(),
                meeting.getEndTime()
        );

        if (start < 0 || end < 0) {
            return null;
        }

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(end);

        if (end < start) {
            endCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return endCalendar;
    }

    private void deleteMeeting(Meeting meeting) {

        DatabaseHelper db = new DatabaseHelper(context);
        boolean deleted;

        try {
            deleted = db.deleteMeeting(meeting.getId());
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

        AlarmHelper.cancelAllAlarmsForMeeting(
                context,
                meeting.getId()
        );

        ImmediateMeetingRolloverWorker
                .cancelExistingRolloverTrigger(
                        context,
                        meeting.getId()
                );

        int position = findMeetingPosition(meeting.getId());

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

    private int findMeetingPosition(int meetingId) {
        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getId() == meetingId) {
                return i;
            }
        }
        return -1;
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

    public static class MeetingViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final TextView description;
        final TextView date;
        final TextView startTime;
        final TextView endTime;
        final TextView status;
        final TextView reccurring;

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.meetingTitle);
            description = itemView.findViewById(R.id.description);
            date = itemView.findViewById(R.id.date);
            startTime = itemView.findViewById(R.id.startTime);
            endTime = itemView.findViewById(R.id.endTime);
            status = itemView.findViewById(R.id.status);
            reccurring = itemView.findViewById(R.id.reccurring);
        }
    }
}
