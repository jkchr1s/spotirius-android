package com.booshaday.spotirius.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.booshaday.spotirius.R;

import java.util.Set;

/**
 * Created by chris on 3/21/15.
 */
public final class AppConfig {

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
