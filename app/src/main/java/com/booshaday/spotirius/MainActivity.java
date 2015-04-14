package com.booshaday.spotirius;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.booshaday.spotirius.app.ApplicationController;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.data.SpotiriusChannel;
import com.booshaday.spotirius.data.SqlHelper;
import com.booshaday.spotirius.net.DogStarRadioClient;
import com.booshaday.spotirius.net.SpotifyClient;
import com.booshaday.spotirius.service.SyncService;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private final Context mContext = this;

    private SpotifyClient mSpotifyClient;
    private boolean mIsFirstLogin = false;
    private Map<String, String> mPlaylists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSpotifyClient = new SpotifyClient(getApplicationContext());

        // if user is not logged in, we need to auth them
        if (!AppConfig.isValidSession(getApplicationContext())) {
            // user is not currently logged in
            Toast.makeText(this, "Please log in to use Spotirius.", Toast.LENGTH_LONG).show();
            openLoginWindow();
            return;
        }

        // user has had a valid session, so try to use the
        // refresh token to authenticate
        mSpotifyClient.getAccessToken(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, response.toString(4));
                    if (response.has("access_token")) {
                        AppConfig.setAccessToken(getApplicationContext(), response.getString("access_token"));
                        if (response.has("expires_in")) {
                            AppConfig.setExpiryTime(getApplicationContext(), System.currentTimeMillis()/1000 + response.getLong("expires_in"));
                            updateChannelsList();
                        } else {
                            AppConfig.setExpiryTime(getApplicationContext(), 0);
                            updateChannelsList();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
                        openLoginWindow();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
                    openLoginWindow();
                }

            }
        }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show();
                    openLoginWindow();
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
                Toast.makeText(getApplicationContext(), "Sync started", Toast.LENGTH_SHORT).show();
                Intent syncIntent = new Intent(this, SyncService.class);
                startService(syncIntent);
                return true;

            case R.id.action_login:
                openLoginWindow();
                return true;

            case R.id.action_add_channel:
                DogStarRadioClient dsrc = new DogStarRadioClient(this);
                dsrc.addChannelByPicker();
                return true;

            case R.id.action_playlist_picker:
//                mSpotifyClient.getPlaylists(new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//                            Log.d(TAG, response.toString(4));
//
//                        } catch (Exception e) {
//                            Log.e(TAG, "Error parsing JSON response");
//                        }
//
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Log.e(TAG, new String(error.networkResponse.data));
//                    }
//                });
                mSpotifyClient.getPlaylists(new SpotifyClient.OnPlaylistComplete() {
                    @Override
                    public void onPlaylistComplete(Map<String, String> playlists) {
                        for (Map.Entry<String, String> item : playlists.entrySet()) {
                            Log.d(TAG, item.getKey() + ": " + item.getValue());
                        }
                    }
                });
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
                        break;

                    // Most likely auth flow was cancelled
                    default:
                        logStatus("Auth result: " + response.getType());
                        finish();
                        break;
                }
                break;

            case Constants.ADD_CHANNELS_RESULT:
                if (resultCode==0) return;

                final SqlHelper db = new SqlHelper(getApplicationContext());

                if (db.channelExists(String.valueOf(resultCode))) {
                    Toast.makeText(this, "Channel already exists.", Toast.LENGTH_LONG).show();
                    break;
                }

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
                                            Toast.makeText(mContext, item.getValue(), Toast.LENGTH_SHORT).show();
                                            SqlHelper db = new SqlHelper(getApplicationContext());
                                            db.addChannel(selectedChannel, item.getKey());
                                            db.close();
                                            Toast.makeText(getApplicationContext(), "Channel added", Toast.LENGTH_SHORT).show();
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
                String value = input.getText().toString();
                mSpotifyClient.createPlaylist(value, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("id")) {
                                SqlHelper db = new SqlHelper(getApplicationContext());
                                db.addChannel(String.valueOf(resultCode), response.getString("id"));
                                db.close();
                                Toast.makeText(getApplicationContext(), "Channel added", Toast.LENGTH_SHORT).show();
                                updateChannelsList();
                            } else {
                                Toast.makeText(getApplicationContext(), "Unable to determine playlist id", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "error parsing JSON response");
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, new String(error.networkResponse.data));
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
        mSpotifyClient.getMe(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("id")) {
                        AppConfig.setUsername(getApplicationContext(), response.getString("id"));
                    } else {
                        AppConfig.setUsername(getApplicationContext(), "");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, new String(error.networkResponse.data));
            }
        });
        mIsFirstLogin = false;
    }

    private void updateRefreshToken(String accessCode) {
        mSpotifyClient.getRefreshToken(accessCode, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Context context = getApplicationContext();
                    if (response.has("access_token")) {
                        AppConfig.setAccessToken(context, response.getString("access_token"));
                    } else {
                        AppConfig.setAccessToken(context, "");
                    }
                    if (response.has("refresh_token")) {
                        AppConfig.setRefreshToken(context, response.getString("refresh_token"));
                    } else {
                        AppConfig.setAccessToken(context, "");
                    }
                    if (response.has("expires_in")) {
                        AppConfig.setExpiryTime(context, System.currentTimeMillis()/1000 + response.getLong("expires_in"));
                    } else {
                        AppConfig.setExpiryTime(context, 0);
                    }
                    if (mIsFirstLogin) getMe();
                    else {
                        if (!AppConfig.isValidSession(getApplicationContext())) {
                            Toast.makeText(getApplicationContext(), "Spotirius credentials are invalid!", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, new String(error.networkResponse.data));
            }
        });
    }

    private void logStatus(String message) {
        Log.d("SpotifyAPI", message);
    }

    private void updateChannelsList() {
        mSpotifyClient.getPlaylists(new SpotifyClient.OnPlaylistComplete() {
            @Override
            public void onPlaylistComplete(Map<String, String> playlists) {
                mPlaylists = playlists;
                SqlHelper db = new SqlHelper(getApplicationContext());
                final ArrayList<SpotiriusChannel> channels = db.getChannels();
                for (SpotiriusChannel channel : channels) {
                    if (playlists.containsKey(channel.getPlaylist()))
                        channel.setPlaylistName(playlists.get(channel.getPlaylist()));
                }
                ChannelsAdapter adapter = new ChannelsAdapter(mContext, channels);
                final ListView listView = (ListView) findViewById(R.id.channelList);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,
                                            final int position, long id) {
                        new AlertDialog.Builder(mContext)
                            .setTitle("Remove Channel")
                            .setMessage("Are you sure you want to delete channel "+channels.get(position).getChannel()+"?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    SqlHelper db = new SqlHelper(getApplicationContext());
                                    if (db.deleteChannel(channels.get(position).getId()))
                                        updateChannelsList();
                                    else
                                        Toast.makeText(getApplicationContext(), "Unable to delete channel!", Toast.LENGTH_LONG).show();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                    }
                });


            }
        });


    }

    public class ChannelsAdapter extends ArrayAdapter<SpotiriusChannel> {
        public ChannelsAdapter(Context context, ArrayList<SpotiriusChannel> channels) {
            super(context, 0, channels);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            SpotiriusChannel channel = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.channel_row, parent, false);
            }
            // Lookup view for data population
            TextView title = (TextView) convertView.findViewById(R.id.item_title);
            TextView desc = (TextView) convertView.findViewById(R.id.item_desc);
            // Populate the data into the template view using the data object
            desc.setText("Satellite channel "+channel.getChannel());
            title.setText(channel.getPlaylistName());
            // Return the completed view to render on screen
            return convertView;
        }
    }
}
