# spotirius-android
Want to hear satellite radio playlists on the go? Spotirius syncs your Spotify playlists with ones from satellite radio!

This application scrapes the playlists from http://www.dogstarradio.com, looks up tracks with the Spotify API, and adds them to a playlist for you automatically.

#Excuse the mess...

The user interface is very clunky at the moment. Please excuse the mess as the application is under heavy development.


#Getting Started
To start using this project, modify <code>app/src/main/java/com/booshaday/spotirius/data/SpotifyClientConfig.java</code> and add your Spotify client ID and secret. In your Spotify developer portal, add the redirect URI: <code>spotirius-login://callback</code>

**Once you're up and running...**
Click the menu button, add a channel or two, then click the menu and select *Sync*.


#What's New
* Add channel to new playlist
* Add channel to existing playlist
* Remove channel
* Background sync (sync currently blocks app's UI thread)
* No longer requires a Spotify Premium account


#TODO
* Better user interface
* Background sync
* Blacklist artists you don't want to hear
* Automatically remove songs after given amount of time to keep playlists "fresh"



#Thanks
This project uses Google Volley and the Spotify Android SDK (beta 9 at the moment). Also thank you to http://www.dogstarradio.com for providing satellite radio playlists!
