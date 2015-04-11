package com.booshaday.spotirius.net;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.booshaday.spotirius.R;
import com.booshaday.spotirius.app.ApplicationController;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.data.SpotiriusChannel;
import com.booshaday.spotirius.data.SqlHelper;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chris on 3/14/15.
 */
public class SpotifyClient {
    private static final String TAG = "SpotifyClient";
    private static final String SPOTIFY_API = "https://api.spotify.com/v1";
    private static final String SPOTIFY_ME = "/me";
    private static final String SPOTIFY_USER = "/users";
    private static final String SPOTIFY_PLAYLISTS = "/playlists";
    private static final String SPOTIFY_SEARCH = "/search";
    private static final String SPOTIFY_AUTHORIZE = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN = "https://accounts.spotify.com/api/token";

    private Context mContext;

    public SpotifyClient(Context context) {
        this.mContext = context.getApplicationContext();
    }

    /**
     * Gets the access token using a valid refresh token
     * @param success = Response.Listener callback
     * @param failure = Response.ErrorListener callback
     */
    public void getAccessToken(Response.Listener success, Response.ErrorListener failure) {
        String url = SPOTIFY_TOKEN;

        Map<String, String> params = new HashMap<>();
        params.put("client_id", Constants.SPOTIFY_CLIENT_ID);
        params.put("client_secret", Constants.SPOTIFY_CLIENT_SECRET);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", AppConfig.getRefreshToken(mContext));

        // set up request
        FormJsonObjectRequest req = new FormJsonObjectRequest(Request.Method.POST, url, params, success, failure) ;

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }


    /**
     * Gets a Spotify refresh token
     * @param accessCode = access code provided by Spotify SDK
     * @param success = Response.Listener callback
     * @param failure = Response.ErrorListener callback
     */
    public void getRefreshToken(final String accessCode, Response.Listener success, Response.ErrorListener failure) {
        String url = SPOTIFY_TOKEN;

        Log.d(TAG, "Using code: "+accessCode);

        Map<String, String> params = new HashMap<String, String>();
        params.put("client_id", Constants.SPOTIFY_CLIENT_ID);
        params.put("client_secret", Constants.SPOTIFY_CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", accessCode);
        params.put("redirect_uri", Constants.SPOTIFY_REDIRECT_URI);

        // set up request
        FormJsonObjectRequest req = new FormJsonObjectRequest(Request.Method.POST, url, params, success, failure);

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    /**
     * Uses the active authtoken to get the current Spotify user name
     * @param success = Response.Listener callback
     * @param failure = Response.ErrorListener callback
     */
    public void getMe(Response.Listener success, Response.ErrorListener failure) {
        String url = SPOTIFY_API + SPOTIFY_ME;

        // set up request
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, success, failure) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+AppConfig.getAccessToken(mContext));

                return params;
            }
        };

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    public void getPlaylists(Response.Listener success, Response.ErrorListener failure) {
        if (!AppConfig.isValidSession(mContext)) {
            failure.onErrorResponse(new VolleyError("Invalid Spotify session data"));
            return;
        }

        String url = SPOTIFY_API
                + SPOTIFY_USER
                + "/" + AppConfig.getUsername(mContext)
                + SPOTIFY_PLAYLISTS;

        // set up request
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, success, failure) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+AppConfig.getAccessToken(mContext));

                return params;
            }
        };

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    /**
     * Searches Spotify to a track, and adds it to the appropriate
     * playlist if the track is found.
     * @param dbId = database id of the song
     * @param channel = SpotiriusChannel of the target
     * @param artist = track artist
     * @param title = track title
     */
    public void addSongIfFound (final long dbId, final SpotiriusChannel channel, final String artist, final String title) {
        String url = null;
        try {
            url = SPOTIFY_API + SPOTIFY_SEARCH
                    + "?q=" + String.format("%s+artist:%s", URLEncoder.encode(title, Constants.URL_ENCODING), URLEncoder.encode(artist, Constants.URL_ENCODING))
                    + "&type=track"
                    + "&market=from_token"
                    + "&limit=1";
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Song lookup failed: unable to build query parameters");
            SqlHelper db = new SqlHelper(mContext.getApplicationContext());
            db.deleteSong(dbId);
        }
//        Uri.Builder builder = Uri.parse(url).buildUpon();

//        try {
//            builder.appendQueryParameter("q", String.format("%s+artist:%s", URLEncoder.encode(title, Constants.URL_ENCODING), URLEncoder.encode(artist, Constants.URL_ENCODING)));
//            builder.appendQueryParameter("type", "track");
//            builder.appendQueryParameter("market", "from_token");
//            builder.appendQueryParameter("limit", "1");
//        } catch (UnsupportedEncodingException e) {
//            Log.d(TAG, "Song lookup failed: unable to build query parameters");
//            SqlHelper db = new SqlHelper(mContext.getApplicationContext());
//            db.deleteSong(dbId);
//        }

        StringRequest req = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                SqlHelper db = new SqlHelper(mContext.getApplicationContext());

                Pattern re = Pattern.compile(".*\\\"uri\\\" \\: \\\"(spotify\\:track:.*)\\\".*");
                Matcher m = re.matcher(new String(response.getBytes()));
                if (m.find()) {
                    Log.d(TAG, "Found Spotify track: " + m.group(1));
                    db.updateUri(dbId, m.group(1));

                    // add song to queue
                    queueSong(dbId, channel, m.group(1), 0);
                } else {
                    //Log.d(TAG, "Song lookup failed (not found in response) "+new String(response.getBytes()));
                    Log.d(TAG, "Song lookup failed (not found in response)");
                    db.deleteSong(dbId);
                }

                db.close();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                SqlHelper db = new SqlHelper(mContext.getApplicationContext());
                db.deleteSong(dbId);
                db.close();
                Log.d(TAG, "Error adding song: "+new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+AppConfig.getAccessToken(mContext));

                return params;
            }
        };

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    private void queueSong(final long dbId, final SpotiriusChannel channel, final String spotifyUri, final int attempt) {
        if (attempt < 5) {
            addSong(channel.getPlaylist(), spotifyUri, false, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Added track");
                    SqlHelper db = new SqlHelper(mContext);
                    db.setSongLoaded(dbId, true);
                    db.close();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.networkResponse.statusCode==500) {
                        Log.d(TAG, "spotify API returned 500 for track: "+spotifyUri+" (attempt "+String.valueOf(attempt)+")");
                        queueSong(dbId, channel, spotifyUri, attempt+1);
                    } else {
                        Log.e(TAG, new String(error.networkResponse.data));
                        SqlHelper db = new SqlHelper(mContext);
                        db.deleteSong(dbId);
                        db.close();
                    }
                }
            });
        } else {
            Log.e(TAG, "Failed to add "+spotifyUri+" to playlist: "+channel.getPlaylist()+", removing id: "+String.valueOf(dbId));
            SqlHelper db = new SqlHelper(mContext);
            db.deleteSong(dbId);
            db.close();
        }

    }

    public void addSong (String playlistId, String trackUri, boolean atFront, Response.Listener success, Response.ErrorListener failure) {
        String url = "";
        try {
            url = SPOTIFY_API
                    + SPOTIFY_USER
                    + "/" + AppConfig.getUsername(mContext)
                    + SPOTIFY_PLAYLISTS
                    + "/" + URLEncoder.encode(playlistId, Constants.URL_ENCODING)
                    + "/tracks?uris=" + URLEncoder.encode(trackUri, Constants.URL_ENCODING);
            if (atFront) url += "&position=0";
        } catch (Exception e) {
            failure.onErrorResponse(new VolleyError("Unable to generate URL"));
        }

        // set up request
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, success, failure) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+AppConfig.getAccessToken(mContext));
                params.put("Accept", "application/json");

                return params;
            }
        };

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    public void createPlaylist(String playlistName, Response.Listener success, Response.ErrorListener failure) {
        // create json payload
        JSONObject json = new JSONObject();
        try {
            json.put("name", playlistName);
            json.put("public", false);
        } catch (Exception e) {}

        // generate url
        String url = SPOTIFY_API
                + SPOTIFY_USER
                + "/" + AppConfig.getUsername(mContext)
                + SPOTIFY_PLAYLISTS;

        // set up request
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, json, success, failure) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer "+AppConfig.getAccessToken(mContext));
                params.put("Content-Type", "application/json");

                return params;
            }
        };

        // add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }
}
