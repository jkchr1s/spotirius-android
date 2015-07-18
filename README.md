![alt tag](http://i.imgur.com/qv25PJf.png) ![alt tag](http://i.imgur.com/EuyRXGI.png)

# BETA 3
Now with background sync!

# spotirius-android
Want to hear satellite radio playlists on the go? Spotirius syncs your Spotify playlists with ones from satellite radio!

This application scrapes the playlists from http://www.dogstarradio.com, looks up tracks with the Spotify API, and adds them to a playlist for you automatically.

# Background Sync
Background sync has been implemented and follows the following rules:
* Sync 1 minute after BOOT_COMPLETED if network is available.
* Check to see if channels need to by synced every 4 hours if network is available.
* If network state changes, you are connected to a WiFi connection, and channels have not tried to sync for 4 hours, a sync will occur after 30 seconds.


#Getting Started
To start using this project, modify <code>app/src/main/java/com/booshaday/spotirius/data/SpotifyClientConfig.java</code> and add your Spotify client ID and secret. In your Spotify developer portal, add the redirect URI: <code>spotirius-login://callback</code>

**Once you're up and running...**
Click the menu button, add a channel or two, then click the menu and select *Sync*.


#What's New
* Background sync


#Fixes
* Only new features in this release


#TODO
* Background sync preferences
* Blacklist artists you don't want to hear
* Automatically remove songs after given amount of time to keep playlists "fresh"



#Thanks
This project uses Retrofit and the Spotify Android SDK (beta 10). Also thank you to http://www.dogstarradio.com for providing satellite radio playlists!
