package com.example.beomusic.repositories;

import com.example.beomusic.models.Album;
import com.example.beomusic.models.AlbumSong;
import com.example.beomusic.models.Song;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Repository class for handling album operations
 */
public class AlbumRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public interface AlbumCallback {
        void onSuccess(Album album);
        void onError(String errorMessage);
    }

    public interface AlbumsCallback {
        void onSuccess(List<Album> albums);
        void onError(String errorMessage);
    }

    public interface SongsCallback {
        void onSuccess(List<Song> songs);
        void onError(String errorMessage);
    }

    public AlbumRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Create a new album
     * @param title Album title
     * @param description Album description
     * @param userId User ID of the creator
     * @param coverImageBytes Cover image bytes (optional)
     * @param callback Callback for result
     */
    public void createAlbum(String title, String description, String userId, 
                           byte[] coverImageBytes, AlbumCallback callback) {
        // Generate unique album ID
        String albumId = UUID.randomUUID().toString();
        
        // Create album object
        Album album = new Album(albumId, title, description, new Date(), userId);
        
        // If cover image is provided, upload it first
        if (coverImageBytes != null && coverImageBytes.length > 0) {
            uploadCoverImage(albumId, coverImageBytes, new AlbumCallback() {
                @Override
                public void onSuccess(Album updatedAlbum) {
                    // Update album with cover image URL
                    album.setCoverImageUrl(updatedAlbum.getCoverImageUrl());
                    
                    // Save album to Firestore
                    saveAlbum(album, callback);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError("Lỗi khi tải lên ảnh bìa: " + errorMessage);
                }
            });
        } else {
            // Save album without cover image
            saveAlbum(album, callback);
        }
    }

    /**
     * Update an existing album
     * @param album Album to update
     * @param coverImageBytes New cover image bytes (optional)
     * @param callback Callback for result
     */
    public void updateAlbum(Album album, byte[] coverImageBytes, AlbumCallback callback) {
        // If cover image is provided, upload it first
        if (coverImageBytes != null && coverImageBytes.length > 0) {
            uploadCoverImage(album.getAlbumId(), coverImageBytes, new AlbumCallback() {
                @Override
                public void onSuccess(Album updatedAlbum) {
                    // Update album with new cover image URL
                    album.setCoverImageUrl(updatedAlbum.getCoverImageUrl());
                    
                    // Save updated album to Firestore
                    saveAlbum(album, callback);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError("Lỗi khi tải lên ảnh bìa: " + errorMessage);
                }
            });
        } else {
            // Save album without changing cover image
            saveAlbum(album, callback);
        }
    }

    /**
     * Delete an album
     * @param albumId Album ID to delete
     * @param callback Callback for result
     */
    public void deleteAlbum(String albumId, AlbumCallback callback) {
        // First delete all album-song relationships
        firestore.collection("album_songs")
                .whereEqualTo("albumId", albumId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Delete each album-song document
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().delete();
                    }
                    
                    // Then delete the album document
                    firestore.collection("albums").document(albumId)
                            .delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> 
                                callback.onError("Lỗi khi xóa album: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi xóa bài hát trong album: " + e.getMessage())
                );
    }

    /**
     * Get all albums for a user
     * @param userId User ID
     * @param callback Callback for result
     */
    public void getUserAlbums(String userId, AlbumsCallback callback) {
        firestore.collection("albums")
                .whereEqualTo("userId", userId)
                .orderBy("createdDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Album> albums = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Album album = doc.toObject(Album.class);
                        if (album != null) {
                            albums.add(album);
                        }
                    }
                    callback.onSuccess(albums);
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi lấy danh sách album: " + e.getMessage())
                );
    }

    /**
     * Get a specific album by ID
     * @param albumId Album ID
     * @param callback Callback for result
     */
    public void getAlbum(String albumId, AlbumCallback callback) {
        firestore.collection("albums").document(albumId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Album album = documentSnapshot.toObject(Album.class);
                    if (album != null) {
                        callback.onSuccess(album);
                    } else {
                        callback.onError("Không tìm thấy album");
                    }
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi lấy thông tin album: " + e.getMessage())
                );
    }

    /**
     * Add a song to an album
     * @param albumId Album ID
     * @param songId Song ID
     * @param callback Callback for result
     */
    public void addSongToAlbum(String albumId, String songId, AlbumCallback callback) {
        // First check if the song already exists in the album
        firestore.collection("album_songs")
                .whereEqualTo("albumId", albumId)
                .whereEqualTo("songId", songId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onError("Bài hát đã tồn tại trong album");
                        return;
                    }
                    
                    // Create album-song relationship
                    String relationId = UUID.randomUUID().toString();
                    AlbumSong albumSong = new AlbumSong(relationId, albumId, songId, new Date());
                    
                    firestore.collection("album_songs").document(relationId)
                            .set(albumSong)
                            .addOnSuccessListener(aVoid -> {
                                // Update song count in album
                                DocumentReference albumRef = firestore.collection("albums").document(albumId);
                                firestore.runTransaction(transaction -> {
                                    DocumentSnapshot albumSnapshot = transaction.get(albumRef);
                                    Album album = albumSnapshot.toObject(Album.class);
                                    if (album != null) {
                                        album.incrementSongCount();
                                        transaction.update(albumRef, "songCount", album.getSongCount());
                                        transaction.update(albumRef, "updatedDate", new Date());
                                    }
                                    return null;
                                })
                                .addOnSuccessListener(aVoid2 -> {
                                    // Get updated album
                                    getAlbum(albumId, callback);
                                })
                                .addOnFailureListener(e -> 
                                    callback.onError("Lỗi khi cập nhật số lượng bài hát: " + e.getMessage())
                                );
                            })
                            .addOnFailureListener(e -> 
                                callback.onError("Lỗi khi thêm bài hát vào album: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi kiểm tra bài hát: " + e.getMessage())
                );
    }

    /**
     * Remove a song from an album
     * @param albumId Album ID
     * @param songId Song ID
     * @param callback Callback for result
     */
    public void removeSongFromAlbum(String albumId, String songId, AlbumCallback callback) {
        firestore.collection("album_songs")
                .whereEqualTo("albumId", albumId)
                .whereEqualTo("songId", songId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onError("Bài hát không tồn tại trong album");
                        return;
                    }
                    
                    // Delete album-song relationship
                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    doc.getReference().delete()
                            .addOnSuccessListener(aVoid -> {
                                // Update song count in album
                                DocumentReference albumRef = firestore.collection("albums").document(albumId);
                                firestore.runTransaction(transaction -> {
                                    DocumentSnapshot albumSnapshot = transaction.get(albumRef);
                                    Album album = albumSnapshot.toObject(Album.class);
                                    if (album != null) {
                                        album.decrementSongCount();
                                        transaction.update(albumRef, "songCount", album.getSongCount());
                                        transaction.update(albumRef, "updatedDate", new Date());
                                    }
                                    return null;
                                })
                                .addOnSuccessListener(aVoid2 -> {
                                    // Get updated album
                                    getAlbum(albumId, callback);
                                })
                                .addOnFailureListener(e -> 
                                    callback.onError("Lỗi khi cập nhật số lượng bài hát: " + e.getMessage())
                                );
                            })
                            .addOnFailureListener(e -> 
                                callback.onError("Lỗi khi xóa bài hát khỏi album: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi kiểm tra bài hát: " + e.getMessage())
                );
    }

    /**
     * Get all songs in an album
     * @param albumId Album ID
     * @param callback Callback for result
     */
    public void getAlbumSongs(String albumId, SongsCallback callback) {
        firestore.collection("album_songs")
                .whereEqualTo("albumId", albumId)
                .orderBy("addedDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    List<String> songIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        AlbumSong albumSong = doc.toObject(AlbumSong.class);
                        if (albumSong != null) {
                            songIds.add(albumSong.getSongId());
                        }
                    }
                    
                    // Get song details for each song ID
                    getSongsByIds(songIds, callback);
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi lấy danh sách bài hát: " + e.getMessage())
                );
    }

    /**
     * Get songs by their IDs
     * @param songIds List of song IDs
     * @param callback Callback for result
     */
    private void getSongsByIds(List<String> songIds, SongsCallback callback) {
        if (songIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        
        // Firestore has a limit of 10 items in a whereIn query
        // So we need to batch the requests if there are more than 10 songs
        List<Song> allSongs = new ArrayList<>();
        List<List<String>> batches = new ArrayList<>();
        
        // Split song IDs into batches of 10
        for (int i = 0; i < songIds.size(); i += 10) {
            batches.add(songIds.subList(i, Math.min(i + 10, songIds.size())));
        }
        
        // Counter for completed batches
        final int[] completedBatches = {0};
        final boolean[] hasError = {false};
        
        // Process each batch
        for (List<String> batch : batches) {
            firestore.collection("songs")
                    .whereIn("songId", batch)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Song song = doc.toObject(Song.class);
                            if (song != null) {
                                allSongs.add(song);
                            }
                        }
                        
                        completedBatches[0]++;
                        
                        // If all batches are complete, return the results
                        if (completedBatches[0] == batches.size() && !hasError[0]) {
                            callback.onSuccess(allSongs);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!hasError[0]) {
                            hasError[0] = true;
                            callback.onError("Lỗi khi lấy thông tin bài hát: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Upload a cover image for an album
     * @param albumId Album ID
     * @param imageBytes Image bytes
     * @param callback Callback for result
     */
    private void uploadCoverImage(String albumId, byte[] imageBytes, AlbumCallback callback) {
        // Create a storage reference
        StorageReference storageRef = storage.getReference();
        StorageReference albumImageRef = storageRef.child("album_covers/" + albumId + ".jpg");
        
        // Upload the image
        UploadTask uploadTask = albumImageRef.putBytes(imageBytes);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            
            // Get the download URL
            return albumImageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Create a temporary album with the image URL
                Album tempAlbum = new Album();
                tempAlbum.setCoverImageUrl(task.getResult().toString());
                callback.onSuccess(tempAlbum);
            } else {
                callback.onError("Lỗi khi tải lên ảnh bìa");
            }
        });
    }

    /**
     * Save an album to Firestore
     * @param album Album to save
     * @param callback Callback for result
     */
    private void saveAlbum(Album album, AlbumCallback callback) {
        firestore.collection("albums").document(album.getAlbumId())
                .set(album)
                .addOnSuccessListener(aVoid -> callback.onSuccess(album))
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi lưu album: " + e.getMessage())
                );
    }
}
