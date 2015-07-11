![alt tag](http://i.imgur.com/qv25PJf.png) ![alt tag](http://i.imgur.com/EuyRXGI.png) ![alt tag](http://i.imgur.com/VmcfB7o.png)

# BETA 2
Just needs automatic background sync

# spotirius-android
Want to hear satellite radio playlists on the go? Spotirius syncs your Spotify playlists with ones from satellite radio!

This application scrapes the playlists from http://www.dogstarradio.com, looks up tracks with the Spotify API, and adds them to a playlist for you automatically.


#Getting Started
To start using this project, modify <code>app/src/main/java/com/booshaday/spotirius/data/SpotifyClientConfig.java</code> and add your Spotify client ID and secret. In your Spotify developer portal, add the redirect URI: <code>spotirius-login://callback</code>

**Once you're up and running...**
Click the menu button, add a channel or two, then click the menu and select *Sync*.


#What's New
* New front end interface
* Option to enable/disable strict searching
* Syncs only occur once daily per channel (no more unneccessary resync)


#Fixes
* Fixed API calls for submitting > 50 tracks in batch
* Fixed track searching


#TODO
* Automatic background sync
* Blacklist artists you don't want to hear
* Automatically remove songs after given amount of time to keep playlists "fresh"



#Thanks
This project uses Retrofit and the Spotify Android SDK (beta 10). Also thank you to http://www.dogstarradio.com for providing satellite radio playlists!
