package com.booshaday.spotirius.net;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.booshaday.spotirius.MainActivity;
import com.booshaday.spotirius.R;
import com.booshaday.spotirius.app.ApplicationController;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.data.SpotiriusChannel;
import com.booshaday.spotirius.data.SqlHelper;
import com.booshaday.spotirius.view.ChannelPickerActivity;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chris on 3/14/15.
 */
public class DogStarRadioClient {
    private static final String BASE_URL = "http://www.dogstarradio.com";
    private static final String SEARCH_URI = "/search_playlist.php";
    private static final String SEARCH_ARGS = "?artist=&title=&channel=%s&month=%d&date=%d&shour=&sampm=&stz=&ehour=&eampm=";
    private static final String SPOTIFY_API = "https://api.spotify.com/v1";
    private static final String TAG = "DogStarRadioClient";
    private static final int POLL_INTERVAL = 5000;

    private Context mContext;
    private Intent mIntent;
    private boolean finished = false;

    /**
     * If accessing from GUI, pass the context of Activity
     * If running as service, add Application context!
     * @param context
     */
    public DogStarRadioClient(Context context) {
        this.mContext = context;
    }

    /**
     * Scrapes dogstarradio to get the channel numbers and launches
     * an activity that lets user pick a channel to add
     */
    public void addChannelByPicker() {
        String url = BASE_URL + SEARCH_URI;

        StringRequest req = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                ArrayList<String> channels = new ArrayList<>();
                ArrayList<String> descriptions = new ArrayList<>();

                String result = new String(response.getBytes());

                // extract the channels
                Pattern re = Pattern.compile("<select name=channel>(.*?)<\\/select><\\/td>");
                Matcher m = re.matcher(result);

                // see if we matched the channels option menu
                if (m.find()) {
                    // get the channels out
                    result = m.group(1);
                    re = Pattern.compile("<option value=\"(\\d+)\">([^<]+)");
                    m = re.matcher(result);
                    while (m.find()) {
                        if (m.groupCount()>1) {
                            try {
                                channels.add(m.group(1));
                                descriptions.add(m.group(2));
                            } catch (Exception e) {Log.e(TAG, "Unable to parse channel: "+m.group(1));}
                        }
                    }

                    if (!channels.isEmpty()) {
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("descriptions", descriptions);
                        bundle.putStringArrayList("channels", channels);
                        Intent intent = new Intent(mContext, ChannelPickerActivity.class);
                        intent.putExtras(bundle);
                        ((MainActivity)mContext).startActivityForResult(intent, Constants.ADD_CHANNELS_RESULT);
                    }
                } else {
                    Log.e(TAG, "channel lookup failed");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Channel lookup failed!", Toast.LENGTH_LONG).show();
            }
        });

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    public ArrayList<SpotiriusChannel> getChannels() {
        SqlHelper db = new SqlHelper(mContext.getApplicationContext());
        return db.getChannels();
    }

    public void sendStopSignal() {
        // cancel http requests
        ApplicationController.getInstance().cancelPendingRequests(ApplicationController.TAG);

        // delete incomplete songs
        if (mContext.getApplicationContext()!=null) {
            SqlHelper db = new SqlHelper(mContext.getApplicationContext());
        }

    }

    public void sync(Intent intent) {
        mIntent = intent;
        SpotifyClient client = new SpotifyClient(mContext.getApplicationContext());
        if (!AppConfig.isValidSession(mContext)) {
            Log.d(TAG+"_init", "no valid session found");
            Toast.makeText(mContext, mContext.getString(R.string.new_session), Toast.LENGTH_LONG).show();
            return;
        }

        // user has had a valid session, so try to use the
        // refresh token to authenticate
        client.getAccessToken(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // try to refresh the token
                try {
                    if (response.has("access_token")) {
                        // we got a new access token
                        // so far so good...
                        Log.d(TAG + "_init", "got access token");
                        AppConfig.setAccessToken(mContext, response.getString("access_token"));
                        if (response.has("expires_in")) {
                            // we got the token expiration time
                            // let's get started
                            Log.d(TAG + "_init", "got token expiration time");
                            AppConfig.setExpiryTime(mContext, System.currentTimeMillis() / 1000 + response.getLong("expires_in"));
                            Log.d(TAG + "_init", "Spotify client initialized successfully");
                            startSync();
                        } else {
                            // we did not receive an expiration time, prompt user to launch app
                            // and log in again
                            AppConfig.setExpiryTime(mContext, 0);
                            Toast.makeText(mContext, mContext.getString(R.string.session_problem), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // the response does not have an access token
                        // ask user to launch app to log in again
                        Toast.makeText(mContext, mContext.getString(R.string.session_problem), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    // there was a general exception, ask the user
                    // to launch the app to log in again
                    Toast.makeText(mContext, mContext.getString(R.string.session_expired), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // we did not receive an expiration time, prompt user to launch app
                // and log in again
                AppConfig.setExpiryTime(mContext, 0);
                Toast.makeText(mContext, mContext.getString(R.string.session_problem), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startSync() {
        // get data from yesterday
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);

        // loop through channels and add the urls
        ArrayList<SpotiriusChannel> channels = getChannels();

        if (channels!=null) {
            for (SpotiriusChannel channel : channels) {
                // add the request object to the queue to be executed

                StringRequest req = new StringRequest(
                        BASE_URL+SEARCH_URI+String.format(SEARCH_ARGS, channel.getChannel(), c.get(Calendar.MONTH)+1, c.get(Calendar.DATE)),
                        ChannelResponse,
                        ChannelErrorResponse
                );
                req.setRetryPolicy(Constants.RETRY_POLICY);
                ApplicationController.getInstance().addToRequestQueue(req);
            }
        }

        Log.d(TAG, "Starting service monitor...");
        //Declare the timer
        final Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {

                  @Override
                  public void run() {
                      //Called each time when 1000 milliseconds (1 second) (the period parameter)
                      if (ApplicationController.getInstance().isEmpty()) {
                          Log.d(TAG, "Queue completed, stopping service");
                          t.cancel();
                          if (mContext!=null && mIntent!=null) {
                              mContext.stopService(mIntent);
                          }
                      }
                  }

              },
            POLL_INTERVAL,
            POLL_INTERVAL);
    }

    private Response.ErrorListener ChannelErrorResponse = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            VolleyLog.e("Error: ", error.getMessage());
            error.printStackTrace();
        }
    };

    private Response.Listener ChannelResponse = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            // get database instance
            SqlHelper db = new SqlHelper(mContext.getApplicationContext());

            final SpotifyClient client = new SpotifyClient(mContext);

            // see if we have another page to process
            Pattern re = Pattern.compile(".*<a href=(.*)>Next<br>Page<\\/a>.*");
            Matcher m = re.matcher(response);

            // if we have another page, add to the url queue
            if (m.find()) {
                Log.d(TAG, "found another page: "+m.group(1));

                // add the request object to the queue to be executed

                ApplicationController.getInstance().addToRequestQueue(new StringRequest(
                        String.format(BASE_URL+"/"+m.group(1)),
                        ChannelResponse,
                        ChannelErrorResponse
                ).setRetryPolicy(Constants.RETRY_POLICY));

            }

            // scrape songs off the page
            re = Pattern.compile("<tr><td>(\\d+)<\\/td><td>(.*)<\\/td><td><a.*\">(.*)<\\/a><\\/td><td>\\d+\\/\\d+\\/\\d+<\\/td><td>\\d+\\:\\d+\\:\\d+ [A|P]M<\\/td><\\/tr>");
            m = re.matcher(response);
            while (m.find()) {
                Log.v("Songs", String.format("Found song. channel: %s, artist: %s, title: %s", m.group(1), m.group(2), m.group(3)));
                final long id = db.addSong(Integer.parseInt(m.group(1)), m.group(2), m.group(3));
                if (id>0) {
                    Log.v("SyncTask", String.format("Found new song: channel: %s, artist: %s, title: %s, dbId: %d", m.group(1), m.group(2), m.group(3), id));

                    SpotiriusChannel channel = db.getChannel(m.group(1));
                    if (channel!=null) {
                        client.addSongIfFound(id, db.getChannel(m.group(1)), m.group(2), m.group(3));
                    } else {
                        Log.e(TAG, "SpotiriusChannel lookup failed, deleting from db");
                        db.deleteSong(id);
                    }
                }
            }

            db.close();
        }
    };
}
