package com.booshaday.spotirius.app;

import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ResponseDelivery;

/**
 * Created by chris on 4/4/15.
 */
public class SpotiriusRequestQueue extends RequestQueue {
    private static final String TAG = "SpotiriusRequestQueue";
    private int queueCount = 0;

    public SpotiriusRequestQueue(Cache cache, Network network, int threadPoolSize, ResponseDelivery delivery) {
        super(cache, network, threadPoolSize, delivery);
    }

    public SpotiriusRequestQueue(Cache cache, Network network, int threadPoolSize) {
        super(cache, network, threadPoolSize);
    }

    public SpotiriusRequestQueue(Cache cache, Network network) {
        super(cache, network);
    }

    @Override
    public <T> Request<T> add(Request<T> request) {
        queueCount++;
        return super.add(request);
    }

    public boolean isEmpty() {
        queueCount++;

        int nextSequence = super.getSequenceNumber();

        Log.d(TAG, String.format("queueCount: %d, nextSequence: %d", queueCount, nextSequence));
        if (queueCount==nextSequence) {
            return true;
        }

        return false;
    }
}
