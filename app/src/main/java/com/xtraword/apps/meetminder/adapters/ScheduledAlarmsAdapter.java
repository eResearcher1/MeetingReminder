
package com.xtraword.apps.meetminder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.databases.DatabaseHelperAlarms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduledAlarmsAdapter
        extends RecyclerView.Adapter<ScheduledAlarmsAdapter.AlarmViewHolder> {

    private List<DatabaseHelperAlarms.AlarmModel> alarms;

    private final SimpleDateFormat dateTimeFormatter;
    private final LayoutInflater inflater;

    public ScheduledAlarmsAdapter(
            Context context,
            List<DatabaseHelperAlarms.AlarmModel> alarms) {

        this.inflater = LayoutInflater.from(context);

        this.alarms = alarms != null
                ? new ArrayList<>(alarms)
                : new ArrayList<>();

        this.dateTimeFormatter = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        );

        this.dateTimeFormatter.setTimeZone(
                TimeZone.getDefault()
        );
    }

    // --------------------------------------------------
    // UPDATE ALARM LIST
    // --------------------------------------------------

    public void updateData(
            List<DatabaseHelperAlarms.AlarmModel> newAlarms) {

        this.alarms = newAlarms != null
                ? new ArrayList<>(newAlarms)
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    // --------------------------------------------------
    // CREATE VIEW HOLDER
    // --------------------------------------------------

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View itemView = inflater.inflate(
                R.layout.item_scheduled_alarm,
                parent,
                false
        );

        return new AlarmViewHolder(itemView);
    }

    // --------------------------------------------------
    // BIND ALARM DATA
    // --------------------------------------------------

    @Override
    public void onBindViewHolder(
            @NonNull AlarmViewHolder holder,
            int position) {

        DatabaseHelperAlarms.AlarmModel alarm =
                alarms.get(position);

        holder.tvAlarmId.setText(
                "ID: " + alarm.getId()
        );

        holder.tvMeetingId.setText(
                "Meeting ID: " + alarm.getMeetingId()
        );

        holder.tvTitle.setText(
                alarm.getTitle() != null
                        ? alarm.getTitle()
                        : "Untitled Meeting"
        );

        holder.tvDescription.setText(
                alarm.getDescription() != null
                        ? alarm.getDescription()
                        : "No description"
        );

        holder.tvReminderType.setText(
                "Type: " + alarm.getReminderType()
        );

        holder.tvMeetingTime.setText(
                "Meeting Time: " +
                        formatMillis(
                                alarm.getMeetingTimeMillis()
                        )
        );

        holder.tvReminderTime.setText(
                "Reminder Time: " +
                        formatMillis(
                                alarm.getReminderTimeMillis()
                        )
        );
    }

    // --------------------------------------------------
    // FORMAT DATE AND TIME
    // --------------------------------------------------

    private String formatMillis(long millis) {

        if (millis <= 0) {
            return "N/A";
        }

        return dateTimeFormatter.format(
                new Date(millis)
        );
    }

    // --------------------------------------------------
    // ITEM COUNT
    // --------------------------------------------------

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    // --------------------------------------------------
    // VIEW HOLDER
    // --------------------------------------------------

    public static class AlarmViewHolder
            extends RecyclerView.ViewHolder {

        final TextView tvAlarmId;
        final TextView tvMeetingId;
        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvReminderType;
        final TextView tvMeetingTime;
        final TextView tvReminderTime;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAlarmId = itemView.findViewById(
                    R.id.tvAlarmId
            );

            tvMeetingId = itemView.findViewById(
                    R.id.tvMeetingId
            );

            tvTitle = itemView.findViewById(
                    R.id.tvTitle
            );

            tvDescription = itemView.findViewById(
                    R.id.tvDescription
            );

            tvReminderType = itemView.findViewById(
                    R.id.tvReminderType
            );

            tvMeetingTime = itemView.findViewById(
                    R.id.tvMeetingTime
            );

            tvReminderTime = itemView.findViewById(
                    R.id.tvReminderTime
            );
        }
    }
}
