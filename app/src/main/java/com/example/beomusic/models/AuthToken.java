package com.example.beomusic.models;

import java.util.Date;

public class AuthToken {
    private String userId;
    private String token;
    private Date createdAt;
    private Date expiresAt; // 30 ngày sau khi tạo
    private String deviceInfo; // Thông tin thiết bị đăng nhập

    public AuthToken() { } // Constructor rỗng cần thiết cho Firebase

    // Constructor cơ bản
    public AuthToken(String userId, String token, String deviceInfo) {
        this.userId = userId;
        this.token = token;
        this.deviceInfo = deviceInfo;
        this.createdAt = new Date();
        
        // Tạo thời gian hết hạn 30 ngày kể từ hiện tại
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 ngày tính bằng mili giây
        this.expiresAt = new Date(createdAt.getTime() + thirtyDaysInMillis);
    }

    // Constructor đầy đủ
    public AuthToken(String userId, String token, Date createdAt, Date expiresAt, String deviceInfo) {
        this.userId = userId;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getToken() { return token; }
    public Date getCreatedAt() { return createdAt; }
    public Date getExpiresAt() { return expiresAt; }
    public String getDeviceInfo() { return deviceInfo; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setToken(String token) { this.token = token; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    // Phương thức kiểm tra token có còn hiệu lực không
    public boolean isValid() {
        return expiresAt != null && expiresAt.after(new Date());
    }

    // Phương thức gia hạn token thêm 30 ngày
    public void extend() {
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        this.expiresAt = new Date(new Date().getTime() + thirtyDaysInMillis);
    }

    // Phương thức tính số ngày còn lại trước khi token hết hạn
    public int getDaysUntilExpiration() {
        if (expiresAt == null) return 0;
        
        long now = new Date().getTime();
        long expiration = expiresAt.getTime();
        long diff = expiration - now;
        
        if (diff <= 0) return 0;
        
        // Chuyển đổi mili giây thành ngày
        return (int) (diff / (24 * 60 * 60 * 1000));
    }
}
