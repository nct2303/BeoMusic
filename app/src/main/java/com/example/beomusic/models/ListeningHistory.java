package com.example.beomusic.models;

import java.util.Date;

public class ListeningHistory {
    private String id;
    private String userId;
    private String songId;
    private Date listenedAt; // Thời gian nghe
    private int listenDuration; // Thời gian nghe (giây)
    private boolean completed; // Đã nghe hết bài hay chưa

    public ListeningHistory() { } // Constructor rỗng cần thiết cho Firebase

    // Constructor cơ bản
    public ListeningHistory(String userId, String songId, int listenDuration, boolean completed) {
        this.userId = userId;
        this.songId = songId;
        this.listenedAt = new Date();
        this.listenDuration = listenDuration;
        this.completed = completed;
        // Tạo ID duy nhất dựa trên userId, songId và thời gian nghe
        this.id = userId + "_" + songId + "_" + listenedAt.getTime();
    }

    // Constructor đầy đủ
    public ListeningHistory(String id, String userId, String songId, Date listenedAt, 
                           int listenDuration, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.songId = songId;
        this.listenedAt = listenedAt;
        this.listenDuration = listenDuration;
        this.completed = completed;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getSongId() { return songId; }
    public Date getListenedAt() { return listenedAt; }
    public int getListenDuration() { return listenDuration; }
    public boolean isCompleted() { return completed; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setSongId(String songId) { this.songId = songId; }
    public void setListenedAt(Date listenedAt) { this.listenedAt = listenedAt; }
    public void setListenDuration(int listenDuration) { this.listenDuration = listenDuration; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    // Phương thức tiện ích để tính phần trăm bài hát đã nghe
    public double getCompletionPercentage(int totalDuration) {
        if (totalDuration <= 0) return 0;
        return Math.min(100.0, (listenDuration * 100.0) / totalDuration);
    }

    // Phương thức kiểm tra xem bài hát có được nghe đủ lâu để tính là đã nghe không
    // (Ví dụ: nghe ít nhất 30% bài hát)
    public boolean isSignificantListen(int totalDuration) {
        return getCompletionPercentage(totalDuration) >= 30.0;
    }
}
