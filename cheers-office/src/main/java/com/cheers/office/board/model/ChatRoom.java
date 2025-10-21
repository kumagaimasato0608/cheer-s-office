package com.cheers.office.board.model;

import java.util.List;

public class ChatRoom {
    private String roomId;
    private String roomName;
    private List<String> members;
    private String createdAt;
    private String icon; // ★これが必要！

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; } // ★これがないとエラー！
}

