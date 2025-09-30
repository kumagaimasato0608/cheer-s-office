package com.cheers.office.board.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty; // JSONプロパティ名とフィールド名が異なる場合に使用

public class Room {
    @JsonProperty("room_id")    // JSONの "room_id" に対応
    private String roomId;
    @JsonProperty("room_name")  // JSONの "room_name" に対応
    private String roomName;
    @JsonProperty("room_icon")  // JSONの "room_icon" に対応
    private String roomIcon;
    private List<String> members; // JSONの "members" に対応

    // デフォルトコンストラクタ (必須)
    public Room() {}

    // GetterとSetter (すべて必須)
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomIcon() { return roomIcon; }
    public void setRoomIcon(String roomIcon) { this.roomIcon = roomIcon; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
}