package com.booshaday.spotirius.net;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.WelcomeActivity;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.service.SyncIntentService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Created by chris on 6/23/15.
 */
public class GuiNetworkClient {
    private static final String TAG = "GuiNetworkClient";

    private Context mContext;
    private TextView mStatusText;
    private TextView mMeText;
    private ProgressBar mProgressBar;
    private Bitmap mMeImage;
    private Map<String, String> mPlaylists;
    private RoundedImageView mRoundedImageView;
    private ScrollView mScrollView;

    public GuiNetworkClient(Context context) {
        this.mContext = context;

        if (mContext instanceof WelcomeActivity) {
            try {
                mStatusText = (TextView) ((WelcomeActivity)mContext).findViewById(R.id.status);
                mMeText = (TextView) ((WelcomeActivity)mContext).findViewById(R.id.meText);
                mProgressBar = (ProgressBar) ((WelcomeActivity)mContext).findViewById(R.id.progressBar);
                mRoundedImageView = (RoundedImageView) ((WelcomeActivity)mContext).findViewById(R.id.mePic);
                mScrollView = (ScrollView) ((WelcomeActivity)mContext).findViewById(R.id.main_scrollview);
            } catch (Exception e) {}
        }
    }

    public void restoreSession() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.ACCOUNTS_URL,
                null
        );

        client.getRefreshTokenAsync(
                Constants.SPOTIFY_CLIENT_ID,
                Constants.SPOTIFY_CLIENT_SECRET,
                "refresh_token",
                AppConfig.getRefreshToken(mContext),
                new Callback<JsonElement>() {

            @Override
            public void success(JsonElement j, retrofit.client.Response response) {
                AppConfig.setAccessToken(
                        mContext,
                        j.getAsJsonObject().get("access_token").getAsString()
                );
                AppConfig.setExpiryTime(
                        mContext,
                        System.currentTimeMillis() / 1000 + j.getAsJsonObject().get("expires_in").getAsLong()
                );

                getMe();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, error.getMessage());
                Toast.makeText(mContext, "Error refreshing token", Toast.LENGTH_LONG).show();
                if (mContext != null && mContext instanceof WelcomeActivity) {
                    ((WelcomeActivity)mContext).finish();
                }
            }
        });
    }

    public void getMe() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );

        client.getMeAsync(new Callback<JsonElement>() {
            @Override
            public void success(final JsonElement j, retrofit.client.Response response) {

                Log.d(TAG, j.toString());
                String imageUrl;

                try {
                    AppConfig.setUsername(
                            mContext,
                            j.getAsJsonObject().get("id").getAsString()
                    );

                    ((WelcomeActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMeText.setText("Logged In: "+j.getAsJsonObject().get("display_name").getAsString());
                        }
                    });


                    JsonArray images = j.getAsJsonObject().get("images").getAsJsonArray();
                    if (images.size() > 0) {
                        imageUrl = images.get(0).getAsJsonObject().get("url").getAsString();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            try {
                                OkHttpClient client = new OkHttpClient();

                                Request request = new Request.Builder()
                                        .url(imageUrl)
                                        .build();

                                client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
                                    @Override
                                    public void onFailure(Request request, IOException e) {
                                        hideProgress();
                                    }

                                    @Override
                                    public void onResponse(Response response) throws IOException {
                                        BitmapFactory.Options options = new BitmapFactory.Options();
                                        mMeImage = BitmapFactory.decodeStream(response.body().byteStream(), null, options);
                                        ((WelcomeActivity)mContext).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mRoundedImageView.setImageBitmap(Bitmap.createScaledBitmap(mMeImage, dpToPx(90), dpToPx(90), false));
                                            }
                                        });
                                        hideProgress();
                                    }
                                });
                            } catch (Exception e) {
                                hideProgress();
                                e.printStackTrace();
                            }
                        } else {
                            hideProgress();
                        }

                    } else {
                        hideProgress();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing JSON response for me");
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void hideProgress() {
        if (mProgressBar!=null) {
            mProgressBar.post(new Runnable() {

                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                }
            });

        }
        if (mRoundedImageView!=null) {
            mRoundedImageView.post(new Runnable() {
                @Override
                public void run() {
                    mRoundedImageView.setVisibility(View.VISIBLE);
                }
            });
        }
        if (mMeText!=null) {
            mMeText.post(new Runnable() {
                @Override
                public void run() {
                    mMeText.setVisibility(View.VISIBLE);
                }
            });
        }
        if (mStatusText!=null) {
            mStatusText.post(new Runnable() {
                @Override
                public void run() {
                    mStatusText.setVisibility(View.VISIBLE);
                    if (mContext!=null) {
                        if (isSyncRunning()) {
                            mStatusText.setText(mContext.getString(R.string.status_sync_running));
                        } else {
                            mStatusText.setText(mContext.getString(R.string.status_sync_not_running));
                        }
                    }
                }
            });
        }
        if (mScrollView!=null) {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void updateRefreshToken(String accessCode) {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.ACCOUNTS_URL,
                null
        );
        client.getAccessTokenAsync(
                Constants.SPOTIFY_CLIENT_ID,
                Constants.SPOTIFY_CLIENT_SECRET,
                "authorization_code",
                accessCode,
                Constants.SPOTIFY_REDIRECT_URI,
                new Callback<JsonElement>() {
                    @Override
                    public void success(JsonElement j, retrofit.client.Response response) {
                        try {
                            AppConfig.setAccessToken(
                                    mContext,
                                    j.getAsJsonObject().get("access_token").getAsString()
                            );
                            AppConfig.setRefreshToken(
                                    mContext,
                                    j.getAsJsonObject().get("refresh_token").getAsString()
                            );
                            AppConfig.setExpiryTime(
                                    mContext,
                                    System.currentTimeMillis() / 1000 + j.getAsJsonObject().get("expires_in").getAsLong()
                            );
                            getMe();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing refresh token response: " + e.getMessage());
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Error getting refresh token: " + error.getMessage());
                        Log.e(TAG, error.getUrl() + "\n" + error.getBody().toString());
                    }
                });
    }

    public void updateChannelsList() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        client.getPlaylistsAsync(
                AppConfig.getUsername(mContext),
                50,
                0,
                new Callback<JsonElement>() {
                    @Override
                    public void success(JsonElement j, retrofit.client.Response response) {
                        mPlaylists = new HashMap<>();

                        // TODO: page through playlists, we are only processing 1 page

                        try {
                            JsonArray items = j.getAsJsonObject().get("items").getAsJsonArray();
                            for (JsonElement e : items) {
                                mPlaylists.put(
                                        e.getAsJsonObject().get("id").getAsString(),
                                        e.getAsJsonObject().get("name").getAsString()
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing playlists, descriptions will not be shown");
                        }

                        final List<Channel> channels = AppConfig.getChannels(mContext);

                        for (Channel channel : channels) {
                            Log.d(TAG, channel.getPlaylistId() + ": " + channel.getChannel());
                        }

                        for (Channel channel : channels) {
                            if (mPlaylists.containsKey(channel.getPlaylistId())) {
                                Log.d(TAG, "Found playlist description");
                                channel.setPlaylist(mPlaylists.get(channel.getPlaylistId()));
                            }
                        }

                        if (channels.isEmpty()) {
                            Toast.makeText(mContext,
                                    "You do not have any channels. You can add channels by clicking the Menu button and selecting Add Channel.",
                                    Toast.LENGTH_LONG).show();
                        }

                        ChannelsAdapter adapter = new ChannelsAdapter(mContext, (ArrayList) channels);
                        final ListView listView = (ListView) ((WelcomeActivity) mContext).findViewById(R.id.channelList);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    final int position, long id) {
                                new AlertDialog.Builder(mContext)
                                        .setTitle("Remove Channel")
                                        .setMessage("Are you sure you want to delete channel " + channels.get(position).getChannel() + "?")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                AppConfig.deleteChannel(mContext, channels.get(position).getChannel());
                                                updateChannelsList();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, null).show();
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, error.getUrl() + " " + error.getMessage());
                    }
                }
        );
    }

    private static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public class ChannelsAdapter extends ArrayAdapter<Channel> {
        public ChannelsAdapter(Context context, ArrayList<Channel> channels) {
            super(context, 0, channels);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Channel channel = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.channel_row, parent, false);
            }
            // Lookup view for data population
            TextView title = (TextView) convertView.findViewById(R.id.item_title);
            TextView desc = (TextView) convertView.findViewById(R.id.item_desc);
            // Populate the data into the template view using the data object
            desc.setText("Satellite channel "+channel.getChannel());
            title.setText(channel.getPlaylist());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    private boolean isSyncRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
        String classname = SyncIntentService.class.getName();
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (classname.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
