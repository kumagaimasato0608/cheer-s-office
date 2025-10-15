package com.cheers.office.board.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoPin {
    private String pinId;         // ピンの一意識別子
    private Location location;    // ピンの地理座標 (緯度・経度)
    private String title;         // ピンのタイトル
    private String description;   // ピンの詳細説明
    private String createdBy;     // ピンを作成したユーザーのID
    private String createdDate;   // ピンの作成日時 (ISO 8601形式など)
    private List<Photo> photos = new ArrayList<>();
    private List<Comment> comments = new ArrayList<>();
    private int bonusPoints = 0;  // このピンで獲得したボーナスポイント

    // ★★★ このフィールドを追加 ★★★
    private String season;        // "YYYY-MM"形式 (例: "2025-10")

    // Lombokの@Dataがゲッター/セッターを自動生成しますが、
    // 明示的に初期化を保証するコンストラクタも保持
    public PhotoPin(String pinId, Location location, String title, String description, String createdBy, String createdDate) {
        this.pinId = pinId;
        this.location = location;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.photos = new ArrayList<>(); // 必ず初期化
        this.comments = new ArrayList<>(); // コメントリストも初期化
        this.bonusPoints = 0; // bonusPointsも初期化
        this.season = null; // seasonフィールドも初期化
    }

    // photosリストがnullになることを防ぐためのセッター (Lombokが生成するが、念のため)
    public void setPhotos(List<Photo> photos) {
        this.photos = (photos != null) ? photos : new ArrayList<>();
    }
    
    // getPhotos(), getSeason(), setSeason() などは @Data が自動生成します
}