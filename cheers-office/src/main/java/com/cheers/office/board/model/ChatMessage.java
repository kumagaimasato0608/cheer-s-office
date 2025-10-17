package com.cheers.office.board.model;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private String roomId;
    private String userId;
    private String userName;
    private String type;
    private String content;
    private String caption;
    private String timestamp;
    private String icon;
    
    // ✅ 既読機能のために2つのフィールドを追加
    private String messageId; // メッセージのユニークID
    private List<String> readBy = new ArrayList<>(); // 既読したユーザーIDのリスト

    // --- 以下、Getter/Setter ---

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public List<String> getReadBy() { return readBy; }
    public void setReadBy(List<String> readBy) { this.readBy = readBy; }
}