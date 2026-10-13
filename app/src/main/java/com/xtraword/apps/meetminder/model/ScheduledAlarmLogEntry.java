package com.xtraword.apps.meetminder.model;

public class ScheduledAlarmLogEntry {
    private String description;
    private long id;
    private String meetingId;
    private long meetingTimeMillis;
    private long reminderTimeMillis;
    private String reminderType;
    private String title;

    public ScheduledAlarmLogEntry(long id, String meetingId, String title, String description, String reminderType, long meetingTimeMillis, long reminderTimeMillis) {
        this.id = id;
        this.meetingId = meetingId;
        this.title = title;
        this.description = description;
        this.reminderType = reminderType;
        this.meetingTimeMillis = meetingTimeMillis;
        this.reminderTimeMillis = reminderTimeMillis;
    }

    public long getId() {
        return this.id;
    }

    public String getMeetingId() {
        return this.meetingId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getReminderType() {
        return this.reminderType;
    }

    public long getMeetingTimeMillis() {
        return this.meetingTimeMillis;
    }

    public long getReminderTimeMillis() {
        return this.reminderTimeMillis;
    }
}
