package com.example.beomusic.views.album;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beomusic.BottomNavigation.BaseActivity;
import com.example.beomusic.R;
import com.example.beomusic.adapters.SongAdapter;
import com.example.beomusic.models.Song;
import com.example.beomusic.repositories.FavoriteRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AlbumListActivity extends BaseActivity implements SongAdapter.OnSongClickListener {

    // UI Components
    private RecyclerView recyclerViewFavorites;
    private ProgressBar progressBar;
    private TextView tvNoFavorites;
    private TextView tvFavoritesTitle;

    private ImageButton imgSong;

    // Adapter
    private SongAdapter adapter;

    // Repository
    private FavoriteRepository favoriteRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize repository
        favoriteRepository = new FavoriteRepository();
        
        // Initialize views
        initViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Load favorite songs
        loadFavoriteSongs();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_album_list;
    }

    @Override
    protected int getDefaultNavigationItemId() {
        return R.id.nav_album;
    }
    
    private void initViews() {
        recyclerViewFavorites = findViewById(R.id.recyclerViewSongs);
        progressBar = findViewById(R.id.progressBar);
        tvNoFavorites = findViewById(R.id.tvNoSongs);
        tvFavoritesTitle = findViewById(R.id.tvAlbumTitle);
        
        // Set title
        tvFavoritesTitle.setText("Bài hát yêu thích");
        
        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.beomusic.views.HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void setupRecyclerView() {
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, this);
        adapter.setAlbumMode(true);
        recyclerViewFavorites.setAdapter(adapter);
    }
    
    private void loadFavoriteSongs() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoFavorites.setVisibility(View.GONE);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            tvNoFavorites.setText("Vui lòng đăng nhập để xem bài hát yêu thích");
            tvNoFavorites.setVisibility(View.VISIBLE);
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d("AlbumListActivity", "Loading favorites for user: " + userId);
        
        favoriteRepository.getFavoriteSongs(new FavoriteRepository.FavoritesCallback() {
            @Override
            public void onSuccess(List<Song> favoriteSongs) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    Log.d("AlbumListActivity", "Retrieved " + favoriteSongs.size() + " favorite songs");
                    
                    if (favoriteSongs.isEmpty()) {
                        tvNoFavorites.setText("Bạn chưa có bài hát yêu thích nào");
                        tvNoFavorites.setVisibility(View.VISIBLE);
                    } else {
                        for (Song song : favoriteSongs) {
                            Log.d("AlbumListActivity", String.format(
                                "Song loaded - Title: %s, ID: %s, Preview URL: %s",
                                song.getTitle(),
                                song.getSongId(),
                                song.getFilePath()
                            ));
                        }
                        
                        tvNoFavorites.setVisibility(View.GONE);
                        adapter.setSongs(favoriteSongs);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Log.e("AlbumListActivity", "Error loading favorites: " + errorMessage);
                    progressBar.setVisibility(View.GONE);
                    tvNoFavorites.setText("Lỗi: " + errorMessage);
                    tvNoFavorites.setVisibility(View.VISIBLE);
                    Toast.makeText(AlbumListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    @Override
    public void onSongClick(Song song) {
        Log.d("AlbumListActivity", "Song clicked: " + song.getTitle());
        Log.d("AlbumListActivity", "Song ID: " + song.getSongId());
        Log.d("AlbumListActivity", "Preview URL: " + song.getFilePath());
        
        if (song.getFilePath() == null || song.getFilePath().isEmpty()) {
            Toast.makeText(this, "Không thể phát bài hát - không có preview URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, SongDetailActivity.class);
        intent.putExtra("song_id", song.getSongId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("preview_url", song.getFilePath());
        
        ArrayList<Song> songList = new ArrayList<>(adapter.getSongs());
        intent.putExtra("song_list", songList);
        intent.putExtra("current_position", adapter.getSongs().indexOf(song));
        
        startActivity(intent);
    }
    
    @Override
    public void onMoreClick(Song song, View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_song_options, popupMenu.getMenu());
        
        // Đảm bảo hiển thị tùy chọn "Xóa khỏi danh sách yêu thích" chỉ khi đang ở trong trang Album
        popupMenu.getMenu().findItem(R.id.action_remove_favorite).setVisible(true);
        popupMenu.getMenu().findItem(R.id.action_add_to_playlist).setVisible(false);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add_to_playlist) {
                Toast.makeText(this, "Added to playlist: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_remove_favorite) {
                removeSongFromFavorites(song);
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
    
    // Phương thức xóa bài hát khỏi danh sách yêu thích
    private void removeSongFromFavorites(Song song) {
        // Hiển thị dialog xác nhận trước khi xóa
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài hát")
                .setMessage("Bạn có chắc chắn muốn xóa bài hát \"" + song.getTitle() + "\" khỏi danh sách yêu thích?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Hiển thị loading
                    progressBar.setVisibility(View.VISIBLE);
                    
                    // Gọi repository để xóa bài hát
                    favoriteRepository.removeFromFavorites(song, new FavoriteRepository.FavoriteCallback() {
                        @Override
                        public void onSuccess(boolean isFavorite) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AlbumListActivity.this, 
                                        "Đã xóa \"" + song.getTitle() + "\" khỏi danh sách yêu thích", 
                                        Toast.LENGTH_SHORT).show();
                                
                                // Tải lại danh sách bài hát yêu thích
                                loadFavoriteSongs();
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AlbumListActivity.this, 
                                        "Lỗi: " + errorMessage, 
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload favorite songs when returning to activity
        loadFavoriteSongs();
    }

    @Override
    public void onRemoveClick(Song song) {
        removeSongFromFavorites(song);
    }
}