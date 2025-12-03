package com.stmicroelectronics.stperf.service;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.stmicroelectronics.stperf.MainActivity;
import com.stmicroelectronics.stperf.R;
import com.stmicroelectronics.stperf.SettingsActivity;
import com.stmicroelectronics.stperf.data.PerfData;

import java.util.List;

public class PerfService extends Service {

    public static final String ACTION_START = "com.stmicroelectronics.stperf.service.action.start";
    private static final String ACTION_STOP = "com.stmicroelectronics.stperf.service.action.stop";

    private static final int NOTIFICATION_ID = 123456789;
    private static final String NOTIFICATION_CHANNEL_ID = "com.stmicroelectronics.stperf.service.notification.channel_id";

    private static final String DEFAULT_PERIOD = "5";

    private NotificationManager mNotificationManager;
    private PerfLayout mLayout;
    private PerfData mPerformance;

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
    private boolean mCpu;
    private boolean mCpuAvg;
    private boolean mGpu;
    private boolean mFps;
    private boolean mGraph;
    private boolean mLog;
    private boolean mBack;
    private boolean mPos;
    private long mPeriod;

    public PerfService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mCpu = preferences.getBoolean(getString(R.string.pref_cpu_label), true);
        mCpuAvg = preferences.getBoolean(getString(R.string.pref_cpu_avg_label), true);
        mGpu = preferences.getBoolean(getString(R.string.pref_gpu_label), true);
        mFps = preferences.getBoolean(getString(R.string.pref_fps_label), true);
        mGraph = preferences.getBoolean(getString(R.string.pref_graph_label), false);
        mBack = preferences.getBoolean(getString(R.string.pref_back_label), true);
        mPos = preferences.getBoolean(getString(R.string.pref_pos_label), true);
        mLog = preferences.getBoolean(getString(R.string.pref_log_label), false);

        mPeriod = Long.parseLong(preferences.getString(getString(R.string.pref_period_label), DEFAULT_PERIOD));

        mLayout = new PerfLayout(getApplicationContext(), mGraph, mPos);
        mLayout.initLayoutVisibility(mCpu, mCpuAvg, mGpu, mFps);
        mLayout.setBackColor(mBack);

        mPerformance = new PerfData(mLayout);

        mPerformance.setPeriod(mPeriod * 1000);
        mPerformance.setLog(mLog);

        if (mPrefListener == null) {
            mPrefListener = (sharedPreferences, key) -> {
                if (key.equals(getString(R.string.pref_cpu_label))) {
                    mCpu = sharedPreferences.getBoolean(key, true);
                }
                if (key.equals(getString(R.string.pref_cpu_avg_label))) {
                    mCpuAvg = sharedPreferences.getBoolean(key, false);
                }
                if (key.equals(getString(R.string.pref_gpu_label))) {
                    mGpu = sharedPreferences.getBoolean(key, true);
                }
                if (key.equals(getString(R.string.pref_fps_label))) {
                    mFps = sharedPreferences.getBoolean(key, true);
                }
                if (key.equals(getString(R.string.pref_period_label))) {
                    mPeriod = Long.parseLong(sharedPreferences.getString(key, DEFAULT_PERIOD));
                    mPerformance.setPeriod(mPeriod * 1000);
                }
                if (key.equals(getString(R.string.pref_graph_label))) {
                    mGraph = sharedPreferences.getBoolean(key, false);
                }
                if (key.equals(getString(R.string.pref_log_label))) {
                    mLog = sharedPreferences.getBoolean(key, false);
                    mPerformance.setLog(mLog);
                }
                if (key.equals(getString(R.string.pref_back_label))) {
                    mBack = sharedPreferences.getBoolean(key, true);
                }
                if (key.equals(getString(R.string.pref_pos_label))) {
                    mPos = sharedPreferences.getBoolean(key, true);
                }
                mLayout.updateLayoutVisibility(mCpu, mCpuAvg, mGpu, mFps, mGraph, mPos);
                mLayout.setBackColor(mBack);
            };
        }

        preferences.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound service (not the case for this service).
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent == null) || (intent.getAction() == null))
            return super.onStartCommand(intent, flags, startId);

        switch (intent.getAction()) {
            case ACTION_START:
                // create action to stop foreground service
                // TODO: add dedicated icon
                Intent stopIntent = new Intent(this, PerfService.class);
                stopIntent.setAction(ACTION_STOP);
                PendingIntent pendingStopIntent;
                pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_MUTABLE);
                NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(R.drawable.ic_stop, getString(R.string.notification_stop_title), pendingStopIntent).build();

                // create action to open settings
                // TODO: add dedicated icon
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                PendingIntent pendingSettingsIntent;
                pendingSettingsIntent = PendingIntent.getActivity(this, 1, settingsIntent, PendingIntent.FLAG_MUTABLE);
                NotificationCompat.Action settingsAction = new NotificationCompat.Action.Builder(R.drawable.ic_settings, getString(R.string.notification_settings_title), pendingSettingsIntent).build();

                Intent notificationIntent = new Intent(this, MainActivity.class);

                PendingIntent pendingIntent;
                pendingIntent = PendingIntent.getActivity(this, 2, notificationIntent, PendingIntent.FLAG_MUTABLE);

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.notification_name), NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription(getString(R.string.notification_description));
                mNotificationManager = getSystemService(NotificationManager.class);
                // mNotificationManager = NotificationManagerCompat.from(this);
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }


                Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.ic_st_black)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .addAction(stopAction)
                        .addAction(settingsAction)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(NOTIFICATION_ID,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                    } else{
                        startForeground(NOTIFICATION_ID, notification);
                    }
                    // mNotificationManager.notify(NOTIFICATION_ID, notification);
                    mPerformance.startDataUpdate(mPeriod * 1000);
                }
                break;
            case ACTION_STOP:
                if (mNotificationManager != null) {
                    mNotificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
                }
                mPerformance.stopDataUpdate();
                mLayout.stopLayout();
                stopForeground(STOP_FOREGROUND_DETACH);
                stopSelf();
                // kill the application to free its memory usage (waiting clarification)
                killApp();
                break;
            default:
                return super.onStartCommand(intent, flags, startId);
        }
        return START_STICKY;
    }

    private void killApp() {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> pids;
        if (am != null) {
            pids = am.getRunningAppProcesses();
            for (int i = 0; i < pids.size(); i++) {
                ActivityManager.RunningAppProcessInfo info = pids.get(i);
                if (info.processName.equalsIgnoreCase("com.stmicroelectronics.stperf")) {
                    android.os.Process.killProcess(info.pid);
                }
            }
        }
    }
}
