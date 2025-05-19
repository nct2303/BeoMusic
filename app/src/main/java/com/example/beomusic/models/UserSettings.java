package com.example.beomusic.models;

public class UserSettings {
    private String userId;
    private boolean darkMode;
    private String language;
    private boolean autoPlay;
    private int audioQuality; // Chất lượng âm thanh (enum hoặc int)
    private boolean downloadOverWifiOnly;
    private boolean cacheEnabled;
    private int cacheSize; // Kích thước cache tối đa (MB)

    // Constants cho audioQuality
    public static final int AUDIO_QUALITY_LOW = 0;
    public static final int AUDIO_QUALITY_MEDIUM = 1;
    public static final int AUDIO_QUALITY_HIGH = 2;

    public UserSettings() { } // Constructor rỗng cần thiết cho Firebase

    // Constructor cơ bản với các giá trị mặc định
    public UserSettings(String userId) {
        this.userId = userId;
        this.darkMode = false;
        this.language = "vi"; // Tiếng Việt mặc định
        this.autoPlay = true;
        this.audioQuality = AUDIO_QUALITY_MEDIUM;
        this.downloadOverWifiOnly = true;
        this.cacheEnabled = true;
        this.cacheSize = 500; // 500MB mặc định
    }

    // Constructor đầy đủ
    public UserSettings(String userId, boolean darkMode, String language, boolean autoPlay,
                       int audioQuality, boolean downloadOverWifiOnly, boolean cacheEnabled, int cacheSize) {
        this.userId = userId;
        this.darkMode = darkMode;
        this.language = language;
        this.autoPlay = autoPlay;
        this.audioQuality = audioQuality;
        this.downloadOverWifiOnly = downloadOverWifiOnly;
        this.cacheEnabled = cacheEnabled;
        this.cacheSize = cacheSize;
    }

    // Getters
    public String getUserId() { return userId; }
    public boolean isDarkMode() { return darkMode; }
    public String getLanguage() { return language; }
    public boolean isAutoPlay() { return autoPlay; }
    public int getAudioQuality() { return audioQuality; }
    public boolean isDownloadOverWifiOnly() { return downloadOverWifiOnly; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public int getCacheSize() { return cacheSize; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    public void setLanguage(String language) { this.language = language; }
    public void setAutoPlay(boolean autoPlay) { this.autoPlay = autoPlay; }
    public void setAudioQuality(int audioQuality) { this.audioQuality = audioQuality; }
    public void setDownloadOverWifiOnly(boolean downloadOverWifiOnly) { this.downloadOverWifiOnly = downloadOverWifiOnly; }
    public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }

    // Phương thức tiện ích để lấy tên chất lượng âm thanh
    public String getAudioQualityName() {
        switch (audioQuality) {
            case AUDIO_QUALITY_LOW:
                return "Thấp";
            case AUDIO_QUALITY_MEDIUM:
                return "Trung bình";
            case AUDIO_QUALITY_HIGH:
                return "Cao";
            default:
                return "Không xác định";
        }
    }

    // Phương thức để reset về cài đặt mặc định
    public void resetToDefaults() {
        this.darkMode = false;
        this.language = "vi";
        this.autoPlay = true;
        this.audioQuality = AUDIO_QUALITY_MEDIUM;
        this.downloadOverWifiOnly = true;
        this.cacheEnabled = true;
        this.cacheSize = 500;
    }
}
