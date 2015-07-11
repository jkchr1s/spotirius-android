package com.booshaday.spotirius.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by chris on 6/13/15.
 */
public class BootReceiver extends BroadcastReceiver {
    private PendingIntent mPendingIntent;
    private AlarmManager mAlarmManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // start the alarm
            Intent alarmIntent = new Intent(context, SyncAlarmReceiver.class);
            mPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            int interval = 10000;
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, mPendingIntent);
        }
    }
}
