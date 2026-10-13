package com.xtraword.apps.meetminder.manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Meeting {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private String date;
    private String description;
    private String endTime;
    private int id;
    private int isActive;
    private boolean isChecked;
    private String nextRecurrenceDate;
    private int recurrenceId;
    private int recurrenceType;
    private String startTime;
    private String title;

    public Meeting(int id, String title, String description, String date, String startTime, String endTime, int recurrenceType, String nextRecurrenceDate, int isActive, int recurrenceId, boolean isChecked) {
        this.isChecked = false;
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.recurrenceType = recurrenceType;
        this.nextRecurrenceDate = nextRecurrenceDate;
        this.isActive = isActive;
        this.recurrenceId = recurrenceId;
        this.isChecked = isChecked();
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

    public void setRecurrence(int recurrence) {
        this.recurrenceType = this.recurrenceType;
    }

    public String getNextRecurrenceDate() {
        return this.nextRecurrenceDate;
    }

    public void setNextRecurrenceDate(String nextRecurrenceDate) {
        this.nextRecurrenceDate = nextRecurrenceDate;
    }

    public int getIsActive() {
        return this.isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public Date getNextRecurrenceDateAsDate() throws ParseException {
        return DATE_FORMAT.parse(this.nextRecurrenceDate);
    }

    public void setNextRecurrenceDateFromDate(Date date) {
        this.nextRecurrenceDate = DATE_FORMAT.format(date);
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
            default:
                return "None";
        }
    }

    public boolean isActive() {
        return this.isActive == 1;
    }

    public void setActive(boolean z) {
        this.isActive = z ? 1 : 0;
    }

    public int getRecurrenceId() {
        return this.recurrenceId;
    }

    public void setRecurrenceId(int recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
}
