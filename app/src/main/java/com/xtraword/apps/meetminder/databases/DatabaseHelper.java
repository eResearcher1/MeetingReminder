package com.xtraword.apps.meetminder.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_RECURRENCE = "recurrence";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_TITLE = "title";
    private static final String CREATE_TABLE_MEETINGS = "CREATE TABLE meetings (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, date TEXT, start_time TEXT, end_time TEXT, recurrence INTEGER, is_active INTEGER DEFAULT 1)";
    private static final String DATABASE_NAME = "meetings.db";
    private static final int DATABASE_VERSION = 12;
    public static final String TABLE_MEETINGS = "meetings";
    private static final String TAG = "DatabaseHelper";
    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 12);
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MEETINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ". Old data will be destroyed.");
        db.execSQL("DROP TABLE IF EXISTS meetings");
        onCreate(db);
    }

    public long addMeeting(String title, String description, String date, String startTime, String endTime, int recurrenceType, int isActive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_START_TIME, startTime);
        values.put(COLUMN_END_TIME, endTime);
        values.put(COLUMN_RECURRENCE, Integer.valueOf(recurrenceType));
        values.put(COLUMN_IS_ACTIVE, Integer.valueOf(isActive));
        long id = -1;
        try {
            id = db.insert(TABLE_MEETINGS, null, values);
            Log.d(TAG, "Inserted meeting with ID: " + id + ", recurrence: " + recurrenceType);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting meeting", e);
            return id;
        }
    }

    public int updateMeeting(int id, String title, String description, String date, String startTime, String endTime, int recurrenceType, int isActive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_START_TIME, startTime);
        values.put(COLUMN_END_TIME, endTime);
        values.put(COLUMN_RECURRENCE, Integer.valueOf(recurrenceType));
        values.put(COLUMN_IS_ACTIVE, Integer.valueOf(isActive));
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_MEETINGS, values, "_id=?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Updated meeting ID: " + id + ", rows affected: " + rowsAffected);
            return rowsAffected;
        } catch (Exception e) {
            Log.e(TAG, "Error updating meeting ID: " + id, e);
            return rowsAffected;
        }
    }

    public boolean updateMeetingDateAndTimes(int id, String newDate, String newStartTime, String newEndTime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, newDate);
        values.put(COLUMN_START_TIME, newStartTime);
        values.put(COLUMN_END_TIME, newEndTime);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_MEETINGS, values, "_id=?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                Log.d(TAG, "Updated meeting date/time for ID=" + id + " date=" + newDate + " start=" + newStartTime + " end=" + newEndTime);
            } else {
                Log.w(TAG, "No rows affected while updating meeting date/time for ID=" + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating meeting date/time for ID=" + id, e);
        }
        return rowsAffected > 0;
    }

    public Meeting getMeetingById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        Meeting meeting = null;
        try {
            try {
                cursor = db.query(TABLE_MEETINGS, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    meeting = cursorToMeeting(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting meeting by ID: " + id, e);
            }
            return meeting;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Meeting> getAllMeetings() {
        List<Meeting> meetings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            try {
                cursor = db.query(TABLE_MEETINGS, null, null, null, null, null, "date ASC, start_time ASC");
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        meetings.add(cursorToMeeting(cursor));
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting all meetings", e);
            }
            return meetings;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean deleteMeeting(int meetingId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            int rowsDeleted = db.delete(TABLE_MEETINGS, "_id=?", new String[]{String.valueOf(meetingId)});
            Log.d(TAG, "Rows deleted: " + rowsDeleted + " for meeting ID: " + meetingId);
            if (rowsDeleted > 0 && this.mContext != null) {
                AlarmHelper.cancelAllAlarmsForMeeting(this.mContext, meetingId);
                MeetingScheduler.cancelImmediateRollover(this.mContext, meetingId);
                Log.d(TAG, "Cancelled alarm + rollover for deleted meeting ID: " + meetingId);
            }
            return rowsDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting meeting ID: " + meetingId, e);
            return false;
        }
    }

    public List<Meeting> searchMeetings(String query) {
        List<Meeting> meetings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            try {
                String[] selectionArgs = {"%" + query + "%"};
                cursor = db.query(TABLE_MEETINGS, null, "title LIKE ?", selectionArgs, null, null, "date ASC, start_time ASC");
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        meetings.add(cursorToMeeting(cursor));
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching meetings for query: " + query, e);
            }
            return meetings;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Meeting cursorToMeeting(Cursor cursor) {
        Meeting meeting = new Meeting();
        meeting.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
        meeting.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        meeting.setDescription(cursor.getString(cursor.getColumnIndex("description")));
        meeting.setDate(cursor.getString(cursor.getColumnIndex(COLUMN_DATE)));
        meeting.setStartTime(cursor.getString(cursor.getColumnIndex(COLUMN_START_TIME)));
        meeting.setEndTime(cursor.getString(cursor.getColumnIndex(COLUMN_END_TIME)));
        meeting.setRecurrenceType(cursor.getInt(cursor.getColumnIndex(COLUMN_RECURRENCE)));
        meeting.setIsActive(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ACTIVE)));
        return meeting;
    }

    public boolean updateMeetingActiveStatus(int id, int isActive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_ACTIVE, Integer.valueOf(isActive));
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_MEETINGS, values, "_id=?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Updated active status for meeting ID: " + id + " to " + isActive + ". Rows affected: " + rowsAffected);
            if (rowsAffected > 0 && this.mContext != null && isActive == 0) {
                AlarmHelper.cancelAllAlarmsForMeeting(this.mContext, id);
                MeetingScheduler.cancelImmediateRollover(this.mContext, id);
                Log.d(TAG, "Cancelled alarm + rollover because meeting was made inactive. ID=" + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating active status for meeting ID: " + id, e);
        }
        return rowsAffected > 0;
    }

    public boolean updateMeetingDate(int id, String newDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, newDate);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_MEETINGS, values, "_id=?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                Log.d(TAG, "Successfully updated date for meeting ID: " + id + " to " + newDate);
            } else {
                Log.w(TAG, "Failed to update date for meeting ID: " + id + ". New date: " + newDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating date for meeting ID: " + id + " to " + newDate, e);
        }
        return rowsAffected > 0;
    }

    public List<Meeting> getAllActiveMeetings() {
        List<Meeting> meetings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String nowKey = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        String[] selectionArgs = {"1", nowKey};
        try {
            cursor = db.query(TABLE_MEETINGS, null, "is_active=? AND (date || ' ' || start_time) >= ?", selectionArgs, null, null, "date ASC, start_time ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    meetings.add(cursorToMeeting(cursor));
                } while (cursor.moveToNext());
            }
            Log.d(TAG, "getAllActiveMeetings: found " + meetings.size() + " active future meetings.");
        } catch (Exception e) {
            Log.e(TAG, "Error getting active meetings", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return meetings;
    }

    public List<String> getTodaysPendingMeetingLines() {
        List<String> lines = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String nowTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String[] columns = {"title", COLUMN_START_TIME};
        String[] selectionArgs = {"1", today, nowTime};
        try {
            try {
                cursor = db.query(TABLE_MEETINGS, columns, "is_active=? AND date=? AND start_time>=?", selectionArgs, null, null, "start_time ASC");
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                        String start = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME));
                        if (title == null) {
                            title = "";
                        }
                        if (start == null) {
                            start = "";
                        }
                        lines.add(start + " - " + title.trim());
                    } while (cursor.moveToNext());
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting today's pending meeting lines", e);
                if (cursor != null) {
                }
            }
            if (lines.isEmpty()) {
                lines.add("You have no pending meetings for today.");
            }
            return lines;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }
}
