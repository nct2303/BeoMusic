package com.example.beomusic.API;

import android.util.Log;

import com.example.beomusic.models.Song;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeezerApiService {
    private static final String TAG = "DeezerApiService";
    private static final String DEEZER_SEARCH_URL = "https://api.deezer.com/search?q=";

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void searchSongs(String query, ApiCallback<List<Song>> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Bắt đầu tìm kiếm: " + query);
                URL url = new URL(DEEZER_SEARCH_URL + query.replace(" ", "%20"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                List<Song> songs = parseDeezerResponse(response.toString());
                callback.onSuccess(songs);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi gọi API Deezer: " + e.getMessage());
                callback.onError(e);
            }
        }).start();
    }

    private List<Song> parseDeezerResponse(String jsonResponse) throws JSONException {
        List<Song> songs = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject trackJson = dataArray.getJSONObject(i);

            String songId = trackJson.getString("id");
            String title = trackJson.getString("title");
            int duration = trackJson.getInt("duration");
            String previewUrl = trackJson.getString("preview");

            JSONObject artist = trackJson.getJSONObject("artist");
            String artistName = artist.getString("name");

            JSONObject album = trackJson.getJSONObject("album");
            String thumbnailUrl = album.getString("cover_medium");

            // Xác định genre nếu có
            String genre = "Unknown";
            if (trackJson.has("genre_id")) {
                genre = trackJson.getString("genre_id");
            }

            Song song = new Song();
            song.setSongId(songId);
            song.setTitle(title);
            song.setArtist(artistName);
            song.setDuration(duration);
            song.setSourceType("Deezer");
            song.setFilePath(previewUrl);  // URL preview để phát nhạc
            song.setThumbnailUrl(thumbnailUrl);
            song.setGenre(genre);
            song.setAddedDate(new Date());  // Thời gian hiện tại
            song.setPlayCount(0);  // Bắt đầu với 0 lượt phát

            songs.add(song);
        }

        return songs;
    }
}
