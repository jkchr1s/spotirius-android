package com.booshaday.spotirius.app;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.text.TextUtils;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;

/**
 * Created by chris on 3/21/15.
 */
public class ApplicationController extends Application {

    public static final String TAG = "VolleyRequest";
    private static final String DEFAULT_CACHE_DIR = "volley";
    private SpotiriusRequestQueue mRequestQueue;
    private static ApplicationController instance;

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize the singleton
        instance = this;
    }

    /**
     * @return ApplicationController singleton instance
     */

    public static ApplicationController getInstance() {
        if (instance==null) {
            instance = getSync();
        }
        return instance;
    }

    private static synchronized ApplicationController getSync() {
        if(instance == null) instance = new ApplicationController();
        return instance;
    }

    /**
     * @return The Volley Request queue, the queue will be created if it is null
     */
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = newRequestQueue(instance.getApplicationContext(), null, -1, 1);
        }

        return mRequestQueue;
    }

    /**
     * Adds the specified request to the global queue, if tag is specified
     * then it is used else Default TAG is used.
     *
     * @param req
     * @param tag
     */
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);

        VolleyLog.d("Adding request to queue: %s", req.getUrl());

        getRequestQueue().add(req);
    }

    public boolean isEmpty() {
        if (mRequestQueue==null) return true;
        return mRequestQueue.isEmpty();
    }

    /**
     * Adds the specified request to the global queue using the Default TAG.
     *
     * @param req
     */
    public <T> void addToRequestQueue(Request<T> req) {
        // set the default tag if tag is empty
        req.setTag(TAG);

        VolleyLog.d("Adding request to queue: %s", req.getUrl());

        getRequestQueue().add(req);
    }

    /**
     * Cancels all pending requests by the specified TAG, it is important
     * to specify a TAG so that the pending/ongoing requests can be cancelled.
     *
     * @param tag
     */
    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    private SpotiriusRequestQueue newRequestQueue(Context context, HttpStack stack, int maxDiskCacheBytes, int maxSimultaneousRequests) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        SpotiriusRequestQueue queue;
        if (maxDiskCacheBytes <= -1)
        {
            // No maximum size specified
            queue = new SpotiriusRequestQueue(new DiskBasedCache(cacheDir), network, maxSimultaneousRequests);
        }
        else
        {
            // Disk cache size specified
            queue = new SpotiriusRequestQueue(new DiskBasedCache(cacheDir, maxDiskCacheBytes), network, maxSimultaneousRequests);
        }

        queue.start();

        return queue;
    }
}