package com.booshaday.spotirius.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by chris on 6/13/15.
 */
public class SyncAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Time to sync!", Toast.LENGTH_SHORT).show();
    }
}
