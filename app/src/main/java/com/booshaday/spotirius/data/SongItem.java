package com.booshaday.spotirius.data;

/**
 * Created by chris on 1/28/15.
 */
public class SongItem {
    private long id;
    private int channel;
    private String artist;
    private String title;
    private String uri;

    public SongItem(int channel, String artist, String title) {
        this.channel = channel;
        this.artist = artist;
        this.title = title;
    }

    public SongItem(long id, int channel, String artist, String title, String uri) {
        this.id = id;
        this.channel = channel;
        this.artist = artist;
        this.title = title;
        this.uri = uri;
    }

    public long getId() {
        return this.id;
    }

    public String getArtist() {
        return this.artist;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUri() {
        return this.uri;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSong(int id, int channel, String artist, String title, String uri) {
        this.id = id;
        this.channel = channel;
        this.artist = artist;
        this.title = title;
        this.uri = uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
