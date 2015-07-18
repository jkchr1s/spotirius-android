package com.booshaday.spotirius;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.net.GuiNetworkClient;
import com.booshaday.spotirius.service.SyncIntentService;
import com.booshaday.spotirius.service.SyncProgressReceiver;
import com.booshaday.spotirius.view.ChannelManagerActivity;
import com.makeramen.roundedimageview.RoundedImageView;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class WelcomeActivity extends Activity {
    private Context mContext;
    private RoundedImageView mRoundedImageView;
    private Bitmap mMeImage;
    private TextView mMeText;
    private TextView mStatusText;
    private ProgressBar mProgressBar;
    private GuiNetworkClient mClient;
    private ScrollView mScrollView;
    private Button mSyncNowButton;
    private TextView mSyncNowTextView;
    private Button mManageChannelsButton;
    private Button mLogOutButton;
    private IntentFilter mIntentFilter;
    private CheckBox mStrictCheckbox;
    private SyncProgressReceiver mSyncProgressReceiver;

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mContext = this;
        mClient = new GuiNetworkClient(this);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mRoundedImageView = (RoundedImageView)findViewById(R.id.mePic);
        mMeText = (TextView)findViewById(R.id.meText);
        mStatusText = (TextView)findViewById(R.id.status);
        mScrollView = (ScrollView)findViewById(R.id.main_scrollview);
        mSyncNowButton = (Button)findViewById(R.id.button_sync_now);
        mSyncNowTextView = (TextView)findViewById(R.id.sync_now_desc);
        mManageChannelsButton = (Button)findViewById(R.id.button_manage_channels);
        mLogOutButton = (Button)findViewById(R.id.button_log_out);
        mStrictCheckbox = (CheckBox)findViewById(R.id.checkbox_strict);
        mStrictCheckbox.setChecked(AppConfig.getStrictSearchEnabled(this));

        mProgressBar.setIndeterminate(true);
        mStatusText.setText(getString(R.string.status_logging_in));

        // set up intentfilter for sync progress
        mIntentFilter = new IntentFilter("com.booshaday.spotirius.SyncProgress");
        mSyncProgressReceiver = new SyncProgressReceiver(mStatusText, mMeText, mSyncNowButton);
        registerReceiver(mSyncProgressReceiver, mIntentFilter);

        // check to make sure network is available
        if (!isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.toast_network_unavailable), Toast.LENGTH_LONG).show();
            finish();
        }

        // register button click events
        registerOnTouchEvents();

        // see if the user is logged in
        // if user is not logged in, we need to auth them
        if (!AppConfig.isValidSession(getApplicationContext())) {
            // user is not currently logged in
            Toast.makeText(this, getString(R.string.toast_please_login), Toast.LENGTH_SHORT).show();
            openLoginWindow();
            return;
        } else {
            // log in from saved session
            mClient.restoreSession();

            if (isSyncRunning()) {
                mStatusText.setText(getString(R.string.status_sync_running));
                mSyncNowButton.setEnabled(false);
                mSyncNowButton.setText(getText(R.string.sync_now_running));
            } else {
                mStatusText.setText(getString(R.string.status_sync_not_running));
            }

            // disable the sync button if no channels are specified
            updateChannelsDisplay();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        Log.d(TAG, "unregistering broadcast receiver");
        unregisterReceiver(mSyncProgressReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "reregistering broadcast receiver");
        registerReceiver(mSyncProgressReceiver, mIntentFilter);

        updateChannelsDisplay();
    }

    public void updateChannelsDisplay() {
        if (AppConfig.getChannels(this).isEmpty()) {
            if (mSyncNowButton!=null) {
                mSyncNowButton.setEnabled(false);
            }
            if (mSyncNowTextView!=null) {
                mSyncNowTextView.setText(getString(R.string.no_channels_status_desc));
            }
        } else {
            if (mSyncNowButton!=null) {
                mSyncNowButton.setEnabled(true);
            }
            if (mSyncNowTextView!=null) {
                mSyncNowTextView.setText(getString(R.string.sync_now_desc));
            }
        }

        if (isSyncRunning()) {
            mStatusText.setText(getString(R.string.status_sync_running));
            mSyncNowButton.setEnabled(false);
            mSyncNowButton.setText(getText(R.string.sync_now_running));
            mManageChannelsButton.setEnabled(false);
            mStrictCheckbox.setEnabled(false);
            mLogOutButton.setEnabled(false);
        } else {
            mStatusText.setText(getString(R.string.status_sync_not_running));
            mSyncNowButton.setEnabled(true);
            mSyncNowButton.setText(getText(R.string.sync_now));
            mManageChannelsButton.setEnabled(true);
            mStrictCheckbox.setEnabled(true);
            mLogOutButton.setEnabled(true);
        }
    }

    private void openLoginWindow() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(Constants.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.CODE, Constants.SPOTIFY_REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "playlist-modify-private", "playlist-modify-public"})
                .build();

        AuthenticationClient.openLoginActivity(this, Constants.SPOTIFY_AUTH_RESULT, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode) {
            case Constants.SPOTIFY_AUTH_RESULT:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                switch (response.getType()) {
                    // Response was successful and contains auth token
                    case CODE:
                        if (mClient!=null) {
                            mClient.updateRefreshToken(response.getCode());
                        }
                        break;

                    // Auth flow returned an error
                    case ERROR:
                        Toast.makeText(this, getString(R.string.toast_authentication_error), Toast.LENGTH_LONG).show();
                        finish();
                        break;

                    // Most likely auth flow was cancelled
                    default:
                        Toast.makeText(this, getString(R.string.toast_authentication_error), Toast.LENGTH_LONG).show();
                        finish();
                        break;
                }
                break;
        }
    }

    private void registerOnTouchEvents() {
        if (mSyncNowButton!=null) {
            mSyncNowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mContext!=null) {
                        mStatusText.setText(getString(R.string.status_sync_running));
                        mSyncNowButton.setEnabled(false);
                        mSyncNowButton.setText(getText(R.string.sync_now_running));
                        mManageChannelsButton.setEnabled(false);
                        mStrictCheckbox.setEnabled(false);
                        mLogOutButton.setEnabled(false);

                        Intent syncIntent = new Intent(mContext, SyncIntentService.class);
                        startService(syncIntent);
                    }
                }
            });
        }

        if (mManageChannelsButton!=null) {
            mManageChannelsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mContext!=null) {
                        if (!isSyncRunning()) {
                            Intent intent = new Intent(mContext, ChannelManagerActivity.class);
                            startActivity(intent);
                        }
                    }
                }
            });
        }

        if (mLogOutButton!=null) {
            mLogOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: this doesn't work right...
                    AppConfig.setAccessToken(mContext, "");
                    AppConfig.setExpiryTime(mContext, 0);
                    AppConfig.setRefreshToken(mContext, "");
                    AppConfig.setUsername(mContext, "");
                    finish();
                }
            });
        }

        if (mStrictCheckbox!=null) {
            mStrictCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, String.format("Strict searching: %b", isChecked));
                    if (mContext!=null) {
                        AppConfig.setStrictSearchEnabled(mContext, isChecked);
                    }
                }
            });
        }
    }

    private boolean isSyncRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        String classname = SyncIntentService.class.getName();
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (classname.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
