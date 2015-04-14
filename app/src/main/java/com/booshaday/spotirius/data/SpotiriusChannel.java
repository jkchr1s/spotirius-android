package com.booshaday.spotirius.data;

/**
 * Created by chris on 3/28/15.
 */
public class SpotiriusChannel {
    private int id;
    private String channel;
    private String playlist;
    private String playlistName;

    public SpotiriusChannel(int id, String channel, String playlist) {
        this.id = id;
        this.channel = channel;
        this.playlist = playlist;
        this.playlistName = playlist;
    }

    public int getId() { return this.id; }

    public String getChannel() {
        return this.channel;
    }

    public String getPlaylist() {
        return this.playlist;
    }

    public String getPlaylistName() {
        return this.playlistName;
    }

    public void setPlaylistName(String name) {
        this.playlistName = name;
    }
}
