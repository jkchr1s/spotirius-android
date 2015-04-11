package com.booshaday.spotirius.app;

import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ResponseDelivery;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

/**
 * Created by chris on 4/4/15.
 */
public class SpotiriusRequestQueue extends RequestQueue {
    private static final String TAG = "SpotiriusRequestQueue";
    private int queueCount = 0;

    public SpotiriusRequestQueue(Cache cache, Network network, int threadPoolSize, ResponseDelivery delivery) {
        super(cache, network, threadPoolSize, delivery);

        super.addRequestFinishedListener(new RequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(Request<Object> request) {
                removeFromQueueCount();
            }
        });
    }

    public SpotiriusRequestQueue(Cache cache, Network network, int threadPoolSize) {
        super(cache, network, threadPoolSize);

        super.addRequestFinishedListener(new RequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(Request<Object> request) {
                removeFromQueueCount();
            }
        });
    }

    public SpotiriusRequestQueue(Cache cache, Network network) {
        super(cache, network);

        super.addRequestFinishedListener(new RequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(Request<Object> request) {
                removeFromQueueCount();
            }
        });
    }

    @Override
    public <T> Request<T> add(Request<T> request) {
        addToQueueCount();

        return super.add(request);
    }

    public boolean isEmpty() {
        return queueCount==0;
    }

    private void removeFromQueueCount() {
        Log.d(TAG, "removing item from queue");
        queueCount = queueCount - 1;
        Log.d(TAG, "queueCount: "+String.valueOf(queueCount));
    }

    private void addToQueueCount() {
        Log.d(TAG, "adding item to queue");
        queueCount = queueCount + 1;
        Log.d(TAG, "queueCount: "+String.valueOf(queueCount));
    }
}
