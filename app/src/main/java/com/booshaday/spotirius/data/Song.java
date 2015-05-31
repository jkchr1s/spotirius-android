package com.booshaday.spotirius.data;

/**
 * Created by chris on 5/30/15.
 */
public class Song {
    private String artist;
    private String title;
    private String uri;

    public Song(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public Song(String artist, String title, String uri) {
        this.artist = artist;
        this.title = title;
        this.uri = uri;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        // match on uri
        if (uri!=null && song.getUri()!=null && song.getUri().equals(uri)) return true;

        if (artist != null ? !artist.equals(song.artist) : song.artist != null) return false;
        return !(title != null ? !title.equals(song.title) : song.title != null);

    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Song{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
