package com.booshaday.spotirius.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by chris on 4/3/15.
 */
public class SyncService extends Service {
    private final IBinder mBinder = new SyncBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class SyncBinder extends Binder {
        SyncService getService() {
            return SyncService.this;
        }
    }
}
