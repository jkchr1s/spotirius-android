package com.booshaday.spotirius.data;

import com.android.volley.DefaultRetryPolicy;

/**
 * Created by chris on 3/14/15.
 */
public class Constants {
    public static final String SPOTIFY_CLIENT_ID = SpotifyClientConfig.SPOTIFY_CLIENT_ID;
    public static final String SPOTIFY_CLIENT_SECRET = SpotifyClientConfig.SPOTIFY_CLIENT_SECRET;
    public static final String SPOTIFY_REDIRECT_URI = SpotifyClientConfig.SPOTIFY_REDIRECT_URI;
    public static final String URL_ENCODING = "UTF-8";
    public static final DefaultRetryPolicy RETRY_POLICY = new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    public static final int ADD_CHANNELS_RESULT = 1001;
    public static final int SPOTIFY_AUTH_RESULT = 1002;
}
