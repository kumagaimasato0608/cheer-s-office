package com.cheers.office.board.model;

import java.time.LocalDateTime;

// ★もしJPA (データベース) に保存するなら、@Entity, @Id, @GeneratedValue などが必要
public class Log {
    private String id; // ユニークなID (InMemoryChatLogRepositoryで自動生成)
    private String roomId; // どのルームのログか
    private String userId; // 誰が送信したか (sender)
    private String log;    // メッセージ内容 (content)
    private LocalDateTime timestamp; // 送信日時

    // デフォルトコンストラクタ (必須)
    public Log() {}

    // GetterとSetter (すべて必須)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}