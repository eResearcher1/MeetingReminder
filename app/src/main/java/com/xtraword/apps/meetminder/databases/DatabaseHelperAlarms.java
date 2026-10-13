package com.xtraword.apps.meetminder.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelperAlarms extends SQLiteOpenHelper {
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MEETING_ID = "meeting_id";
    public static final String COLUMN_MEETING_TIME_MILLIS = "meeting_time_millis";
    public static final String COLUMN_REMINDER_TIME_MILLIS = "reminder_time_millis";
    public static final String COLUMN_REMINDER_TYPE = "reminder_type";
    public static final String COLUMN_TITLE = "title";
    private static final String CREATE_TABLE_ALARMS = "CREATE TABLE alarms (id INTEGER PRIMARY KEY AUTOINCREMENT, meeting_id INTEGER, title TEXT, description TEXT, reminder_type TEXT, meeting_time_millis INTEGER, reminder_time_millis INTEGER);";
    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_ALARMS = "alarms";

    public DatabaseHelperAlarms(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ALARMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS alarms");
        onCreate(db);
    }

    public long insertAlarm(int meetingId, String title, String description, String reminderType, long meetingTimeMillis, long reminderTimeMillis) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("meeting_id", Integer.valueOf(meetingId));
        values.put("title", title);
        values.put("description", description);
        values.put("reminder_type", reminderType);
        values.put(COLUMN_MEETING_TIME_MILLIS, Long.valueOf(meetingTimeMillis));
        values.put(COLUMN_REMINDER_TIME_MILLIS, Long.valueOf(reminderTimeMillis));
        return db.insert(TABLE_ALARMS, null, values);
    }

    public List<AlarmModel> getAllAlarms() {
        List<AlarmModel> alarms = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ALARMS, null, null, null, null, null, "reminder_time_millis ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                alarms.add(cursorToAlarm(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return alarms;
    }

    public List<AlarmModel> getAlarmsByMeetingId(int meetingId) {
        List<AlarmModel> alarms = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ALARMS, null, "meeting_id=?", new String[]{String.valueOf(meetingId)}, null, null, "reminder_time_millis ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                alarms.add(cursorToAlarm(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return alarms;
    }

    public int deleteAlarm(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_ALARMS, "id=?", new String[]{String.valueOf(id)});
    }

    public int deleteAlarmsByMeetingId(int meetingId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_ALARMS, "meeting_id=?", new String[]{String.valueOf(meetingId)});
    }

    public void deleteAllAlarms() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ALARMS, null, null);
    }

    private AlarmModel cursorToAlarm(Cursor cursor) {
        AlarmModel alarm = new AlarmModel();
        alarm.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        alarm.setMeetingId(cursor.getInt(cursor.getColumnIndexOrThrow("meeting_id")));
        alarm.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        alarm.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        alarm.setReminderType(cursor.getString(cursor.getColumnIndexOrThrow("reminder_type")));
        alarm.setMeetingTimeMillis(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MEETING_TIME_MILLIS)));
        alarm.setReminderTimeMillis(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME_MILLIS)));
        return alarm;
    }

    public static class AlarmModel {
        private String description;
        private int id;
        private int meetingId;
        private long meetingTimeMillis;
        private long reminderTimeMillis;
        private String reminderType;
        private String title;

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getMeetingId() {
            return this.meetingId;
        }

        public void setMeetingId(int meetingId) {
            this.meetingId = meetingId;
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

        public String getReminderType() {
            return this.reminderType;
        }

        public void setReminderType(String reminderType) {
            this.reminderType = reminderType;
        }

        public long getMeetingTimeMillis() {
            return this.meetingTimeMillis;
        }

        public void setMeetingTimeMillis(long meetingTimeMillis) {
            this.meetingTimeMillis = meetingTimeMillis;
        }

        public long getReminderTimeMillis() {
            return this.reminderTimeMillis;
        }

        public void setReminderTimeMillis(long reminderTimeMillis) {
            this.reminderTimeMillis = reminderTimeMillis;
        }
    }
}
