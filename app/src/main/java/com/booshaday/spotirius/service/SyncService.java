package com.booshaday.spotirius.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.net.DogStarRadioClient;

/**
 * Created by chris on 4/3/15.
 */
public class SyncService extends Service {
    private final String TAG = "SyncService";
    private final int NOTIFICATION_ID = 1000;
    private final IBinder mBinder = new SyncBinder();
    private DogStarRadioClient mClient;
    private NotificationManager mNotificationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Spotirius Sync")
                .setContentText("Playlist sync in progress...");

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        if (mClient==null)
            mClient = new DogStarRadioClient(getApplicationContext(), intent);
        mClient.sync();
        return Service.START_STICKY;
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (mNotificationManager!=null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager = null;
        }

        if (mClient!=null) {
            mClient.sendStopSignal();
        }
        mClient = null;

        super.onDestroy();
    }
}
