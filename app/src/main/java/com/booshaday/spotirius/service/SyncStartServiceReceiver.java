package com.booshaday.spotirius.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by chris on 4/3/15.
 */
public class SyncStartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SyncService.class);
        context.startService(service);
    }
}
