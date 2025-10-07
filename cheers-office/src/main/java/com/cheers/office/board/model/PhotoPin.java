package com.cheers.office.board.model;

import java.time.LocalDateTime;
// Lombokを使用している場合は @Data を使うとGetter/Setterは不要
// import lombok.Data; 

/**
 * フォトピン機能：地図上のピンと関連付けられた写真データ
 */
// @Data // Lombokを使用しない場合は手動でGetter/Setterを記述
public class PhotoPin {
    
    private String pinId;
    private String userId;        // 投稿者
    private String photoPath;     // サーバー上の公開ファイルパス
    private String caption;       // 写真の説明
    private double latitude;      // 緯度
    private double longitude;     // 経度
    private LocalDateTime createdAt; // 投稿日時

    // --- GetterとSetter ---
    // PhotopinServiceで利用しているsetPinId, setUserId, setPhotoPath, setCaption, setLatitude, setLongitude, setCreatedAt 
    // および、対応する Getter メソッドをすべて実装する必要があります。
    
    public String getPinId() { return pinId; }
    public void setPinId(String pinId) { this.pinId = pinId; }
    // ... (他のフィールドについても同様にGetter/Setterを記述してください) ...
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}