# ALPHA 4 RELEASE
Now it just needs UI love and automatic syncing! :)

# spotirius-android
Want to hear satellite radio playlists on the go? Spotirius syncs your Spotify playlists with ones from satellite radio!

This application scrapes the playlists from http://www.dogstarradio.com, looks up tracks with the Spotify API, and adds them to a playlist for you automatically.

#Excuse the mess...

The user interface is very clunky at the moment. Please excuse the mess as the application is under heavy development. It's best to install the apk, sign in, quit the app, and relaunch. If you don't, it won't see existing playlists when you add a channel. This is on my to-do list.


#Getting Started
To start using this project, modify <code>app/src/main/java/com/booshaday/spotirius/data/SpotifyClientConfig.java</code> and add your Spotify client ID and secret. In your Spotify developer portal, add the redirect URI: <code>spotirius-login://callback</code>

**Once you're up and running...**
Click the menu button, add a channel or two, then click the menu and select *Sync*.


#What's New
* No longer requires a Spotify Premium account
* Switched from Volley to Retrofit
* Removed sqlite, now checks Spotify playlist for duplicates


#Fixes
* Sync no longer blocks UI thread
* Deduplicates based on Spotify playlist rather than local sqlite database


#TODO
* Better user interface
* Automatic background sync
* Blacklist artists you don't want to hear
* Automatically remove songs after given amount of time to keep playlists "fresh"



#Thanks
This project uses Retrofit and the Spotify Android SDK (beta 9 at the moment). Also thank you to http://www.dogstarradio.com for providing satellite radio playlists!
