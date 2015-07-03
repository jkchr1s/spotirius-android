package com.booshaday.spotirius.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.WelcomeActivity;

/**
 * Created by chris on 6/13/15.
 */
public class SyncProgressReceiver extends BroadcastReceiver {
    private ScrollView mScrollView;
    private TextView mTextView;
    private TextView mHeadingTextView;
    private Button mSyncButton;
    private static final String EOL = "\n";
    private static final String TAG = "SyncProgressReceiver";

    public SyncProgressReceiver(TextView textView) {
        this.mTextView = textView;

        try {
            mScrollView = ((ScrollView)mTextView.getParent());
        } catch (Exception e) {
            Log.e(TAG, "Unable to determine mTextView parent");
        }
    }

    public SyncProgressReceiver(TextView statusTextView, TextView headingTextView, Button syncButton) {
        this.mTextView = statusTextView;
        this.mHeadingTextView = headingTextView;
        this.mSyncButton = syncButton;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String heading = intent.getStringExtra("syncHeading");
        final String text = intent.getStringExtra("syncStatus");
        if (text!=null && mTextView!=null) {
            Log.d(TAG, "Received BroadcastIntent: " + text);

            if (mScrollView != null) {
                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
                mTextView.append(text + EOL);
            } else {
                mTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(text);
                    }
                });

            }
        } else if (heading!=null && mHeadingTextView!=null) {
            mHeadingTextView.post(new Runnable() {
                @Override
                public void run() {
                    mHeadingTextView.setText(heading);
                }
            });
            if (heading.equals("Sync Complete") && mSyncButton!=null) {
                mSyncButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mSyncButton.setText(context.getString(R.string.sync_now));
                        mSyncButton.setEnabled(true);
                    }
                });
                if (context!=null && context instanceof WelcomeActivity) {
                    ((WelcomeActivity)context).updateChannelsDisplay();
                }
            }
        } else {
            Log.d(TAG, "Received BroadcastIntent, but text was null");
        }
    }
}
