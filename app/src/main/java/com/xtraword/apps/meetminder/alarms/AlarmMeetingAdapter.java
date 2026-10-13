
package com.xtraword.apps.meetminder.alarms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xtraword.apps.meetminder.R;

import java.util.Collections;
import java.util.List;

public class AlarmMeetingAdapter
        extends RecyclerView.Adapter<AlarmMeetingAdapter.AlarmMeetingViewHolder> {

    private final LayoutInflater inflater;
    private final List<MeetingAlarm> meetingAlarms;

    public AlarmMeetingAdapter(
            Context context,
            List<MeetingAlarm> meetingAlarms) {

        this.inflater = LayoutInflater.from(context);

        this.meetingAlarms = meetingAlarms != null
                ? meetingAlarms
                : Collections.emptyList();
    }

    @NonNull
    @Override
    public AlarmMeetingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = inflater.inflate(
                R.layout.item_alarm_meeting,
                parent,
                false
        );

        return new AlarmMeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlarmMeetingViewHolder holder,
            int position) {

        MeetingAlarm meetingAlarm = meetingAlarms.get(position);

        holder.textViewTitle.setText(
                meetingAlarm.getTitle()
        );

        holder.textViewRecurrence.setText(
                "Recurrence: " + meetingAlarm.getRecurrence()
        );

        holder.textViewRecurrenceDate.setText(
                "Recurrence Date: " + meetingAlarm.getRecurrenceDate()
        );

        holder.textViewAlarmStatus.setText(
                meetingAlarm.isAlarmActive()
                        ? "Alarm Active"
                        : "Alarm Inactive"
        );
    }

    @Override
    public int getItemCount() {
        return meetingAlarms.size();
    }

    public static class AlarmMeetingViewHolder
            extends RecyclerView.ViewHolder {

        final TextView textViewTitle;
        final TextView textViewRecurrence;
        final TextView textViewRecurrenceDate;
        final TextView textViewAlarmStatus;

        public AlarmMeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewTitle =
                    itemView.findViewById(R.id.textViewTitle);

            textViewRecurrence =
                    itemView.findViewById(R.id.textViewRecurrence);

            textViewRecurrenceDate =
                    itemView.findViewById(R.id.textViewRecurrenceDate);

            textViewAlarmStatus =
                    itemView.findViewById(R.id.textViewAlarmStatus);
        }
    }
}
