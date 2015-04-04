package com.booshaday.spotirius.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by chris on 1/28/15.
 */
public class SqlHelper extends SQLiteOpenHelper {
    public static final String TABLE_SONGS = "songs";
    public static final String COLUMN_SONGS_ID = "_id";
    public static final String COLUMN_SONGS_CHANNEL = "channel";
    public static final String COLUMN_SONGS_ARTIST = "artist";
    public static final String COLUMN_SONGS_TITLE = "title";
    public static final String COLUMN_SONGS_URI = "uri";
    public static final String COLUMN_SONGS_TS = "ts";
    public static final String COLUMN_SONGS_ADDED = "added_to_playlist";

    public static final String TABLE_CHANNELS = "channels";
    public static final String COLUMN_CHANNELS_ID = "_id";
    public static final String COLUMN_CHANNELS_CHANNEL = "channel";
    public static final String COLUMN_CHANNELS_PLAYLIST = "playlist";

    private static final String DATABASE_NAME = "songs.db";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE_TABLE_SONGS = "create table "
            + TABLE_SONGS + "(" + COLUMN_SONGS_ID + " integer primary key autoincrement, "
            + COLUMN_SONGS_CHANNEL + " text not null, "
            + COLUMN_SONGS_ARTIST + " text not null, "
            + COLUMN_SONGS_TITLE + " text not null, "
            + COLUMN_SONGS_URI + " text, "
            + COLUMN_SONGS_TS + " date default CURRENT_DATE, "
            + COLUMN_SONGS_ADDED + " integer default 0)";
    private static final String DATABASE_CREATE_TABLE_CHANNELS = "create table "
            + TABLE_CHANNELS + "(" + COLUMN_CHANNELS_ID + " integer primary key autoincrement, "
            + COLUMN_CHANNELS_CHANNEL + " text not null, "
            + COLUMN_CHANNELS_PLAYLIST + " text not null)";

    // constuctor
    public SqlHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_TABLE_SONGS);
        database.execSQL(DATABASE_CREATE_TABLE_CHANNELS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SqlHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANNELS);
        onCreate(db);
    }

    public long addSong(int channel, String artist, String title) {
        if (songExists(channel, artist, title)) {
            // song already exists, skip
            return -1;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_SONGS_CHANNEL, channel);
        cv.put(COLUMN_SONGS_ARTIST, artist);
        cv.put(COLUMN_SONGS_TITLE, title);

        return db.insert(TABLE_SONGS, null, cv);
    }

    public int deleteSong(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        String where = "_id='"+id+"'";

        return db.delete(TABLE_SONGS, where, null);
    }

    public boolean updateUri(long id, String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        String filter = "_id="+id;

        cv.put(COLUMN_SONGS_URI, uri);

        db.update(TABLE_SONGS, cv, filter, null);
        return true;
    }

    public void setSongLoaded(long id, boolean added) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        String filter = COLUMN_SONGS_ID + "=" + id;

        int songAdded = (added ? 1 : 0);

        cv.put(COLUMN_SONGS_ADDED, songAdded);

        db.update(TABLE_SONGS, cv, filter, null);
    }

    public boolean channelExists(String channel) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor ds = db.rawQuery("select _id from channels where channel=?", new String[]{channel});
        return ds.getCount()>0;
    }

    public boolean songExists(int channel, String artist, String title) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor ds = db.rawQuery("select _id from songs where channel=? and artist=? and title=?",
                new String[] {String.format("%d", channel), artist, title});
        if (ds.getCount() > 0)
            return true;
        else
            return false;
    }

    public ArrayList<SpotiriusChannel> getChannels() {
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<SpotiriusChannel> channels = new ArrayList<>();

        Cursor cursor = db.rawQuery("select "+COLUMN_CHANNELS_ID+", "+COLUMN_CHANNELS_CHANNEL+", "+COLUMN_CHANNELS_PLAYLIST+" FROM "+TABLE_CHANNELS+" ORDER BY "+COLUMN_CHANNELS_CHANNEL, null);
        if (cursor==null) return channels;

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            channels.add(cursorToChannel(cursor));
            cursor.moveToNext();
        }

        return channels;
    }

    public void addChannel(String channel, String playlistUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_CHANNELS_CHANNEL, channel);
        cv.put(COLUMN_CHANNELS_PLAYLIST, playlistUri);

        db.insert(TABLE_CHANNELS, null, cv);
    }

    public ArrayList<SongItem> getSongs(int channel) {
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<SongItem> songs = new ArrayList<>();

        Cursor cursor = db.rawQuery("select _id, channel, artist, title, uri from songs where channel=?",
                new String[]{String.format("%d", channel)});

        if (cursor==null) {
            Log.v("SqlHelper", "Null cursor");
        }

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SongItem song = cursorToSong(cursor);
                songs.add(song);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();
        }

        return songs;
    }

    public SpotiriusChannel getChannel(String channel) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select "+COLUMN_CHANNELS_ID+", "+COLUMN_CHANNELS_CHANNEL+", "+COLUMN_CHANNELS_PLAYLIST+" FROM "+TABLE_CHANNELS+" WHERE "+COLUMN_CHANNELS_CHANNEL+"=?",
                new String[]{channel});

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursorToChannel(cursor);
        } else {
            return null;
        }
    }

    private SpotiriusChannel cursorToChannel(Cursor cursor) {
        return new SpotiriusChannel(cursor.getInt(0), cursor.getString(1), cursor.getString(2));
    }

    private SongItem cursorToSong(Cursor cursor) {
        SongItem song = new SongItem(cursor.getLong(0),
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
        );
        return song;
    }


    SongItem getSong(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SONGS,
                new String[] {COLUMN_SONGS_ID, COLUMN_SONGS_CHANNEL, COLUMN_SONGS_ARTIST, COLUMN_SONGS_TITLE, COLUMN_SONGS_URI},
                COLUMN_SONGS_ID + "=?",
                new String[] { String.valueOf(id) },
                null, null, null, null
        );
        if (cursor != null)
            cursor.moveToFirst();

            SongItem song = new SongItem(Integer.parseInt(cursor.getString(0)),
                    Integer.parseInt(cursor.getString(1)),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
                    );
            return song;
    }

}
