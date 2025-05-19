package com.example.beomusic.utils;

import com.example.beomusic.models.Album;
import com.example.beomusic.models.AlbumSong;
import com.example.beomusic.models.Song;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlbumUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ALBUMS_COLLECTION = "albums";
    private static final String ALBUM_SONGS_COLLECTION = "album_songs";
    
    /**
     * Kiểm tra xem người dùng đã có album chưa
     * @param userId ID của người dùng
     * @return Task trả về DocumentSnapshot chứa thông tin album (nếu có)
     */
    public static Task<DocumentSnapshot> hasUserAlbum(String userId) {
        String albumId = Album.generateAlbumId(userId);
        return db.collection(ALBUMS_COLLECTION).document(albumId).get();
    }
    
    /**
     * Tạo album mới cho người dùng
     * @param userId ID của người dùng
     * @return Task trả về kết quả của thao tác tạo album
     */
    public static Task<Void> createAlbumForUser(String userId) {
        Album album = new Album(userId);
        return db.collection(ALBUMS_COLLECTION).document(album.getAlbumId()).set(album);
    }
    
    /**
     * Xóa album của người dùng
     * @param userId ID của người dùng
     * @return Task trả về kết quả của thao tác xóa album
     */
    public static Task<Void> deleteUserAlbum(String userId) {
        String albumId = Album.generateAlbumId(userId);
        return db.collection(ALBUMS_COLLECTION).document(albumId).delete();
    }
    
    /**
     * Thêm bài hát vào album của người dùng
     * @param userId ID của người dùng
     * @param songId ID của bài hát
     * @return Task trả về kết quả của thao tác thêm bài hát
     */
    public static Task<Void> addSongToAlbum(String userId, String songId) {
        String albumId = Album.generateAlbumId(userId);
        AlbumSong albumSong = new AlbumSong(albumId, songId);
        return db.collection(ALBUM_SONGS_COLLECTION).document(albumSong.getId()).set(albumSong);
    }
    
    /**
     * Xóa bài hát khỏi album của người dùng
     * @param userId ID của người dùng
     * @param songId ID của bài hát
     * @return Task trả về kết quả của thao tác xóa bài hát
     */
    public static Task<Void> removeSongFromAlbum(String userId, String songId) {
        String albumId = Album.generateAlbumId(userId);
        String albumSongId = AlbumSong.generateId(albumId, songId);
        return db.collection(ALBUM_SONGS_COLLECTION).document(albumSongId).delete();
    }
    
    /**
     * Lấy danh sách bài hát trong album của người dùng
     * @param userId ID của người dùng
     * @return Task trả về QuerySnapshot chứa danh sách các AlbumSong
     */
    public static Task<QuerySnapshot> getAlbumSongs(String userId) {
        String albumId = Album.generateAlbumId(userId);
        return db.collection(ALBUM_SONGS_COLLECTION)
                .whereEqualTo("albumId", albumId)
                .get();
    }
    
    /**
     * Kiểm tra xem một bài hát đã được thêm vào album của người dùng chưa
     * @param userId ID của người dùng
     * @param songId ID của bài hát
     * @return Task trả về DocumentSnapshot chứa thông tin albumSong (nếu có)
     */
    public static Task<DocumentSnapshot> isSongInAlbum(String userId, String songId) {
        String albumId = Album.generateAlbumId(userId);
        String albumSongId = AlbumSong.generateId(albumId, songId);
        return db.collection(ALBUM_SONGS_COLLECTION).document(albumSongId).get();
    }
} 