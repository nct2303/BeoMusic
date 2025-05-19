package com.example.beomusic.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beomusic.BottomNavigation.BaseActivity;
import com.example.beomusic.R;
import com.example.beomusic.adapters.SongAdapter;
import com.example.beomusic.models.Song;
import com.example.beomusic.ViewModel.HomeViewModel;
import com.example.beomusic.views.album.SongDetailActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class HomeActivity extends BaseActivity implements SongAdapter.OnSongClickListener {

    // UI Components
    private RecyclerView recyclerSongs;
    private ProgressBar progressBar;
    private ImageButton btnSearch;
    private TabLayout tabLayout;

    // ViewModel & Adapter
    private HomeViewModel viewModel;
    private SongAdapter adapter;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_home;
    }

    @Override
    protected int getDefaultNavigationItemId() {
        return R.id.nav_home;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ Khởi tạo view
        initViews();

        // ⭐ Cài đặt RecyclerView
        setupRecyclerView();

        // ⭐ Khởi tạo ViewModel
        setupViewModel();

        // ⭐ Gắn các sự kiện
        setupTabLayout();
        setupSearchButton();

        // ⭐ Tải bài hát mặc định ("popular")
        viewModel.searchSongs("popular");
    }

    // 📌 Khởi tạo view
    private void initViews() {
        recyclerSongs = findViewById(R.id.recyclerSongs);
        progressBar = findViewById(R.id.progressBar);
        btnSearch = findViewById(R.id.btnSearch);
        tabLayout = findViewById(R.id.tabLayout);
    }

    // 📌 Setup RecyclerView
    private void setupRecyclerView() {
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, this); // this = context, this = click listener
        recyclerSongs.setAdapter(adapter);
    }

    // 📌 Setup ViewModel và observe dữ liệu
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.getSongs().observe(this, songs -> {
            adapter.setSongs(songs);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // 📌 TabLayout xử lý phân loại bài hát
    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        viewModel.searchSongs("popular");
                        break;
                    case 1:
                        viewModel.searchSongs("top");
                        break;
                    case 2:
                        viewModel.searchSongs("new release");
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // 📌 Xử lý khi bấm nút tìm kiếm
    private void setupSearchButton() {
        btnSearch.setOnClickListener(v -> viewModel.searchSongs("Alan Walker"));
    }

    // 📌 Khi click vào 1 bài hát
    @Override
    public void onSongClick(Song song) {
        Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, SongDetailActivity.class);

        // Truyền thông tin bài hát được chọn
        intent.putExtra("song_id", song.getSongId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("duration", song.getDuration());
        intent.putExtra("thumbnail_url", song.getThumbnailUrl());
        intent.putExtra("preview_url", song.getFilePath());
        intent.putExtra("genre", song.getGenre());

        // Truyền danh sách bài hát hiện tại
        ArrayList<Song> songList = new ArrayList<>(adapter.getSongs());
        // Đảm bảo các bài hát có đầy đủ thông tin để có thể lưu vào firebase
        for (Song s : songList) {
            if (s.getSongId() == null || s.getSongId().isEmpty()) {
                s.setSongId(s.getId()); // Đảm bảo songId không rỗng
            }
        }
        intent.putExtra("song_list", songList);
        intent.putExtra("current_position", adapter.getSongs().indexOf(song));

        startActivity(intent);
    }

    // 📌 Khi click vào nút "more" của bài hát (menu tuỳ chọn)
    @Override
    public void onMoreClick(Song song, View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_song_options, popupMenu.getMenu());

        // Ẩn tùy chọn xóa khỏi danh sách yêu thích vì không phải trang Album
        popupMenu.getMenu().findItem(R.id.action_remove_favorite).setVisible(false);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add_to_playlist) {
                Toast.makeText(this, "Added to playlist: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_share) {
                Toast.makeText(this, "Share clicked: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_details) {
                Toast.makeText(this, "Showing song details: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }
}