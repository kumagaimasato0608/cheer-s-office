package com.cheers.office.board.model;

import java.util.List; // ★ List のインポートを追加

import lombok.Data;

@Data // Getter, Setterなどを自動生成
public class CalendarEvent {
    private String id;          // イベントID
    private String title;       // タイトル
    private String start;       // 開始日時 (ISO 8601形式: "2025-10-09T10:00:00")
    private String end;         // 終了日時
    private String description; // 備考
    private String color;       // イベントの色
    private boolean allDay = false; // 終日イベントかどうか
    
    // ★★★ 追加: 作成者ID (必須) ★★★
    private String createdByUserId; 

    // ★★★ 追加: 共有メンバーIDリスト (任意) ★★★
    private List<String> sharedWithUserIds; 
}