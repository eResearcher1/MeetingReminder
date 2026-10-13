package com.xtraword.apps.meetminder.alarms;

public class MeetingAlarm {
    private boolean alarmActive;
    private String recurrence;
    private String recurrenceDate;
    private String title;

    public MeetingAlarm(String title, String recurrence, String recurrenceDate, boolean alarmActive) {
        this.title = title;
        this.recurrence = recurrence;
        this.recurrenceDate = recurrenceDate;
        this.alarmActive = alarmActive;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRecurrence() {
        return this.recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public String getRecurrenceDate() {
        return this.recurrenceDate;
    }

    public void setRecurrenceDate(String recurrenceDate) {
        this.recurrenceDate = recurrenceDate;
    }

    public boolean isAlarmActive() {
        return this.alarmActive;
    }

    public void setAlarmActive(boolean alarmActive) {
        this.alarmActive = alarmActive;
    }
}
