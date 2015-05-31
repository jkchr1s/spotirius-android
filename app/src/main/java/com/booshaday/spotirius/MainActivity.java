package com.booshaday.spotirius;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.net.RestClient;
import com.booshaday.spotirius.service.SyncIntentService;
import com.booshaday.spotirius.service.SyncService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private static final String EOL = "\n";
    private final Context mContext = this;
    private boolean mIsFirstLogin = false;
    private Map<String, String> mPlaylists;
    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView)findViewById(R.id.recent_activity);

        mTextView.setText("Spotirius alpha 4 started."+EOL);

        initMainActivity();

        Log.d(TAG, "channels: " + String.valueOf(AppConfig.getChannels(this).size()));
    }

    public void initMainActivity() {
        // if user is not logged in, we need to auth them
        if (!AppConfig.isValidSession(getApplicationContext())) {
            // user is not currently logged in
            addLog("No Spotify session found, please log in.");
            openLoginWindow();
            return;
        }

        addLog("Logging in to saved session...");

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
                            addLog("Login success.");
                            updateChannelsList();
                        } catch (Exception e) {
                            Log.e(TAG, "error getting access token: " + e.getMessage());
                            addLog("Error obtaining token, please log in again.");
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Toast.makeText(mContext, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error getting access token: " + error.getMessage());
                        addLog("Error obtaining token, please log in again.");
                    }
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_sync:
                addLog("Sync started. You can put this application in the background.");
                Intent syncIntent = new Intent(this, SyncIntentService.class);
                startService(syncIntent);
                return true;

            case R.id.action_login:
                openLoginWindow();

                return true;

            case R.id.action_add_channel:
                SyncService.addChannelByPicker(this);
                return true;

            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
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
                        mIsFirstLogin = true;
                        updateRefreshToken(response.getCode());
                        break;

                    // Auth flow returned an error
                    case ERROR:
                        logStatus("Auth error: " + response.getError());
                        addLog("Authentication error: " + response.getError());
                        break;

                    // Most likely auth flow was cancelled
                    default:
                        logStatus("Auth result: " + response.getType());
                        addLog("Authentication error: "+response.getError());
                        finish();
                        break;
                }
                break;

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
                                            addLog("Added channel: " + selectedChannel);
                                            addLog("Refreshing channel list...");
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
                            addLog("Created playlist: " + value);
                            updateChannelsList();
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating playlist: "+e.getMessage());
                            addLog("Error creating playlist: " + e.getMessage());
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Error sending request: "+ error.getMessage()+"\n"+error.getBody().toString());
                        addLog("Error creating playlist: "+error.getMessage());
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

    private void getMe() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );

        client.getMeAsync(new Callback<JsonElement>() {
            @Override
            public void success(JsonElement j, retrofit.client.Response response) {
                try {
                    AppConfig.setUsername(
                            mContext,
                            j.getAsJsonObject().get("id").getAsString()
                    );
                    mIsFirstLogin = false;
                    updateChannelsList();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing JSON response for me");
                    logStatus("Cannot load Spotify user.");
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private void updateRefreshToken(String accessCode) {
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
                                    getApplicationContext(),
                                    j.getAsJsonObject().get("access_token").getAsString()
                            );
                            AppConfig.setRefreshToken(
                                    getApplicationContext(),
                                    j.getAsJsonObject().get("refresh_token").getAsString()
                            );
                            AppConfig.setExpiryTime(
                                    getApplicationContext(),
                                    System.currentTimeMillis() / 1000 + j.getAsJsonObject().get("expires_in").getAsLong()
                            );
                            getMe();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing refresh token response: " + e.getMessage());
                            addLog("Error parsing refresh token: "+e.getMessage());
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Error getting refresh token: " + error.getMessage());
                        Log.e(TAG, error.getUrl() + "\n" + error.getBody().toString());
                        addLog("Error getting refresh token: "+error.getMessage());
                    }
                });
    }

    private void logStatus(String message) {
        Log.d("SpotifyAPI", message);
    }

    private void updateChannelsList() {
        addLog("Loading channels...");
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
                            addLog("You do not have any channels. You can add channels by clicking the Menu button and selecting Add Channel.");
                        }

                        ChannelsAdapter adapter = new ChannelsAdapter(mContext, (ArrayList)channels);
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
                        addLog("Channels loaded, ready.");
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        addLog("Error loading channels: " + error.getMessage());
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

    public void addLog(String text) {
        if (mTextView!=null)
            mTextView.append(text+EOL);
    }
}
