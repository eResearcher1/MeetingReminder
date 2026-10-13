package com.xtraword.apps.meetminder;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Meeting {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final int RECURRENCE_DAILY = 1;
    public static final int RECURRENCE_MONTHLY = 3;
    public static final int RECURRENCE_NONE = 0;
    public static final int RECURRENCE_WEEKLY = 2;
    public static final int RECURRENCE_YEARLY = 4;
    private String date;
    private String description;
    private String endTime;
    private int id;
    private int isActive;
    private boolean isChecked = false;
    private int parentMeetingId;
    private int recurrenceType;
    private String startTime;
    private String title;

    public Meeting() {
    }

    public Meeting(int id, String title, String description, String date, String startTime, String endTime, int recurrenceType, int isActive) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.recurrenceType = recurrenceType;
        this.isActive = isActive;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getRecurrence() {
        return this.recurrenceType;
    }

    public void setRecurrence(int recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public int getIsActive() {
        return this.isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public String getSpinner() {
        switch (this.recurrenceType) {
            case 0:
                return "Once";
            case 1:
                return "Daily";
            case 2:
                return "Weekly";
            case 3:
                return "Monthly";
            case 4:
                return "Yearly";
            default:
                return "Unknown";
        }
    }

    public boolean isActive() {
        return this.isActive == 1;
    }

    public void setActive(boolean z) {
        this.isActive = z ? 1 : 0;
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public int getRecurrenceType() {
        return this.recurrenceType;
    }

    public void setRecurrenceType(int recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public int getParentMeetingId() {
        return this.parentMeetingId;
    }

    public void setParentMeetingId(int parentMeetingId) {
        this.parentMeetingId = parentMeetingId;
    }

    public String getOriginalStartTimeForRollover() {
        return this.startTime;
    }

    public String getOriginalEndTimeForRollover() {
        return this.endTime;
    }
}
