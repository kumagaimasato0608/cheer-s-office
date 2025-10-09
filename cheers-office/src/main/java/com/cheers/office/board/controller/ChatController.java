package com.cheers.office.board.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.ChatMessage;
import com.cheers.office.board.model.ChatRoom;
import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final String DATA_DIR = "src/main/resources/data/";
    private static final String ROOM_PATH = DATA_DIR + "rooms.json";
    private static final String CHAT_DIR = DATA_DIR + "chat_logs/";
    private static final String USER_PATH = DATA_DIR + "user.json";

    private final ObjectMapper mapper = new ObjectMapper();

    // 💬 メッセージ送受信
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessage incomingMessage) throws IOException {

        // --- 送信データを確認 ---
        System.out.println("💬 受信: " + incomingMessage.getUserName() + " / icon=" + incomingMessage.getIcon());

        // タイムスタンプを付与
        incomingMessage.setTimestamp(LocalDateTime.now().toString());

        // アイコンが空の場合 → user.json から補完
        if (incomingMessage.getIcon() == null || incomingMessage.getIcon().isEmpty()) {
            incomingMessage.setIcon(getUserIconById(incomingMessage.getUserId()));
        }

        // ログファイルの保存
        File dir = new File(CHAT_DIR);
        if (!dir.exists()) dir.mkdirs();
        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");

        List<ChatMessage> logs = new ArrayList<>();
        if (logFile.exists()) {
            logs = mapper.readValue(logFile, new TypeReference<List<ChatMessage>>() {});
        }

        logs.add(incomingMessage);
        mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);

        return incomingMessage;
    }

    // 📜 チャット履歴取得
    @GetMapping("/chat/{roomId}")
    public List<ChatMessage> getChatLogs(@PathVariable String roomId) throws IOException {
        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (!logFile.exists()) return new ArrayList<>();

        List<ChatMessage> logs = mapper.readValue(logFile, new TypeReference<List<ChatMessage>>() {});
        // アイコン補完
        for (ChatMessage m : logs) {
            if (m.getIcon() == null || m.getIcon().isEmpty()) {
                m.setIcon(getUserIconById(m.getUserId()));
            }
        }
        return logs;
    }

    // 🏠 ルーム一覧
    @GetMapping("/rooms")
    public List<ChatRoom> getRooms() throws IOException {
        File file = new File(ROOM_PATH);
        if (!file.exists()) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<List<ChatRoom>>() {});
    }

    // ➕ ルーム作成（重複禁止）
    @PostMapping("/rooms")
    public ChatRoom createRoom(@RequestBody ChatRoom newRoom) throws IOException {
        File file = new File(ROOM_PATH);
        List<ChatRoom> rooms = new ArrayList<>();
        if (file.exists()) {
            rooms = mapper.readValue(file, new TypeReference<List<ChatRoom>>() {});
        }

        if (newRoom.getMembers() != null && newRoom.getMembers().size() == 2) {
            for (ChatRoom r : rooms) {
                if (r.getMembers() != null
                        && r.getMembers().size() == 2
                        && r.getMembers().containsAll(newRoom.getMembers())
                        && newRoom.getMembers().containsAll(r.getMembers())) {
                    return r;
                }
            }
        }

        newRoom.setRoomId("r" + (rooms.size() + 1));
        newRoom.setCreatedAt(LocalDateTime.now().toString());
        rooms.add(newRoom);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);

        return newRoom;
    }

    // ❌ ルーム削除（履歴も削除）
    @DeleteMapping("/rooms/{roomId}")
    public boolean deleteRoom(@PathVariable String roomId) throws IOException {
        File file = new File(ROOM_PATH);
        List<ChatRoom> rooms = new ArrayList<>();
        if (file.exists()) {
            rooms = mapper.readValue(file, new TypeReference<List<ChatRoom>>() {});
        }

        rooms.removeIf(r -> r.getRoomId().equals(roomId));
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);

        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (logFile.exists()) logFile.delete();

        System.out.println("🗑️ ルーム削除: " + roomId);
        return true;
    }

    // 👤 IDからアイコン取得
    private String getUserIconById(String userId) throws IOException {
        File file = new File(USER_PATH);
        if (!file.exists()) return "/images/default-avatar.png";

        List<User> users = mapper.readValue(file, new TypeReference<List<User>>() {});
        for (User u : users) {
            if (u.getUserId().equals(userId)) {
                return (u.getIcon() != null && !u.getIcon().isEmpty())
                        ? u.getIcon()
                        : "/images/default-avatar.png";
            }
        }
        return "/images/default-avatar.png";
    }
}

