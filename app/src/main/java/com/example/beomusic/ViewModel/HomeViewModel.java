package com.example.beomusic.ViewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.beomusic.API.DeezerApiService;
import com.example.beomusic.models.Song;
import com.example.beomusic.repositories.MusicRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private MusicRepository repository;
    private MutableLiveData<List<Song>> songs = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    public HomeViewModel() {
        repository = new MusicRepository();
    }

    public LiveData<List<Song>> getSongs() {
        return songs;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void searchSongs(String query) {
        isLoading.setValue(true);
        error.setValue(null);

        repository.searchSongs(query, new DeezerApiService.ApiCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                isLoading.postValue(false);
                songs.postValue(result);
                Log.d(TAG, "Tìm thấy " + result.size() + " bài hát");
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                error.postValue("Lỗi khi tìm kiếm: " + e.getMessage());
                Log.e(TAG, "Lỗi khi tìm kiếm: " + e.getMessage());
            }
        });
    }
}