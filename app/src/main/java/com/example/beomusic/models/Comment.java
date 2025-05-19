package com.example.beomusic.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    @DocumentId
    private String commentId;
    private String userId;
    private String songId;
    private String content;
    private String username;
    @ServerTimestamp
    private Date timestamp;
    private String userPhotoUrl;

    // Empty constructor for Firestore
    public Comment() {
    }

    public Comment(String userId, String songId, String content, String username) {
        this.userId = userId;
        this.songId = songId;
        this.content = content;
        this.username = username;
    }

    // Constructor with all fields
    public Comment(String commentId, String userId, String songId, String content, String username, Date timestamp, String userPhotoUrl) {
        this.commentId = commentId;
        this.userId = userId;
        this.songId = songId;
        this.content = content;
        this.username = username;
        this.timestamp = timestamp;
        this.userPhotoUrl = userPhotoUrl;
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserPhotoUrl() {
        return userPhotoUrl;
    }

    public void setUserPhotoUrl(String userPhotoUrl) {
        this.userPhotoUrl = userPhotoUrl;
    }
} 