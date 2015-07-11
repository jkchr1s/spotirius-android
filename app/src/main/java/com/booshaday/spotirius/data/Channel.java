package com.booshaday.spotirius.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Created by chris on 5/30/15.
 */
public class Channel {
    private String channel;
    private String playlistId;
    private String playlist;
    private long lastSync;

    public Channel(String channel, String playlistId) {
        this.channel = channel;
        this.playlistId = playlistId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public String getPlaylist() {
        return playlist;
    }

    public void setPlaylist(String playlist) {
        this.playlist = playlist;
    }

    public long getLastSync() {
        return lastSync;
    }

    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }
}
