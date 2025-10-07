package com.cheers.office.board.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * フォトピン機能：写真アップロードフォームからのデータを受け取るDTO
 */
public class PhotoUploadForm {

    // 1. ファイル本体
    // HTMLの <input type="file" name="file"> に対応
    private MultipartFile file;

    // 2. 写真の説明文
    // HTMLの <input type="text" name="caption"> に対応
    private String caption;

    // 3. 地図座標 (緯度)
    // Leaflet/OSMからJavaScript経由で送信される緯度データ
    private double latitude;

    // 4. 地図座標 (経度)
    // Leaflet/OSMからJavaScript経由で送信される経度データ
    private double longitude;

    // --- GetterとSetter ---
    
    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}