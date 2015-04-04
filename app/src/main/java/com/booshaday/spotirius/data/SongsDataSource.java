package com.booshaday.spotirius.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chris on 1/28/15.
 */
public class SongsDataSource {
    // Database fields
    private SQLiteDatabase database;
    private SqlHelper dbHelper;
    private String[] allColumns = { dbHelper.COLUMN_SONGS_ID,
            dbHelper.COLUMN_SONGS_CHANNEL,
            dbHelper.COLUMN_SONGS_ARTIST,
            dbHelper.COLUMN_SONGS_TITLE,
            dbHelper.COLUMN_SONGS_URI
    };

    public SongsDataSource(Context context) {
        dbHelper = new SqlHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public SongItem createSong(int channel, String artist, String title) {
        ContentValues values = new ContentValues();
        values.put(SqlHelper.COLUMN_SONGS_CHANNEL, channel);
        values.put(SqlHelper.COLUMN_SONGS_ARTIST, artist);
        values.put(SqlHelper.COLUMN_SONGS_TITLE, title);

        long insertId = database.insert(SqlHelper.TABLE_SONGS, null,
                values);

        Cursor cursor = database.query(SqlHelper.TABLE_SONGS,
                allColumns, SqlHelper.COLUMN_SONGS_ID + " = " + insertId, null,
                null, null, null);

        cursor.moveToFirst();
        SongItem song = cursorToSong(cursor);
        cursor.close();
        return song;
    }

    public void deleteSong(SongItem song) {
        long id = song.getId();
        System.out.println("Song deleted with id: " + id);
        database.delete(SqlHelper.TABLE_SONGS, SqlHelper.COLUMN_SONGS_ID
                + " = " + id, null);
    }


    public List<SongItem> getAllSongs() {
        List<SongItem> songs = new ArrayList<SongItem>();

        Cursor cursor = database.query(SqlHelper.TABLE_SONGS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            SongItem song = cursorToSong(cursor);
            songs.add(song);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return songs;
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

}
