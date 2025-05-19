package com.example.beomusic.repositories;

import com.example.beomusic.models.Album;
import com.example.beomusic.models.AlbumSong;
import com.example.beomusic.models.Song;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

/**
 * Repository class for handling user favorites operations
 */
public class FavoriteRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    public interface FavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onError(String errorMessage);
    }

    public interface FavoritesCallback {
        void onSuccess(List<Song> favoriteSongs);
        void onError(String errorMessage);
    }

    public FavoriteRepository() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Add a song to user's favorites
     * @param song Song to add to favorites
     * @param callback Callback for result
     */
    public void addToFavorites(Song song, FavoriteCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn cần đăng nhập để thêm bài hát vào danh sách yêu thích");
            return;
        }

        String userId = currentUser.getUid();
        String favoriteAlbumId = Album.generateFavoritesAlbumId(userId);

        // Check if favorites album exists, create if not
        firestore.collection("albums").document(favoriteAlbumId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create favorites album
                        Album favoritesAlbum = new Album(userId);
                        firestore.collection("albums").document(favoriteAlbumId)
                                .set(favoritesAlbum)
                                .addOnSuccessListener(aVoid -> addSongToFavorites(userId, favoriteAlbumId, song, callback))
                                .addOnFailureListener(e -> callback.onError("Lỗi khi tạo album yêu thích: " + e.getMessage()));
                    } else {
                        // Album already exists, add song
                        addSongToFavorites(userId, favoriteAlbumId, song, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError("Lỗi khi kiểm tra album yêu thích: " + e.getMessage()));
    }

    /**
     * Remove a song from user's favorites
     * @param song Song to remove from favorites
     * @param callback Callback for result
     */
    public void removeFromFavorites(Song song, FavoriteCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn cần đăng nhập để xóa bài hát khỏi danh sách yêu thích");
            return;
        }

        String userId = currentUser.getUid();
        String favoriteAlbumId = Album.generateFavoritesAlbumId(userId);
        String favoriteEntryId = AlbumSong.generateFavoriteEntryId(userId, song.getSongId());

        firestore.collection("album_songs").document(favoriteEntryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Decrement song count in album document
                    firestore.collection("albums").document(favoriteAlbumId)
                            .update("songCount", FieldValue.increment(-1))
                            .addOnSuccessListener(v -> callback.onSuccess(false))
                            .addOnFailureListener(e -> 
                                callback.onError("Lỗi khi cập nhật số lượng bài hát: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e -> callback.onError("Lỗi khi xóa bài hát khỏi danh sách yêu thích: " + e.getMessage()));
    }

    /**
     * Check if a song is in user's favorites
     * @param song Song to check
     * @param callback Callback for result
     */
    public void isFavorite(Song song, FavoriteCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onSuccess(false);
            return;
        }

        String userId = currentUser.getUid();
        String favoriteEntryId = AlbumSong.generateFavoriteEntryId(userId, song.getSongId());

        firestore.collection("album_songs").document(favoriteEntryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> callback.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(e -> callback.onError("Lỗi khi kiểm tra bài hát yêu thích: " + e.getMessage()));
    }

    /**
     * Get all songs in user's favorites
     * @param callback Callback for result
     */
    public void getFavoriteSongs(FavoritesCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Bạn cần đăng nhập để xem danh sách yêu thích");
            return;
        }

        String userId = currentUser.getUid();
        String favoriteAlbumId = Album.generateFavoritesAlbumId(userId);
        
        Log.d("FavoriteRepository", "Getting favorites for user: " + userId);

        firestore.collection("album_songs")
                .whereEqualTo("albumId", favoriteAlbumId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> songIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        AlbumSong albumSong = doc.toObject(AlbumSong.class);
                        if (albumSong != null) {
                            songIds.add(albumSong.getSongId());
                            Log.d("FavoriteRepository", "Found song ID: " + albumSong.getSongId());
                        }
                    }

                    if (songIds.isEmpty()) {
                        Log.d("FavoriteRepository", "No favorite songs found");
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    // Lấy thông tin bài hát từ collection songs
                    firestore.collection("songs")
                            .whereIn("songId", songIds)
                            .get()
                            .addOnSuccessListener(songsSnapshot -> {
                                List<Song> songs = new ArrayList<>();
                                for (DocumentSnapshot doc : songsSnapshot.getDocuments()) {
                                    try {
                                        Song song = new Song();
                                        song.setSongId(doc.getString("songId"));
                                        song.setTitle(doc.getString("title"));
                                        song.setArtist(doc.getString("artist"));
                                        song.setFilePath(doc.getString("previewUrl"));
                                        
                                        Log.d("FavoriteRepository", "Loaded song: " + 
                                            song.getTitle() + ", Preview URL: " + song.getFilePath());
                                        
                                        songs.add(song);
                                    } catch (Exception e) {
                                        Log.e("FavoriteRepository", "Error parsing song: " + e.getMessage());
                                    }
                                }
                                callback.onSuccess(songs);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FavoriteRepository", "Error getting songs: " + e.getMessage());
                                callback.onError("Lỗi khi lấy thông tin bài hát: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FavoriteRepository", "Error getting album_songs: " + e.getMessage());
                    callback.onError("Lỗi khi lấy danh sách yêu thích: " + e.getMessage());
                });
    }

    /**
     * Toggle favorite status (add or remove)
     * @param song Song to toggle
     * @param callback Callback for result
     */
    public void toggleFavorite(Song song, FavoriteCallback callback) {
        isFavorite(song, new FavoriteCallback() {
            @Override
            public void onSuccess(boolean isFavorite) {
                if (isFavorite) {
                    removeFromFavorites(song, callback);
                } else {
                    addToFavorites(song, callback);
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Private helper methods
    private void addSongToFavorites(String userId, String favoriteAlbumId, Song song, FavoriteCallback callback) {
        String favoriteEntryId = AlbumSong.generateFavoriteEntryId(userId, song.getSongId());
        
        // Chỉ lưu thông tin cần thiết
        Map<String, Object> songData = new HashMap<>();
        songData.put("songId", song.getSongId());        // Deezer track ID
        songData.put("title", song.getTitle());          // Tên bài hát
        songData.put("artist", song.getArtist());        // Tên nghệ sĩ
        songData.put("previewUrl", song.getFilePath());  // Preview URL từ Deezer
        
        // Lưu vào Firestore
        firestore.collection("songs").document(song.getSongId())
                .set(songData)
                .addOnSuccessListener(aVoid -> {
                    createAlbumSongEntry(favoriteAlbumId, favoriteEntryId, song, callback);
                })
                .addOnFailureListener(e -> callback.onError("Lỗi khi lưu bài hát: " + e.getMessage()));
    }

    private void createAlbumSongEntry(String favoriteAlbumId, String favoriteEntryId, Song song, FavoriteCallback callback) {
        AlbumSong favoriteEntry = new AlbumSong(favoriteAlbumId, song.getSongId());
        
        firestore.collection("album_songs").document(favoriteEntryId)
                .set(favoriteEntry)
                .addOnSuccessListener(aVoid -> {
                    // Increment song count in album document
                    firestore.collection("albums").document(favoriteAlbumId)
                            .update("songCount", FieldValue.increment(1))
                            .addOnSuccessListener(v -> callback.onSuccess(true))
                            .addOnFailureListener(e -> 
                                callback.onError("Lỗi khi cập nhật số lượng bài hát: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e -> callback.onError("Lỗi khi thêm bài hát vào danh sách yêu thích: " + e.getMessage()));
    }
} 