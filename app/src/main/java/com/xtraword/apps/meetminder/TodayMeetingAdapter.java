package com.xtraword.apps.meetminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TodayMeetingAdapter extends RecyclerView.Adapter<TodayMeetingAdapter.MeetingViewHolder> {
    private List<Meeting> meetings;

    public TodayMeetingAdapter(List<Meeting> meetings) {
        this.meetings = meetings;
    }

    @Override
    public MeetingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_today_meeting, parent, false);
        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MeetingViewHolder holder, int position) {
        Meeting meeting = this.meetings.get(position);
        holder.titleTextView.setText(meeting.getTitle());
        holder.timeTextView.setText(meeting.getStartTime());
    }

    @Override
    public int getItemCount() {
        return this.meetings.size();
    }

    static class MeetingViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView titleTextView;

        MeetingViewHolder(View itemView) {
            super(itemView);
            this.titleTextView = (TextView) itemView.findViewById(R.id.meetingTitle);
            this.timeTextView = (TextView) itemView.findViewById(R.id.meetingTime);
        }
    }
}
