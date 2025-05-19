package com.example.beomusic.repositories;

import com.example.beomusic.API.DeezerApiService;
import com.example.beomusic.models.Song;

import java.util.List;

public class MusicRepository {
    private DeezerApiService apiService;

    public MusicRepository() {
        this.apiService = new DeezerApiService();
    }

    public void searchSongs(String query, DeezerApiService.ApiCallback<List<Song>> callback) {
        apiService.searchSongs(query, callback);
    }
}
