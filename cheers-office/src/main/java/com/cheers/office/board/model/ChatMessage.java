package com.cheers.office.board.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String roomId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;

    public ChatMessage() { }

    public ChatMessage(String roomId, String sender, String content, LocalDateTime timestamp) {
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    // --- GetterとSetter (Eclipseで自動生成する推奨) ---
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChatMessage{" +
               "roomId='" + roomId + '\'' +
               ", sender='" + sender + '\'' +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}