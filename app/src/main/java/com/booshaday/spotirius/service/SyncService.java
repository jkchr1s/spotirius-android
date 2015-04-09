package com.booshaday.spotirius.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.booshaday.spotirius.net.DogStarRadioClient;

/**
 * Created by chris on 4/3/15.
 */
public class SyncService extends Service {
    private final String TAG = "SyncService";
    private final IBinder mBinder = new SyncBinder();
    private DogStarRadioClient mClient;

//    public SyncService() {
//        super("SyncService");
//    }

//    public SyncService(String name) {
//        super(name);
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        Log.d(TAG, "onStartCommand");
        if (mClient==null)
            mClient = new DogStarRadioClient(getApplicationContext());
        mClient.sync();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

//    @Override
//    protected void onHandleIntent(Intent intent) {
//        Log.d(TAG, "onHandleIntent()");
//        Toast.makeText(getApplicationContext(), "Sync started", Toast.LENGTH_SHORT).show();
//        DogStarRadioClient client = new DogStarRadioClient(getApplicationContext());
//        client.sync();
//    }

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
        mClient = null;
        super.onDestroy();
    }
}
