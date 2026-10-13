
package com.xtraword.apps.meetminder.search;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xtraword.apps.meetminder.EditMeetingActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;

import java.util.ArrayList;
import java.util.List;

public class MeetingAdapter
        extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {

    private final Context context;
    private final List<Meeting> meetings;
    private final OnMeetingClickListener onMeetingClickListener;

    public interface OnMeetingClickListener {
        void onMeetingClick(Meeting meeting);
    }

    public MeetingAdapter(
            Context context,
            List<Meeting> meetings,
            OnMeetingClickListener listener) {

        this.context = context;
        this.meetings = new ArrayList<>();

        if (meetings != null) {
            this.meetings.addAll(meetings);
        }

        this.onMeetingClickListener = listener;
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_search,
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

        holder.titleTextView.setText(meeting.getTitle());

        holder.itemView.setOnClickListener(v -> {

            if (onMeetingClickListener != null) {
                onMeetingClickListener.onMeetingClick(meeting);
            } else {
                openMeetingForEditing(meeting);
            }
        });
    }

    private void openMeetingForEditing(Meeting meeting) {

        Intent intent = new Intent(
                context,
                EditMeetingActivity.class
        );

        intent.putExtra("meetingId", meeting.getId());

        context.startActivity(intent);
    }

    public void updateData(List<Meeting> newMeetings) {

        meetings.clear();

        if (newMeetings != null) {
            meetings.addAll(newMeetings);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public static class MeetingViewHolder
            extends RecyclerView.ViewHolder {

        final TextView titleTextView;

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(
                    R.id.meeting_title
            );
        }
    }
}
