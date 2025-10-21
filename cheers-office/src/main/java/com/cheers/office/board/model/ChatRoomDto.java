package com.cheers.office.board.model;

/**
 * APIレスポンス用のチャットルーム情報DTO.
 * 既存のChatRoomモデルに未読メッセージ数を追加します。
 */
public class ChatRoomDto extends ChatRoom {

    private int unreadCount;

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * ChatRoomオブジェクトからDTOを生成するためのコンストラクタ.
     * @param room 元となるChatRoomオブジェクト
     */
    public ChatRoomDto(ChatRoom room) {
        this.setRoomId(room.getRoomId());
        this.setRoomName(room.getRoomName());
        this.setMembers(room.getMembers());
        this.setIcon(room.getIcon());
        this.setCreatedAt(room.getCreatedAt());
    }
}