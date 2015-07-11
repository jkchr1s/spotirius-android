package com.booshaday.spotirius.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.net.RestClient;
import com.booshaday.spotirius.service.SyncService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Created by chris on 7/3/15.
 */
public class ChannelManagerActivity extends Activity {

    private static final String TAG = "ChannelManagerActivity";
    private ListView mChannelListView;
    private Map<String, String> mPlaylists;
    private Button mAddChannelButton;
    private Context mContext;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_manager);

        mContext = this;
        mPlaylists = new HashMap<>();
        mAddChannelButton = (Button)findViewById(R.id.button_add_channel);
        mChannelListView = (ListView)findViewById(R.id.channelList);

        // refresh the token, then load existing channels
        refreshToken();

        mAddChannelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncService.addChannelByPicker(mContext);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode) {

            case Constants.ADD_CHANNELS_RESULT:
                if (resultCode==0) return;

                final String selectedChannel = String.valueOf(resultCode);

                if (mPlaylists==null) {
                    // we haven't loaded any playlists or no playlists exist
                    displayNewPlaylistDialog(String.valueOf(resultCode));
                } else {
                    // prompt to add new playlist or add to existing
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("Sync Target");
                    alertDialog.setMessage("Would you like to create a new playlist or sync with an existing one?");

                    final String selected = String.valueOf(resultCode);

                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "New", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            displayNewPlaylistDialog(selected);
                        } });

                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // just close the dialog
                        }});

                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Existing", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                            List<String> list = new ArrayList<>();

                            for (Map.Entry<String, String> item : mPlaylists.entrySet()) {
                                list.add(item.getValue());
                            }

                            final ArrayAdapter<String> playlistAdapter = new ArrayAdapter<String>(getBaseContext(), R.layout.channel_picker_row, list);
                            alert.setAdapter(playlistAdapter, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int i = 0;
                                    for (Map.Entry<String, String> item : mPlaylists.entrySet()) {
                                        if (i==which) {
                                            AppConfig.addChannel(getApplicationContext(), new Channel(selectedChannel, item.getKey()));
                                            updateChannelsList();
                                            break;
                                        } else {
                                            i++;
                                        }
                                    }
                                }
                            });

                            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Canceled.
                                }
                            });
                            alert.show();
                        }});
                    alertDialog.show();
                }
                break;
        }
    }

    private void refreshToken() {
        // user has had a valid session, so try to use the
        // refresh token to authenticate
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.ACCOUNTS_URL,
                null
        );
        client.getRefreshTokenAsync(
                Constants.SPOTIFY_CLIENT_ID,
                Constants.SPOTIFY_CLIENT_SECRET,
                "refresh_token",
                AppConfig.getRefreshToken(this),
                new Callback<JsonElement>() {
                    @Override
                    public void success(JsonElement j, retrofit.client.Response response) {
                        try {
                            AppConfig.setAccessToken(
                                    getApplicationContext(),
                                    j.getAsJsonObject().get("access_token").getAsString()
                            );
                            AppConfig.setExpiryTime(
                                    getApplicationContext(),
                                    System.currentTimeMillis() / 1000 + j.getAsJsonObject().get("expires_in").getAsLong()
                            );
                            Log.d(TAG, "Token refreshed successfully");
                            updateChannelsList();
                        } catch (Exception e) {
                            Log.e(TAG, "error getting access token: " + e.getMessage());
                            Toast.makeText(mContext, mContext.getString(R.string.toast_network_unavailable_retry),
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Toast.makeText(mContext, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(mContext, mContext.getString(R.string.toast_network_unavailable_retry),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void updateChannelsList() {
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

                        final List<Channel> channels = AppConfig.getChannels(getApplicationContext());

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
                            Log.d(TAG, "No channels found.");
                        }

                        ChannelsAdapter adapter = new ChannelsAdapter(mContext, (ArrayList) channels);
                        final ListView listView = (ListView) findViewById(R.id.channelList);
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

    private void displayNewPlaylistDialog(final String resultCode) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Create Playlist");
        alert.setMessage("Destination playlist name:");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText("Spotirius "+resultCode);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String value = input.getText().toString();
                Log.d(TAG, "Create playlist: "+value);

                RestClient.Spotify client = RestClient.create(
                        RestClient.Spotify.class,
                        RestClient.Spotify.API_URL,
                        "Bearer " + AppConfig.getAccessToken(mContext)
                );

                // build json body
                Map<String, Object> body = new HashMap<>();
                body.put("name", value);
                body.put("public", true);

                client.createPlaylistAsync(AppConfig.getUsername(mContext), body, new Callback<JsonElement>() {
                    @Override
                    public void success(JsonElement j, retrofit.client.Response response) {
                        try {
                            AppConfig.addChannel(mContext, new Channel(String.valueOf(resultCode),
                                    j.getAsJsonObject().get("id").getAsString()));
                            updateChannelsList();
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating playlist: " + e.getMessage());
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Error sending request: " + error.getMessage() + "\n" + error.getBody().toString());
                    }
                });
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }


}
