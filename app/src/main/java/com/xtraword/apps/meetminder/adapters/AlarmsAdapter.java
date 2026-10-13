
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmsAdapter
        extends RecyclerView.Adapter<AlarmsAdapter.AlarmViewHolder> {

    private final Context context;
    private final List<DatabaseHelperAlarms.AlarmModel> alarms;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
            );

    public AlarmsAdapter(
            Context context,
            List<DatabaseHelperAlarms.AlarmModel> alarms) {

        this.context = context;
        this.alarms = alarms;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_alarm,
                        parent,
                        false
                );

        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlarmViewHolder holder,
            int position) {

        DatabaseHelperAlarms.AlarmModel alarm =
                alarms.get(position);

        holder.textMeetingId.setText(
                "Meeting ID: " + alarm.getMeetingId()
        );

        holder.textTitle.setText(
                "Title: " + alarm.getTitle()
        );

        holder.textDescription.setText(
                "Description: " + alarm.getDescription()
        );

        holder.textReminderType.setText(
                "Reminder Type: " + alarm.getReminderType()
        );

        holder.textMeetingTime.setText(
                "Meeting Time: " +
                        dateFormat.format(
                                new Date(alarm.getMeetingTimeMillis())
                        )
        );

        holder.textReminderTime.setText(
                "Reminder Time: " +
                        dateFormat.format(
                                new Date(alarm.getReminderTimeMillis())
                        )
        );
    }

    @Override
    public int getItemCount() {

        return alarms != null
                ? alarms.size()
                : 0;
    }

    // --------------------------------------------------
    // VIEW HOLDER
    // --------------------------------------------------

    public static class AlarmViewHolder
            extends RecyclerView.ViewHolder {

        final TextView textMeetingId;
        final TextView textTitle;
        final TextView textDescription;
        final TextView textReminderType;
        final TextView textMeetingTime;
        final TextView textReminderTime;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);

            textMeetingId =
                    itemView.findViewById(R.id.textMeetingId);

            textTitle =
                    itemView.findViewById(R.id.textTitle);

            textDescription =
                    itemView.findViewById(R.id.textDescription);

            textReminderType =
                    itemView.findViewById(R.id.textReminderType);

            textMeetingTime =
                    itemView.findViewById(R.id.textMeetingTime);

            textReminderTime =
                    itemView.findViewById(R.id.textReminderTime);
        }
    }
}
