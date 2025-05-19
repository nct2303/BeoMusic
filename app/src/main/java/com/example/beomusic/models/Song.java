package com.example.beomusic.models;

import java.io.Serializable;
import java.util.Date;

public class Song implements Serializable {
    private String songId;
    private String title;
    private String artist;
    private int duration; // Giây
    private String sourceType; // "YouTube" hoặc "Local"
    private String filePath;
    private String thumbnailUrl; // URL hình ảnh thumbnail
    private String genre; // Thể loại nhạc
    private Date addedDate; // Ngày thêm vào hệ thống
    private long playCount; // Số lần phát

    public Song() { } // Constructor rỗng cần thiết cho Firebase

    // Constructor cơ bản
    public Song(String songId, String title, String artist, int duration, String sourceType, String filePath) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.sourceType = sourceType;
        this.filePath = filePath;
        this.addedDate = new Date();
        this.playCount = 0;
    }

    // Constructor đầy đủ
    public Song(String songId, String title, String artist, int duration, String sourceType,
                String filePath, String thumbnailUrl, String genre,
                Date addedDate, long playCount) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.sourceType = sourceType;
        this.filePath = filePath;
        this.thumbnailUrl = thumbnailUrl;
        this.genre = genre;
        this.addedDate = addedDate;
        this.playCount = playCount;
    }

    // Getters
    public String getSongId() { return songId; }
    public String getId() { return songId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getDuration() { return duration; }
    public String getSourceType() { return sourceType; }
    public String getFilePath() { return filePath; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getGenre() { return genre; }
    public Date getAddedDate() { return addedDate; }
    public long getPlayCount() { return playCount; }

    // Setters
    public void setSongId(String songId) { this.songId = songId; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setAddedDate(Date addedDate) { this.addedDate = addedDate; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }

    // Phương thức tiện ích
    public void incrementPlayCount() {
        this.playCount++;
    }

    // Phương thức kiểm tra xem bài hát có phải từ YouTube không
    public boolean isFromYouTube() {
        return "YouTube".equalsIgnoreCase(sourceType);
    }

    // Phương thức kiểm tra xem bài hát có phải là file local không
    public boolean isLocalFile() {
        return "Local".equalsIgnoreCase(sourceType) && filePath != null && !filePath.isEmpty();
    }

    // Phương thức chuyển đổi thời lượng sang định dạng mm:ss
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
