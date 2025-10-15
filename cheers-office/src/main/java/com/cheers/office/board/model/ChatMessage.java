package com.cheers.office.board.model;

public class ChatMessage {
    private String roomId;
    private String userId;
    private String userName;
    private String content;
    private String timestamp;

    // ★ 追加：プロフィールアイコン
    private String icon;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // ★ getter/setter 追加
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
