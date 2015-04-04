package com.booshaday.spotirius.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Created by chris on 4/3/15.
 */
public class SyncScheduleServiceReceiver extends BroadcastReceiver {
    private static final int SYNC_DELAY_HOURS = 24;
    private static final long SYNC_DELAY_MILLIS = SYNC_DELAY_HOURS * 60 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        // restart the sync every SYNC_DELAY_HOURS
        AlarmManager service = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, SyncStartServiceReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, SYNC_DELAY_HOURS);
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), SYNC_DELAY_MILLIS, pending);
    }
}
