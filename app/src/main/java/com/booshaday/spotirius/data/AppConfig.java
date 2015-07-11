package com.booshaday.spotirius.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.booshaday.spotirius.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by chris on 3/21/15.
 */
public final class AppConfig {
    public static final String SPOTIFY_API = "https://api.spotify.com/v1";
    public static final String SPOTIFY_ME = "/me";
    public static final String SPOTIFY_USER = "/users";
    public static final String SPOTIFY_PLAYLISTS = "/playlists";
    public static final String SPOTIFY_SEARCH = "/search";
    public static final String SPOTIFY_AUTHORIZE = "https://accounts.spotify.com/authorize";
    public static final String SPOTIFY_TOKEN = "https://accounts.spotify.com/api/token";
    public static final String DOG_BASE_URL = "http://www.dogstarradio.com";
    public static final String DOG_SEARCH_URI = "/search_playlist.php";
    public static final String DOG_SEARCH_ARGS = "?artist=&title=&channel=%s&month=%d&date=%d&shour=&sampm=&stz=&ehour=&eampm=";
    private static final String TAG = "AppConfig";


    public static String getSpotifyClientAuth() {
        String auth = Constants.SPOTIFY_CLIENT_ID + ":" + Constants.SPOTIFY_CLIENT_SECRET;
        try {
            return "Basic " + Base64.encodeToString(auth.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        return prefs.getString("spotify_user", "");
    }

    public static void setUsername(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("spotify_user", username);
        editor.commit();
    }

    public static void setAccessToken(Context context, String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("access_token", accessToken);
        editor.commit();
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        return prefs.getString("access_token", "");
    }

    public static boolean getStrictSearchEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        return prefs.getBoolean("strict_search", true);
    }

    public static void setStrictSearchEnabled(Context context, Boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("strict_search", enabled);
        editor.commit();
    }

    public static List<Channel> getChannels(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        String json = prefs.getString("channels", "");
        List<Channel> channels;
        if (!json.isEmpty()) {
            Type type = new TypeToken<List<Channel>>(){}.getType();
            channels = new Gson().fromJson(json, type);
        } else {
            channels = new ArrayList<>();
        }
        return channels;
    }

    public static void addChannel(Context context, Channel channel) {
        if (channel==null) return;
        List<Channel> channels = getChannels(context);

        // prevent duplicate channels
        for (Channel c : channels) {
            if (c.getChannel().equals(channel.getChannel())) {
                return;
            }
        }
        channels.add(channel);
        String json = new Gson().toJson(channels);

        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("channels", json);
        editor.commit();
    }

    public static void deleteChannel(Context context, String channel) {
        if (channel==null) return;
        List<Channel> channels = getChannels(context);
        if (channels!=null && !channels.isEmpty()) {
            for (int i=0; i<channels.size(); i++) {
                if (channels.get(i).getChannel().equals(channel)) {
                    channels.remove(i);
                }
            }
        }
        String json = new Gson().toJson(channels);

        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("channels", json);
        editor.commit();
    }

    public static void updateChannelSyncTimestamp(Context context, String channel) {
        if (channel==null) return;
        Log.d(TAG, "Updating channel sync timestamp: "+channel);

        List<Channel> channels = getChannels(context);

        for (Channel c : channels) {
            if (c.getChannel().equals(channel)) {
                c.setLastSync(System.currentTimeMillis());
            }
        }

        String json = new Gson().toJson(channels);

        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("channels", json);
        editor.commit();
    }

    public static void setRefreshToken(Context context, String refreshToken) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("refresh_token", refreshToken);
        editor.commit();
    }

    public static String getRefreshToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        return prefs.getString("refresh_token", "");
    }

    public static void setExpiryTime(Context context, long expirationTime) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("expiry_time", expirationTime);
        editor.commit();
    }

    public static long getExpiryTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        return prefs.getLong("expiry_time", 0);
    }

    public static boolean tokenNeedsRefresh(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        long expiry = prefs.getLong("expiry_time", 0);
        return System.currentTimeMillis()/1000 < expiry;
    }

    public static boolean isValidSession(Context context) {
        boolean valid = true;
        SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.prefs_key), Context.MODE_PRIVATE);
        if (prefs.getString("access_token", "").isEmpty()) valid = false;
        if (prefs.getString("refresh_token", "").isEmpty()) valid = false;
        if (prefs.getString("spotify_user", "").isEmpty()) valid = false;
        if (!valid) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("access_token", "");
            editor.putString("refresh_token", "");
            editor.putString("spotify_user", "");
            editor.putLong("expiry_time", 0);
            editor.commit();
        }
        return valid;
    }
}
