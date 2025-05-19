package com.example.beomusic.models;

import java.util.Date;

public class Album {
    private String albumId;
    private String userId;
    private String title;
    private String description;
    private int songCount;
    private Date createdDate;
    private Date updatedDate;
    private String coverImageUrl;

    // Empty constructor required for Firebase
    public Album() {}

    // Constructor for creating a favorites album for a user
    public Album(String userId) {
        this.userId = userId;
        this.albumId = generateFavoritesAlbumId(userId);
        this.title = "Favorite Songs";
        this.description = "Your favorite songs";
        this.songCount = 0;
        this.createdDate = new Date();
        this.updatedDate = this.createdDate;
    }

    // Constructor for creating a regular album
    public Album(String userId, String title, String coverImageUrl) {
        this.userId = userId;
        this.albumId = userId + "_" + System.currentTimeMillis();
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.songCount = 0;
        this.createdDate = new Date();
        this.updatedDate = this.createdDate;
    }
    
    // Constructor used by AlbumRepository.createAlbum
    public Album(String albumId, String title, String description, Date createdDate, String userId) {
        this.albumId = albumId;
        this.title = title;
        this.description = description;
        this.createdDate = createdDate;
        this.updatedDate = createdDate;
        this.userId = userId;
        this.songCount = 0;
    }

    // Getters
    public String getAlbumId() { return albumId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getSongCount() { return songCount; }
    public Date getCreatedDate() { return createdDate; }
    public Date getUpdatedDate() { return updatedDate; }
    public String getCoverImageUrl() { return coverImageUrl; }

    // Setters
    public void setAlbumId(String albumId) { this.albumId = albumId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSongCount(int songCount) { this.songCount = songCount; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    public void setUpdatedDate(Date updatedDate) { this.updatedDate = updatedDate; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    // Increment song count when adding a song
    public void incrementSongCount() {
        this.songCount++;
        this.updatedDate = new Date();
    }

    // Decrement song count when removing a song
    public void decrementSongCount() {
        if (this.songCount > 0) {
            this.songCount--;
            this.updatedDate = new Date();
        }
    }

    // Utility method to generate an album ID for a user
    public static String generateAlbumId(String userId) {
        return userId + "_album";
    }

    // Utility method to generate a consistent album ID for a user's favorites
    public static String generateFavoritesAlbumId(String userId) {
        return userId + "_favorites";
    }
    
    // Helper method to check if an album is a favorites album
    public boolean isFavoritesAlbum() {
        return albumId != null && albumId.endsWith("_favorites");
    }
    
    // Helper method to get a user's favorites album ID
    public static String getFavoritesAlbumId(String userId) {
        return generateFavoritesAlbumId(userId);
    }
}
