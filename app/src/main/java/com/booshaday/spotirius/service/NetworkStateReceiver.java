package com.booshaday.spotirius.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import com.booshaday.spotirius.data.AppConfig;

/**
 * Created by chris on 6/13/15.
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";
    private PendingIntent mPendingIntent;
    private AlarmManager mAlarmManager;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent==null || intent.getAction()==null) return;
        if (!AppConfig.isValidSession(context)) return;
        String action = intent.getAction();

        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            // device booted
            // start the alarm, check every 4 hours
            Log.d(TAG, "received BOOT_COMPLETED");

            initPendingIntent(context);
            mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            int interval = 14400000; // 4 hours
            long startAt = System.currentTimeMillis() + 60000; // 1 minute after boot_completed
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startAt, interval, mPendingIntent);
        } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
            Log.d(TAG, "received CONNECTIVITY_CHANGE");

            // see if network is connected
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                // network is connected
                if (wifi.isConnected()) {
                    Log.d(TAG, "wifi connected");
                    if (System.currentTimeMillis() - AppConfig.getLastSync(context) > 14400000) {
                        Log.d(TAG, "sync has not occurred in 14400000 ms, updating last sync");
                        AppConfig.updateLastSync(context);

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "starting sync");
                                Intent syncIntent = new Intent(context, SyncIntentService.class);
                                context.startService(syncIntent);
                            }
                        }, 30000);
                    } else {
                        Log.d(TAG, "sync has occurred in past 14400000 ms, no sync required");
                    }
                } else {
                    Log.d(TAG, "found network, but not wifi");
                }
            }
        }
    }

    private void initPendingIntent(Context context) {
        if (mPendingIntent==null) {
            mPendingIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(context, SyncAlarmReceiver.class), 0);
        }
    }
}
