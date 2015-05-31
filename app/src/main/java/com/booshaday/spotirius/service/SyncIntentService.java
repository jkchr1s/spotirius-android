package com.booshaday.spotirius.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.booshaday.spotirius.data.AppConfig;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Song;

import java.util.Calendar;
import java.util.List;

/**
 * Created by chris on 5/27/15.
 */
public class SyncIntentService extends IntentService {
    private static final String TAG = "SyncIntentService";
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_FINISHED = 1;
    private static final int STATUS_ERROR = 2;
    private final int NOTIFICATION_ID = 1000;

    private Intent mIntent;
    private NotificationManager mNotificationManager;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SyncIntentService(String name) {
        super(name);
    }

    public SyncIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service started.");

        this.mIntent = intent;

        // show notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Spotirius Sync")
                .setContentText("Playlist sync in progress...");

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        mNotificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        SyncService service = new SyncService(this, null);

        try {
            service.refreshToken();
        } catch (Exception e) {
            Toast.makeText(this, "Spotirius: Please launch the app and log in again.", Toast.LENGTH_LONG).show();
            // stop notification
            if (mNotificationManager!=null) {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mNotificationManager = null;
            }
            stopSelf();
        }

        // get date from yesterday
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        try {
            List<Channel> channels = AppConfig.getChannels(this);
            for (Channel c : channels) {
                // get the existing playlist for the channel
                List<Song> playlist = service.getPlaylistTracks(c.getPlaylistId());
                Log.d(TAG, String.format("playlist for channel %s contains %d songs", c.getChannel(), playlist.size()));

                // start sync
                service.getChannelPlaylist(playlist, cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE), c.getChannel(), c.getPlaylistId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // stop notification
        if (mNotificationManager!=null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager = null;
        }

        stopSelf();
    }
}
