package com.booshaday.spotirius.service;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.MainActivity;
import com.booshaday.spotirius.R;
import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Constants;
import com.booshaday.spotirius.data.Song;
import com.booshaday.spotirius.net.RestClient;
import com.booshaday.spotirius.view.ChannelManagerActivity;
import com.booshaday.spotirius.view.ChannelPickerActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

/**
 * Created by chris on 5/29/15.
 */
public class SyncService {

    private static final Pattern REGEX_CHANNELS = Pattern.compile("<select name=channel>(.*?)<\\/select><\\/td>");
    private static final Pattern REGEX_CHANNELS_OPTIONS = Pattern.compile("<option value=\"(\\d+)\">([^<]+)");
    private static final Pattern REGEX_CHANNEL_NEXT_PAGE = Pattern.compile(".*<a href=(.*)>Next<br>Page<\\/a>.*");
    private static final Pattern REGEX_CHANNEL_NEXT_PAGE_NUMBER = Pattern.compile("&page=(\\d+)");
    private static final Pattern REGEX_CHANNEL_SONGS = Pattern.compile("<tr><td>(\\d+)<\\/td><td>(.*)<\\/td><td><a.*\">(.*)<\\/a><\\/td><td>\\d+\\/\\d+\\/\\d+<\\/td><td>\\d+\\:\\d+\\:\\d+ [A|P]M<\\/td><\\/tr>");
    private static final Pattern REGEX_NEXT_PAGE = Pattern.compile(".*<a href=(.*)>Next<br>Page<\\/a>.*");
    private static final Pattern REGEX_SONG_LIST = Pattern.compile("<tr><td>(\\d+)<\\/td><td>(.*)<\\/td><td><a.*\">(.*)<\\/a><\\/td><td>\\d+\\/\\d+\\/\\d+<\\/td><td>\\d+\\:\\d+\\:\\d+ [A|P]M<\\/td><\\/tr>");
    private static final Pattern REGEX_TRACK_URI = Pattern.compile(".*\\\"uri\\\" \\: \\\"(spotify\\:track:.*)\\\".*");
    private static final Pattern REGEX_TITLE_WITH_YEAR = Pattern.compile("^(.*?)(\\ \\(\\d+\\))?$");
    private static final Pattern REGEX_PAGE_OFFSET = Pattern.compile("offset=(\\d+)");
    private static final String TAG = "SyncService";
    private static final int SPOTIFY_RATELIMIT_MILLIS = 100;
    private static final int SPOTIFY_TRACK_ADD_BATCH_SIZE = 100;

    private Context mContext;
    private Intent mIntent;
    private boolean finished = false;
    private RestClient mSpotify;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    public SyncService(Context context, NotificationManager manager, NotificationCompat.Builder builder) {
        this.mContext = context;
        this.mNotificationManager = manager;
        this.mBuilder = builder;
    }

    public JsonElement addTracks(String playlistId, JsonObject uris) {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        return client.addTracks(AppConfig.getUsername(mContext), playlistId, uris);
    }

    public JsonElement createPlaylist(String name, Boolean isPublic) {
        if (isPublic==null) isPublic = false;
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        return client.createPlaylist(AppConfig.getUsername(mContext), name, isPublic);
    }

    public Map<String, String> getChannels() throws Exception {
        RestClient.DogStarRadio client = RestClient.create(
                RestClient.DogStarRadio.class,
                RestClient.DogStarRadio.URL,
                null
        );
        Response response = client.getChannels();
        String body = new String(((TypedByteArray) response.getBody()).getBytes());

        // extract the channels
        Matcher m = REGEX_CHANNELS.matcher(body);

        String result;
        Map<String, String> channels = new HashMap<>();

        // see if we matched the channels option menu
        if (m.find()) {
            // get the channels out
            result = m.group(1);
            m = REGEX_CHANNELS_OPTIONS.matcher(result);
            while (m.find()) {
                if (m.groupCount()>1) {
                    try {
                        channels.put(m.group(1), m.group(2));
                    } catch (Exception e) {Log.e(TAG, "Unable to parse channel: "+m.group(1));}
                }
            }
        } else {
            throw new Exception("Unable to get channel list");
        }

        return channels;
    }

    public static void addChannelByPicker(final Context context) {
        RestClient.DogStarRadio client = RestClient.create(
                RestClient.DogStarRadio.class,
                RestClient.DogStarRadio.URL,
                null
        );
        client.getChannelsAsync(new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                String body = new String(((TypedByteArray) response.getBody()).getBytes());

                // extract the channels
                Matcher m = REGEX_CHANNELS.matcher(body);

                String result;
                ArrayList<String> channels = new ArrayList<>();
                ArrayList<String> descriptions = new ArrayList<>();

                // see if we matched the channels option menu
                if (m.find()) {
                    // get the channels out
                    result = m.group(1);
                    m = REGEX_CHANNELS_OPTIONS.matcher(result);
                    while (m.find()) {
                        if (m.groupCount() > 1) {
                            try {
                                channels.add(m.group(1));
                                descriptions.add(m.group(2));
                            } catch (Exception e) {
                                Log.e(TAG, "Unable to parse channel: " + m.group(1));
                            }
                        }
                    }

                    if (!channels.isEmpty()) {
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("descriptions", descriptions);
                        bundle.putStringArrayList("channels", channels);
                        Intent intent = new Intent(context, ChannelPickerActivity.class);
                        intent.putExtras(bundle);
                        ((ChannelManagerActivity) context).startActivityForResult(intent, Constants.ADD_CHANNELS_RESULT);
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(context, "Channel lookup failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void refreshToken() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.ACCOUNTS_URL,
                null
        );
        JsonElement j = client.getRefreshToken(
                Constants.SPOTIFY_CLIENT_ID,
                Constants.SPOTIFY_CLIENT_SECRET,
                "refresh_token",
                AppConfig.getRefreshToken(mContext));

        AppConfig.setAccessToken(
                mContext,
                j.getAsJsonObject().get("access_token").getAsString()
        );
        AppConfig.setExpiryTime(
                mContext,
                System.currentTimeMillis() / 1000 + j.getAsJsonObject().get("expires_in").getAsLong()
        );
    }

    public List<Song> getChannelPlaylist(List<Song> playlist, int month, int date, String channel, String playlistId) {
        RestClient.DogStarRadio client = RestClient.create(
                RestClient.DogStarRadio.class,
                RestClient.DogStarRadio.URL,
                null
        );

        RestClient.Spotify spotify = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );

        int nextPage = 1;
        if (playlist==null) {
            playlist = new ArrayList<>();
        }

        List<String> newTracks = new ArrayList<>();

        // loop until we quit receiving next page
        while (nextPage > 0) {
            Log.d(TAG, String.format("Processing playlists for channel: %s, page: %d", channel, nextPage));
            updateHeading(String.format("Syncing Channel: %s", channel));
            Response response = client.getPlaylist("", "", channel, month, date, "", "", "", "", "", nextPage);
            if (response.getStatus()!=200) {
                updateNotification("Error: Received http "+String.valueOf(response.getStatus())+" trying to read from "+response.getUrl());
            }
            String body = new String(((TypedByteArray) response.getBody()).getBytes());
            updateNotification("Loading songs from DogStarRadio...");

            // see if we have another page to process
            Matcher m = REGEX_CHANNEL_NEXT_PAGE.matcher(body);

            // if we have another page, update nextPage
            if (m.find()) {
                Matcher pageMatch = REGEX_CHANNEL_NEXT_PAGE_NUMBER.matcher(m.group(1));
                if (!pageMatch.find()) {
                    Log.e(TAG, "found next page, but could not determine number: "+m.group(1));
                    nextPage = 0;
                }
                nextPage = Integer.valueOf(pageMatch.group(1));
                pageMatch = null;
            } else {
                nextPage = 0;
            }

            // scrape songs off the page
            m = REGEX_CHANNEL_SONGS.matcher(body);
            while (m.find()) {
                String title = m.group(3);

                // if the title of the track contains the year at the end, remove it
                Matcher removeYearFromTitle = REGEX_TITLE_WITH_YEAR.matcher(m.group(3));
                if (removeYearFromTitle.find()) {
                    title = removeYearFromTitle.group(1);
                }

                Song s = new Song(m.group(2), title);

                // deduplicate
                boolean found = false;
                for (Song song : playlist) {
                    if (song.getArtist().equalsIgnoreCase(s.getArtist()) && song.getTitle().equalsIgnoreCase(s.getTitle())) {
                        found = true;
                        break;
                    }
                }

                // process song
                if (!found) {
                    Log.d(TAG, String.format("Looking up track: %s - %s", s.getArtist(), s.getTitle()));
                    try {
                        final boolean strictSearch = AppConfig.getStrictSearchEnabled(mContext);
                        String params;

                        if (strictSearch) {
                            params = "\"" + s.getTitle() + "\"+artist:\"" + s.getArtist() + "\"";
                        } else {
                            params = s.getTitle() + "+artist:" + s.getArtist();
                        }

                        JsonElement t = spotify.getSearchResults(params, "track", "from_token", 1);
                        JsonObject o = t.getAsJsonObject();
                        s.setUri(o.get("tracks").getAsJsonObject().get("items").getAsJsonArray().get(0).getAsJsonObject().get("uri").getAsString());

                        // try to deduplicate again
                        for (Song song : playlist) {
                            if (song.getUri().equals(s.getUri())) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            Log.d(TAG, String.format("FOUND: artist: %s, track: %s, uri: %s", s.getArtist(), s.getTitle(), s.getUri()));
                            updateNotification(String.format("Found %s - %s", s.getArtist(), s.getTitle()));

                            // add to playlist for de-duplication
                            playlist.add(s);

                            // add to new tracks queue
                            newTracks.add(s.getUri());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, String.format("NOT FOUND: artist: %s, track: %s", s.getArtist(), s.getTitle()));
                        updateNotification("Not found: "+s.getArtist()+" - "+s.getTitle());
                    } finally {
                        // pause to help with rate limiting
                        try {
                            Thread.sleep(SPOTIFY_RATELIMIT_MILLIS);
                        } catch (InterruptedException e) {}
                    }

                    // check to see if we need to publish tracks to the playlist
                    if (newTracks.size() == SPOTIFY_TRACK_ADD_BATCH_SIZE) {
                        publishTracksToPlaylist(newTracks, playlistId);
                        // empty array list
                        newTracks.clear();
                    }
                } else {
                    Log.d(TAG, "Found duplicate song: "+s.getArtist()+" - "+s.getTitle());
                    updateNotification("Found duplicate song: "+s.getArtist()+" - "+s.getTitle());
                }
            }
        }

        // push any existing songs to spotify
        if (newTracks.size()>0) {
            updateNotification("Adding tracks to playlist...");
            publishTracksToPlaylist(newTracks, playlistId);
            // empty array list
            newTracks.clear();
        }

        Log.d(TAG, String.format("FINISHED PLAYLIST PARSE: %s", channel));
        updateHeading(String.format("Finished: Channel %s", channel));
        return playlist;
    }

    private void publishTracksToPlaylist(List<String> newTracks, String playlistId) {
        JsonObject tracks = new JsonObject();
        JsonArray trackList = new JsonArray();
        for (String uri : newTracks) {
            trackList.add(new JsonPrimitive(uri));
        }
        tracks.add("uris", trackList);

        // send to playlist
        try {
            addTracks(playlistId, tracks);
            Log.d(TAG, String.format("Added %d tracks to %s", newTracks.size(), playlistId));
        } catch (Exception e) {
            Log.e(TAG, "Error submitting tracks to playlist");
            e.printStackTrace();
        } finally {
            // pause to help with rate limiting
            try {
                Thread.sleep(SPOTIFY_RATELIMIT_MILLIS);
            } catch (InterruptedException e) {}
        }
    }

    public List<Song> getPlaylistTracks(String playlistId) {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );

        List<Song> playlist = new ArrayList<>();

        final int limit = 100;
        int offset = 0;

        while (offset > -1) {
            Log.d(TAG, "Parsing playlist offset: "+String.valueOf(offset));
            try {
                JsonElement j = client.getPlaylistTracks(
                        AppConfig.getUsername(mContext),
                        playlistId,
                        limit,
                        offset
                );

                if (j==null) throw new Exception("Received null response");

                try {
                    if (j.getAsJsonObject().get("next").isJsonNull()) {
                        Log.d(TAG, "Reached end of playlist tracks pages");
                        offset = -1;
                    } else {
                        String next = j.getAsJsonObject().get("next").getAsString();
                        Matcher nextOffset = REGEX_PAGE_OFFSET.matcher(next);
                        if (nextOffset.find()) {
                            offset = Integer.valueOf(nextOffset.group(1));
                            if (offset==0) offset = -1;
                            if (offset>-1) Log.d(TAG, "Found next offset: " + String.valueOf(offset));
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Caught exception looking for next page");
                    offset = -1;
                }

                try {
                    JsonArray a = j.getAsJsonObject().get("items").getAsJsonArray();
                    for (final JsonElement element : a) {
                        try {
                            JsonObject track = element.getAsJsonObject().get("track").getAsJsonObject();
                            Song s = new Song(
                                    track.get("artists").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                                    track.get("name").getAsString(),
                                    track.get("uri").getAsString()
                            );

                            playlist.add(s);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing item");
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error looping through items");
                }
            } catch (Exception e) {
                e.printStackTrace();
                // escape while loop
                offset = -1;
            }
        }

        return playlist;
    }

    public JsonElement getMe() {
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        return client.getMe();
    }

    public JsonElement getPlaylists(Integer limit) {
        // TODO: iterate through pages returned
        if (limit==null) limit = 50;
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        return client.getPlaylists(AppConfig.getUsername(mContext), limit);
    }

    /**
     * Gets a Spotify refresh token
     * @param accessCode = access code provided by Spotify SDK
     */
    public JsonElement getRefreshToken(final String accessCode) {
        RestClient.Spotify token = RestClient.create(RestClient.Spotify.class, RestClient.Spotify.ACCOUNTS_URL, null);
        return token.getAccessToken(Constants.SPOTIFY_CLIENT_ID, Constants.SPOTIFY_CLIENT_ID, "authorization_code", accessCode, Constants.SPOTIFY_REDIRECT_URI);
    }

    public JsonElement getSearchResults(String parameters, String type, String market, Integer limit) {
        if (limit==null) limit = 1;
        if (market==null) market = "from_token";
        if (type==null) type = "track";
        RestClient.Spotify client = RestClient.create(
                RestClient.Spotify.class,
                RestClient.Spotify.API_URL,
                "Bearer " + AppConfig.getAccessToken(mContext)
        );
        return client.getSearchResults(parameters, type, market, limit);
    }

    private void updateNotification(String text) {
        if (mNotificationManager!=null && mBuilder!=null) {
            mBuilder.setContentText(text);
            mNotificationManager.notify(SyncIntentService.NOTIFICATION_ID, mBuilder.build());
        }

        // try to update the gui
        try {
            Intent intent = new Intent();
            intent.setAction("com.booshaday.spotirius.SyncProgress");
            intent.putExtra("syncStatus", text);
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHeading(String text) {
        // try to update the gui
        try {
            Intent intent = new Intent();
            intent.setAction("com.booshaday.spotirius.SyncProgress");
            intent.putExtra("syncHeading", text);
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
