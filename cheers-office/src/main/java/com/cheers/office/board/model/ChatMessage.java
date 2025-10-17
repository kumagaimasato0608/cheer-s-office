package com.cheers.office.board.model;

public class ChatMessage {
    private String roomId;
    private String userId;
    private String userName;
    private String content;
    private String timestamp;
    private String icon;
    
    // ✅ メッセージの種類 ("TEXT" or "IMAGE") を格納
    private String type;
    
    // ✅ 画像に添えるキャプション (テキスト) を格納
    private String caption;


    // --- 以下、Getter/Setter ---

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

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    // ✅ 追加したフィールドのGetter/Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}