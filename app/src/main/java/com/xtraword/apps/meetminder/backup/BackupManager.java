package com.xtraword.apps.meetminder.backup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelperAlarms;
import com.xtraword.apps.meetminder.worker.MeetingScheduler;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.alarms.BootReceiverMM;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Versioned, portable backup; never exports billing entitlements. */
public final class BackupManager {
    private static final String TAG = "MeetMinderBackup";
    private static final int VERSION = 1;
    private static final int MAX_JSON_BYTES = 20 * 1024 * 1024;
    private BackupManager() { }

    public static void exportTo(Context context, Uri uri) throws Exception {
        JSONObject data = snapshot(context);
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new Exception("Cannot open backup destination");
            writeZip(stream, data);
        }
    }

    private static void writeZip(OutputStream output, JSONObject data) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(output);
        zip.putNextEntry(new ZipEntry("meetminder.json"));
        zip.write(data.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.finish();
    }

    private static JSONObject snapshot(Context context) throws Exception {
        JSONObject data = new JSONObject();
        data.put("format", "MeetMinderBackup");
        data.put("version", VERSION);
        try (DatabaseHelper meetings = new DatabaseHelper(context);
             DatabaseHelperAlarms alarms = new DatabaseHelperAlarms(context)) {
            data.put("meetings", readTable(meetings.getReadableDatabase(), "meetings"));
            data.put("alarms", readTable(alarms.getReadableDatabase(), "alarms"));
        }
        String tone = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                .getString("notification_tone", null);
        if (tone != null) data.put("notification_tone", tone);
        return data;
    }

    private static JSONArray readTable(SQLiteDatabase db, String table) throws Exception {
        JSONArray rows = new JSONArray();
        try (Cursor cursor = db.query(table, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String column = cursor.getColumnName(i);
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER: row.put(column, cursor.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: row.put(column, cursor.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_STRING: row.put(column, cursor.getString(i)); break;
                        case Cursor.FIELD_TYPE_NULL: row.put(column, JSONObject.NULL); break;
                        default: throw new Exception("Unsupported column type");
                    }
                }
                rows.put(row);
            }
        }
        return rows;
    }

    public static int restoreFrom(Context context, Uri uri) throws Exception {
        JSONObject data;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new Exception("Cannot read backup");
            data = readZip(stream);
        }
        validate(data);
        // Local safety copy, retained if restoration fails.
        File safety = new File(context.getFilesDir(), "meetminder_before_restore.zip");
        try (FileOutputStream out = new FileOutputStream(safety)) { writeZip(out, snapshot(context)); }
        JSONArray meetings = data.getJSONArray("meetings");
        JSONArray alarms = data.getJSONArray("alarms");
        Set<Integer> oldIds = new HashSet<>();
        try (DatabaseHelper oldDb = new DatabaseHelper(context)) {
            for (Meeting old : oldDb.getAllMeetings()) oldIds.add(old.getId());
        }
        try (DatabaseHelper meetingHelper = new DatabaseHelper(context);
             DatabaseHelperAlarms alarmHelper = new DatabaseHelperAlarms(context)) {
            SQLiteDatabase mdb = meetingHelper.getWritableDatabase();
            SQLiteDatabase adb = alarmHelper.getWritableDatabase();
            // Validate everything before any database mutation.
            // Hold both transactions open until both tables have been inserted.
            mdb.beginTransaction();
            try {
                adb.beginTransaction();
                try {
                    mdb.delete("meetings", null, null);
                    insertRows(mdb, "meetings", meetings,
                            new String[]{"_id", "title", "description", "date", "start_time", "end_time", "recurrence", "is_active"});
                    adb.delete("alarms", null, null);
                    insertRows(adb, "alarms", alarms,
                            new String[]{"id", "meeting_id", "title", "description", "reminder_type", "meeting_time_millis", "reminder_time_millis"});
                    mdb.setTransactionSuccessful();
                    adb.setTransactionSuccessful();
                } finally { adb.endTransaction(); }
            } finally { mdb.endTransaction(); }
        }
        if (data.has("notification_tone")) {
            context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
                    .putString("notification_tone", data.getString("notification_tone")).commit();
        }
        // Clear alarms/jobs for meetings removed by the restore.
        for (Integer oldId : oldIds) {
            AlarmHelper.cancelAllAlarmsForMeeting(context, oldId);
            MeetingScheduler.cancelImmediateRollover(context, oldId);
        }
        BootReceiverMM.restoreSchedules(context);
        return meetings.length();
    }

    private static JSONObject readZip(InputStream input) throws Exception {
        ZipInputStream zip = new ZipInputStream(input);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (!"meetminder.json".equals(entry.getName())) continue;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = zip.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_JSON_BYTES) throw new Exception("Backup too large");
                bytes.write(buffer, 0, count);
            }
            return new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
        throw new Exception("Not a MeetMinder backup file");
    }

    private static void validate(JSONObject data) throws Exception {
        if (!"MeetMinderBackup".equals(data.optString("format")) || data.optInt("version") != VERSION)
            throw new Exception("Unsupported backup format/version");
        JSONArray meetings = data.getJSONArray("meetings");
        JSONArray alarms = data.getJSONArray("alarms");
        if (meetings.length() > 100000 || alarms.length() > 200000) throw new Exception("Too many records");
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < meetings.length(); i++) {
            JSONObject m = meetings.getJSONObject(i);
            int id = m.getInt("_id");
            if (id <= 0 || !ids.add(id)) throw new Exception("Invalid/duplicate meeting ID");
            for (String key : new String[]{"date", "start_time", "end_time"}) {
                if (m.isNull(key) || m.optString(key).isEmpty()) throw new Exception("Invalid meeting " + key);
            }
            if (AlarmHelper.parseMeetingDateTime(m.getString("date"), m.getString("start_time")) == null
                    || AlarmHelper.parseMeetingDateTime(m.getString("date"), m.getString("end_time")) == null)
                throw new Exception("Invalid meeting date/time");
            m.getInt("recurrence"); m.getInt("is_active");
        }
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject a = alarms.getJSONObject(i);
            a.getInt("id"); a.getInt("meeting_id");
            a.getLong("meeting_time_millis"); a.getLong("reminder_time_millis");
        }
    }

    private static void insertRows(SQLiteDatabase db, String table, JSONArray rows, String[] columns) throws Exception {
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            ContentValues values = new ContentValues();
            for (String col : columns) {
                if (!row.has(col) || row.isNull(col)) continue;
                Object value = row.get(col);
                if (value instanceof Integer || value instanceof Long) values.put(col, ((Number)value).longValue());
                else if (value instanceof Number) values.put(col, ((Number)value).doubleValue());
                else if (value instanceof String) values.put(col, (String)value);
                else throw new Exception("Invalid field " + col);
            }
            if (db.insertOrThrow(table, null, values) < 0) throw new Exception("Insert failed");
        }
    }
}
