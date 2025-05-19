package com.example.beomusic.models;

import java.util.Date;

public class User {
    private String userId;
    private String username;
    private String email;
    private String passwordHash; // mật khẩu đã băm
    private String fullName;
    private String refreshToken; // Token để nhớ đăng nhập
    private Date tokenExpiry; // Thời gian hết hạn token (30 ngày)
    private Date createdAt; // Thời gian tạo tài khoản
    private Date lastLogin; // Thời gian đăng nhập gần nhất
    private boolean isActive; // Trạng thái tài khoản

    public User() { } // Constructor rỗng cần thiết cho Firebase

    public User(String userId, String username, String email, String fullName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = new Date();
        this.isActive = true;
    }

    // Constructor đầy đủ
    public User(String userId, String username, String email, String passwordHash, String fullName, 
                String refreshToken, Date tokenExpiry, Date createdAt, Date lastLogin, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.refreshToken = refreshToken;
        this.tokenExpiry = tokenExpiry;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.isActive = isActive;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRefreshToken() { return refreshToken; }
    public Date getTokenExpiry() { return tokenExpiry; }
    public Date getCreatedAt() { return createdAt; }
    public Date getLastLogin() { return lastLogin; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTokenExpiry(Date tokenExpiry) { this.tokenExpiry = tokenExpiry; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }
    public void setActive(boolean active) { isActive = active; }

    // Phương thức tiện ích
    public boolean isTokenValid() {
        if (refreshToken == null || tokenExpiry == null) {
            return false;
        }
        return tokenExpiry.after(new Date());
    }

    // Phương thức để cập nhật token đăng nhập với thời hạn 30 ngày
    public void updateRefreshToken(String newToken) {
        this.refreshToken = newToken;
        
        // Tạo thời gian hết hạn 30 ngày kể từ hiện tại
        Date now = new Date();
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 ngày tính bằng mili giây
        this.tokenExpiry = new Date(now.getTime() + thirtyDaysInMillis);
        
        // Cập nhật thời gian đăng nhập gần nhất
        this.lastLogin = now;
    }

    // Phương thức để xóa token khi đăng xuất
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.tokenExpiry = null;
    }
}
