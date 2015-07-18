package com.booshaday.spotirius.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by chris on 6/13/15.
 */
public class SyncAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "SyncAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "starting sync");
        Intent syncIntent = new Intent(context, SyncIntentService.class);
        context.startService(syncIntent);
    }
}
