package com.xtraword.apps.meetminder.foreground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.alarms.AlarmHelper;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;

public class ReminderService extends Service {
    public static final String ACTION_DISMISS = "com.xtraword.apps.meetminder.action.DISMISS";
    public static final String ACTION_SNOOZE_10 = "com.xtraword.apps.meetminder.action.SNOOZE_10";
    public static final String ACTION_START_RINGING = "com.xtraword.apps.meetminder.action.START_RINGING";
    public static final String CHANNEL_ID = "reminder_foreground_channel";
    private static final int FG_NOTIFICATION_ID = 99001;
    private static final String KEY_NOTIFICATION_TONE = "notification_tone";
    private static final String PREFS_APP_SETTINGS = "AppPreferences";
    private AudioManager.OnAudioFocusChangeListener afListener;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager audioManager;
    private Runnable autoStopRunnable;
    private MediaPlayer player;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler serviceHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createForegroundChannel();
        this.audioManager = (AudioManager) getSystemService("audio");
        this.afListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public final void onAudioFocusChange(int i) {
                ReminderService.this.m320x545b171(i);
            }
        };
    }

    void m320x545b171(int focusChange) {
        if (focusChange == -1 || focusChange == -2) {
            stopRingingAndStopSelf("audio_focus_lost");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopRingingAndStopSelf("null_intent_or_action");
            return 2;
        }
        String action = intent.getAction();
        int meetingId = intent.getIntExtra("meeting_id", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);
        String title = intent.getStringExtra("title");
        if (ACTION_START_RINGING.equals(action)) {
            Notification fg = buildForegroundNotification(title);
            startForeground(FG_NOTIFICATION_ID, fg);
            startRingingWithUri(getAlarmSoundUri());
            scheduleAutoStop(60000L);
            return 2;
        }
        if (ACTION_DISMISS.equals(action)) {
            cancelAlarmNotification(notificationId);
            stopRingingAndStopSelf("dismiss");
            return 2;
        }
        if (ACTION_SNOOZE_10.equals(action)) {
            boolean ok = scheduleSnooze10(meetingId);
            cancelAlarmNotification(notificationId);
            toast(ok ? "Snoozed 10 minutes" : "Snooze failed");
            stopRingingAndStopSelf("snooze_10");
            return 2;
        }
        stopRingingAndStopSelf("unknown_action_" + action);
        return 2;
    }

    private void cancelAlarmNotification(int notificationId) {
        NotificationManager nm;
        if (notificationId != -1 && (nm = (NotificationManager) getSystemService("notification")) != null) {
            nm.cancel(notificationId);
        }
    }

    private void toast(final String msg) {
        this.mainHandler.post(new Runnable() {
            @Override
            public final void run() {
                ReminderService.this.m324xe26c7f04(msg);
            }
        });
    }

    void m324xe26c7f04(String msg) {
        Toast.makeText(getApplicationContext(), msg, 0).show();
    }

    private Notification buildForegroundNotification(String title) {
        String t = (title == null || title.trim().isEmpty()) ? "Meeting Reminder" : title;
        return new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle("Reminder active").setContentText("Ringing: " + t).setOngoing(true).setSilent(true).build();
    }

    private void startRingingWithUri(Uri uri) {
        try {
            requestAudioFocus();
            stopPlayerIfRunning();
            this.player = new MediaPlayer();
            this.player.setAudioAttributes(new AudioAttributes.Builder().setUsage(4).setContentType(4).build());
            this.player.setLooping(true);
            this.player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public final void onPrepared(MediaPlayer mediaPlayer) {
                    ReminderService.this.m322x11695f94(mediaPlayer);
                }
            });
            this.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public final boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                    return ReminderService.this.m323x36fd6895(mediaPlayer, i, i2);
                }
            });
            this.player.setDataSource(this, uri);
            this.player.prepareAsync();
        } catch (Exception e) {
            stopRingingAndStopSelf("ring_start_failed");
        }
    }

    void m322x11695f94(MediaPlayer mp) {
        try {
            mp.start();
        } catch (Exception e) {
            stopRingingAndStopSelf("player_start_failed");
        }
    }

    boolean m323x36fd6895(MediaPlayer mp, int what, int extra) {
        stopRingingAndStopSelf("player_error_" + what + "_" + extra);
        return true;
    }

    private Uri getAlarmSoundUri() {
        try {
            SharedPreferences preferences = getSharedPreferences(PREFS_APP_SETTINGS, 0);
            String def = RingtoneManager.getDefaultUri(4).toString();
            String s = preferences.getString(KEY_NOTIFICATION_TONE, def);
            Uri uri = Uri.parse(s);
            return uri != null ? uri : RingtoneManager.getDefaultUri(4);
        } catch (Exception e) {
            Uri uri2 = RingtoneManager.getDefaultUri(4);
            if (uri2 == null) {
                return RingtoneManager.getDefaultUri(2);
            }
            return uri2;
        }
    }

    private void scheduleAutoStop(long ms) {
        if (this.autoStopRunnable != null) {
            this.serviceHandler.removeCallbacks(this.autoStopRunnable);
        }
        this.autoStopRunnable = new Runnable() {
            @Override
            public final void run() {
                ReminderService.this.m321x61898082();
            }
        };
        this.serviceHandler.postDelayed(this.autoStopRunnable, ms);
    }

    void m321x61898082() {
        stopRingingAndStopSelf("auto_stop");
    }

    private boolean scheduleSnooze10(int meetingId) {
        if (meetingId == -1) {
            return false;
        }
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            Meeting meeting = dbHelper.getMeetingById(meetingId);
            if (meeting == null || meeting.getIsActive() != 1) {
                return false;
            }
            AlarmHelper.scheduleSnoozeAlarm(getApplicationContext(), meeting);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopRingingAndStopSelf(String reason) {
        try {
            if (this.autoStopRunnable != null) {
                this.serviceHandler.removeCallbacks(this.autoStopRunnable);
            }
        } catch (Exception e) {
        }
        try {
            stopPlayerIfRunning();
        } catch (Exception e2) {
        }
        try {
            abandonAudioFocus();
        } catch (Exception e3) {
        }
        try {
            stopForeground(true);
        } catch (Exception e4) {
        }
        stopSelf();
    }

    private void stopPlayerIfRunning() {
        try {
            if (this.player != null) {
                try {
                    if (this.player.isPlaying()) {
                        this.player.stop();
                    }
                } catch (IllegalStateException e) {
                }
                this.player.reset();
                this.player.release();
            }
        } catch (Exception e2) {
        } finally {
            this.player = null;
        }
    }

    private void requestAudioFocus() {
        if (this.audioManager == null) {
            return;
        }
        this.audioFocusRequest = new AudioFocusRequest.Builder(2).setAudioAttributes(new AudioAttributes.Builder().setUsage(4).setContentType(4).build()).setOnAudioFocusChangeListener(this.afListener).build();
        this.audioManager.requestAudioFocus(this.audioFocusRequest);
    }

    private void abandonAudioFocus() {
        if (this.audioManager != null && this.audioFocusRequest != null) {
            this.audioManager.abandonAudioFocusRequest(this.audioFocusRequest);
        }
    }

    private void createForegroundChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Reminder Service", 2);
        ch.setSound(null, null);
        NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        try {
            stopPlayerIfRunning();
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
