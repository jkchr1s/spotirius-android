package com.booshaday.spotirius.net;

import android.util.Log;

import com.google.gson.JsonElement;
import com.squareup.okhttp.OkHttpClient;

import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by chris on 5/29/15.
 */
public class RestClient {
    private static final String TAG = "RestClient";

    public static <S> S create(Class<S> serviceClass, String baseUrl, final String token) {

        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(baseUrl)
                .setClient(new OkClient(new OkHttpClient()));

        if (token != null) {
            Log.d(TAG, "Authorization: "+token);
            builder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Accept", "application/json");
                    request.addHeader("Authorization", token);
                }
            });
        }

        RestAdapter adapter = builder.build();

        return adapter.create(serviceClass);
    }

    public interface DogStarRadio {
        String URL = "http://www.dogstarradio.com";

        @GET("/search_playlist.php")
        Response getChannels();

        @GET("/search_playlist.php")
        void getChannelsAsync(Callback<Response> callback);

        @GET("/search_playlist.php")
        Response getPlaylist(@Query("artist") String artist,
                             @Query("title") String title,
                             @Query("channel") String channel,
                             @Query("month") int month,
                             @Query("date") int date,
                             @Query("shour") String startHour,
                             @Query("sampm") String startAmpm,
                             @Query("stz") String timezone,
                             @Query("ehour") String endHour,
                             @Query("eampm") String endAmpm,
                             @Query("page") int page);
    }

    public interface Spotify {
        String API_URL = "https://api.spotify.com/v1";
        String ACCOUNTS_URL = "https://accounts.spotify.com";

        @POST("/users/{username}/playlists/{playlistId}/tracks")
        JsonElement addTracks(@Path("username") String username,
                              @Path("playlistId") String playlistId,
                              @Query("uris") String uris,
                              @Body String body);

        @FormUrlEncoded
        @POST("/users/{username}/playlists")
        JsonElement createPlaylist(@Path("username") String username,
                                   @Field("name") String playlistTitle,
                                   @Field("public") boolean isPublic);

        @POST("/users/{username}/playlists")
        void createPlaylistAsync(@Path("username") String username,
                                 @Body Map<String, Object> body,
                                 Callback<JsonElement> callback);

        @GET("/me")
        JsonElement getMe();

        @GET("/me")
        void getMeAsync(Callback<JsonElement> callback);

        @GET("/users/{username}/playlists")
        JsonElement getPlaylists(@Path("username") String username,
                                 @Query("limit") int limit);

        @GET("/users/{username}/playlists")
        void getPlaylistsAsync(@Path("username") String username,
                               @Query("limit") int limit,
                               @Query("offset") int offset,
                               Callback<JsonElement> callback);

        @GET("/users/{username}/playlists/{playlistId}/tracks")
        JsonElement getPlaylistTracks(@Path("username") String username,
                                      @Path("playlistId") String playlistId,
                                      @Query("limit") int limit,
                                      @Query("offset") int offset);

        @FormUrlEncoded
        @POST("/api/token")
        JsonElement getAccessToken(@Field("client_id") String clientId,
                                   @Field("client_secret") String clientSecret,
                                   @Field("grant_type") String grantType,
                                   @Field("code") String code,
                                   @Field("redirect_uri") String redirectUri);

        @FormUrlEncoded
        @POST("/api/token")
        void getAccessTokenAsync(@Field("client_id") String clientId,
                                 @Field("client_secret") String clientSecret,
                                 @Field("grant_type") String grantType,
                                 @Field("code") String code,
                                 @Field("redirect_uri") String redirectUri,
                                 Callback<JsonElement> callback);

        @FormUrlEncoded
        @POST("/api/token")
        JsonElement getRefreshToken(@Field("client_id") String clientId,
                                    @Field("client_secret") String clientSecret,
                                    @Field("grant_type") String grantType,
                                    @Field("refresh_token") String refreshToken);

        @FormUrlEncoded
        @POST("/api/token")
        void getRefreshTokenAsync(@Field("client_id") String clientId,
                                  @Field("client_secret") String clientSecret,
                                  @Field("grant_type") String grantType,
                                  @Field("refresh_token") String refreshToken,
                                  Callback<JsonElement> callback);

        @GET("/search")
        JsonElement getSearchResults(@Query("q") String searchParameters,
                                     @Query("type") String type,
                                     @Query("market") String market,
                                     @Query("limit") int limit);

        @GET("/search")
        Response getSearchResultsString(@Query("q") String searchParameters,
                                        @Query("type") String type,
                                        @Query("market") String market,
                                        @Query("limit") int limit);

    }
}
