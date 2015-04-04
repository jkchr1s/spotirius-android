package com.booshaday.spotirius.net;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.booshaday.spotirius.data.SongItem;
import com.booshaday.spotirius.data.SqlHelper;
import com.google.common.base.Charsets;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.Header;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chris on 1/27/15.
 */

public class SiriusChannel {
    protected static Context context;
    private final String baseUrl = "http://www.dogstarradio.com";
    private final String spotifyLookupUrl = "https://api.spotify.com/v1/search?q=";
    private int channel;
    private int day;
    private int month;
    private int year;
    private int timezone;
    private ArrayList<SongItem> songs = new ArrayList<>();
    private Queue<String> urls;
    private SqlHelper db;

    public SiriusChannel(Context context) {
        this.context = context;
        this.channel = -1;
        this.songs = new ArrayList();
        this.urls = new LinkedList<String>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        this.day = c.get(Calendar.DATE);
        this.month = c.get(Calendar.MONTH)+1;
        this.year = c.get(Calendar.YEAR);
        this.timezone = 0;

        Log.v("SiriusChannelInit", String.format("Day: %d, month: %d, year: %d", this.day, this.month, this.year));
    }

    public SiriusChannel(Context context, int channel) {
        this.context = context;
        this.channel = channel;
        this.songs = new ArrayList();
        this.urls = new LinkedList<String>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        this.day = c.get(Calendar.DATE);
        this.month = c.get(Calendar.MONTH)+1;
        this.year = c.get(Calendar.YEAR);
        this.timezone = 0;

        // init db helper
        db = new SqlHelper(context);
        Log.v("SiriusChannelInit", String.format("Day: %d, month: %d, year: %d", this.day, this.month, this.year));

        // set initial url
        urls.add(String.format("%s/search_playlist.php?artist=&title=&channel=%d&month=%d&date=%d&shour=&sampm=&stz=&ehour=&eampm=",
                this.baseUrl,
                this.channel,
                this.month,
                this.day));

    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public ArrayList<SongItem> getSongs() {
        ArrayList<SongItem> songs = db.getSongs(channel);
        if (songs==null) {
            Log.v("getSongs", "No songs found");
        }
        return db.getSongs(channel);
    }

    public void doChannelLookup() {
        // if we have URLs in the queue, get the song list
        if (!this.urls.isEmpty()) {
            final String url = urls.remove();

            AsyncHttpClient client = new AsyncHttpClient();
            client.setMaxRetriesAndTimeout(5, 20000);
            client.setResponseTimeout(20000);
            client.setURLEncodingEnabled(true);

            client.get(url, new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    // called before request is started
                    Log.v("SiriusChannelLookup", String.format("Loading URL: %s", url));
                }


                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    String result = new String(response, Charsets.UTF_8);

                    Log.v("SiriusChannel Result", "Done");

                    // see if we have another page to process
                    Pattern re = Pattern.compile(".*<a href=(.*)>Next<br>Page<\\/a>.*");
                    Matcher m = re.matcher(result);

                    // if we have another page, add to the url queue
                    if (m.find()) {
                        Toast.makeText(context, String.format("Found another page: %s", m.group(1)), Toast.LENGTH_LONG).show();
                        Log.v("FoundPage", m.group(1));
                        urls.add(baseUrl+"/"+m.group(1));
                    } else {
                        Toast.makeText(context, String.format("Finished processing pages"), Toast.LENGTH_LONG).show();
                    }

                    // scrape songs off the page
                    re = Pattern.compile("<tr><td>(\\d+)<\\/td><td>(.*)<\\/td><td><a.*\">(.*)<\\/a><\\/td><td>\\d+\\/\\d+\\/\\d+<\\/td><td>\\d+\\:\\d+\\:\\d+ [A|P]M<\\/td><\\/tr>");
                    m = re.matcher(result);
                    while (m.find()) {
                        Log.v("Songs", String.format("Found song. channel: %s, artist: %s, title: %s", m.group(1), m.group(2), m.group(3)));
                        db.addSong(Integer.parseInt(m.group(1)), m.group(2), m.group(3));
                        songs.add(new SongItem(Integer.parseInt(m.group(1)), m.group(2), m.group(3)));
                    }

                    // if we have another url to process, call this method again
                    if (!urls.isEmpty()) {
                        Log.v("Page", "Page complete, recusing...");
                        doChannelLookup();
                    } else {
                        Toast.makeText(context, String.format("Finished processing channel: %d, found %d songs",channel, songs.size()), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    Toast.makeText(context, String.format("HTTP error %d downloading song list, found %d songs", statusCode, songs.size()),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onRetry(int retryNo) {
                    // called when request is retried
                    Toast.makeText(context, String.format("HTTP request attempt %d failed, retrying...", retryNo),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void sync() {
        SyncTask sync = new SyncTask();
        sync.execute(new String[] {urls.remove() });
    }

    private class SyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.v("SyncTask", "Started");

            // make sure we have a url
            if (urls.length==0) {
                return null;
            }

            Queue<String> li = new LinkedList<>();

            for (String u : urls ) {
                li.add(u);
            }

            li.add(urls[0]);

            while (!li.isEmpty()) {
                String url = li.remove();

                try {
                    Log.v("SyncTask", "Processing "+url);
                    final OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    String responseBody = response.body().string();

                    // see if we have another page to process
                    Pattern re = Pattern.compile(".*<a href=(.*)>Next<br>Page<\\/a>.*");
                    Matcher m = re.matcher(responseBody);

                    // if we have another page, add to the url queue
                    if (m.find()) {
                        Log.v("SyncTask", "Found new page: " + m.group(1));
                        li.add(baseUrl+"/"+m.group(1));
                    } else {
                        Log.v("SyncTask", "Reached last page");
                    }

                    // scrape songs off the page
                    re = Pattern.compile("<tr><td>(\\d+)<\\/td><td>(.*)<\\/td><td><a.*\">(.*)<\\/a><\\/td><td>\\d+\\/\\d+\\/\\d+<\\/td><td>\\d+\\:\\d+\\:\\d+ [A|P]M<\\/td><\\/tr>");
                    m = re.matcher(responseBody);
                    while (m.find()) {
                        long id = db.addSong(Integer.parseInt(m.group(1)), m.group(2), m.group(3));
                        if (id>0) {
                            Log.v("SyncTask", String.format("Found new song: channel: %s, artist: %s, title: %s, dbId: %d", m.group(1), m.group(2), m.group(3), id));
                            String track = getSpotifyTrack(m.group(2), m.group(3));
                            if (!track.equals("")) {
                                // update db
                                db.updateUri(id, track);
                            } else {
                                db.deleteSong(id);
                                Log.v("SyncTask", "Unknown track, dropping from db");
                            }
                        }
//                        songs.add(new SongItem(Integer.parseInt(m.group(1)), m.group(2), m.group(3)));
                    }

                } catch (Exception e) {
                    Log.e("SyncTask", "Exception", e);
                }


            }

            return null;
        }

        private String getSpotifyTrack(String artist, String title) {
            try {
                final OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(spotifyLookupUrl+URLEncoder.encode(title)+"+artist:"+URLEncoder.encode(artist)+"&type=track&market=US")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String responseBody = response.body().string();

                Pattern re = Pattern.compile(".*\\\"uri\\\" \\: \\\"(spotify\\:track:.*)\\\".*");
                Matcher m = re.matcher(responseBody);
                if (m.find()) {
                    Log.v("SyncTask", "Found Spotify track: " + m.group(1));
                    return m.group(1);
                } else {
                    Log.v("SyncTask", "Song lookup failed (not found in response)");
                    return "";
                }

            } catch (Exception e) {
                Log.v("SyncTask", "Song lookup failed (exception)");
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, String.format("Sync complete for channel %d", channel),
                    Toast.LENGTH_SHORT).show();
            Log.v("SyncTask", "Complete");
        }
    }


}
