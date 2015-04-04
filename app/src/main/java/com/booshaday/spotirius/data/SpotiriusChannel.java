package com.booshaday.spotirius.data;

/**
 * Created by chris on 3/28/15.
 */
public class SpotiriusChannel {
    private int id;
    private String channel;
    private String playlist;

    public SpotiriusChannel(int id, String channel, String playlist) {
        this.id = id;
        this.channel = channel;
        this.playlist = playlist;
    }

    public int getId() { return this.id; }

    public String getChannel() {
        return this.channel;
    }

    public String getPlaylist() {
        return this.playlist;
    }
}
