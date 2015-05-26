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
import com.booshaday.spotirius.app.ApplicationController;
import com.booshaday.spotirius.app.SpotiriusRequestQueue;
import com.booshaday.spotirius.data.SpotiriusChannel;
import com.booshaday.spotirius.net.DogStarRadioClient;

import java.util.List;

/**
 * Created by chris on 4/3/15.
 */
public class SyncService extends Service  implements SpotiriusRequestQueue.OnQueueComplete {
    private final String TAG = "SyncService";
    private final int NOTIFICATION_ID = 1000;
    private final IBinder mBinder = new SyncBinder();

    private DogStarRadioClient mClient;
    private NotificationManager mNotificationManager;
    private Intent mIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        this.mIntent = intent;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Spotirius Sync")
                .setContentText("Playlist sync in progress...");

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        ApplicationController.getInstance().setOnQueueCompleteCallback(this);
        ApplicationController.getInstance().setCallbackEnabled(false);

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

    @Override
    public void onQueueComplete() {
        Log.d(TAG, "onQueueComplete");
        if (ApplicationController.getInstance().isEmpty()) {
            Log.d(TAG, "queue is empty");
            ApplicationController.getInstance().setOnQueueCompleteCallback(null);
            List<SpotiriusChannel> channels = ApplicationController.getDb().getSyncChannels();
            if (channels!=null) {
                for (SpotiriusChannel channel : channels) {
                    ApplicationController.getDb().updateChannelLastSync(channel.getId());
                }
            }

            if (getApplicationContext()!=null && mIntent!=null) {
                getApplicationContext().stopService(mIntent);
            }
        } else {
            Log.d(TAG, "queue is not empty");
        }
    }

    public class SyncBinder extends Binder {
        SyncService getService() {
            return SyncService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (mClient!=null) {
            mClient.sendStopSignal();
        }

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