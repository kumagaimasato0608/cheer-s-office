package com.cheers.office.board.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.ChatMessage;
import com.cheers.office.board.model.ChatRoom;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.board.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final String DATA_DIR = "src/main/resources/data/";
    private static final String ROOM_FILE = DATA_DIR + "rooms.json";
    private static final String CHAT_DIR = DATA_DIR + "chat_logs/";

    @Value("${app.upload-dir.chat}")
    private String chatUploadDir;
    @Value("${app.upload-dir.group}")
    private String groupUploadDir;

    private final ObjectMapper mapper;
    private final UserRepository userRepo;
    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatController(UserRepository userRepo, ObjectMapper mapper, ChatService chatService, SimpMessagingTemplate simpMessagingTemplate) {
        this.userRepo = userRepo;
        this.mapper = mapper;
        this.chatService = chatService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    // ==============================
    //  WebSocket: メッセージ送信
    // ==============================
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload ChatMessage msg) throws IOException {
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now().toString());

        if (msg.getReadBy() == null) {
            msg.setReadBy(new ArrayList<>());
        }
        msg.getReadBy().add(msg.getUserId());

        if (msg.getIcon() == null || msg.getIcon().isEmpty()) {
            msg.setIcon(getUserIconById(msg.getUserId()));
        }

        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        List<ChatMessage> logs = logFile.exists()
                ? mapper.readValue(logFile, new TypeReference<>() {})
                : new ArrayList<>();
        logs.add(msg);
        mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
        
        simpMessagingTemplate.convertAndSend("/topic/chat/" + roomId, msg);
    }

    // ==============================
    //  WebSocket: 既読通知
    // ==============================
    @MessageMapping("/chat/{roomId}/read")
    public void markAsRead(@DestinationVariable String roomId, @Payload Map<String, String> payload) throws IOException {
        String userId = payload.get("userId");
        String messageId = payload.get("messageId");

        if (userId == null || messageId == null) {
            return; // 必要な情報がなければ何もしない
        }

        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (!logFile.exists()) {
            return;
        }

        List<ChatMessage> logs = mapper.readValue(logFile, new TypeReference<>() {});
        
        Optional<ChatMessage> targetMessageOpt = logs.stream()
                .filter(log -> messageId.equals(log.getMessageId()))
                .findFirst();

        if (targetMessageOpt.isPresent()) {
            ChatMessage targetMessage = targetMessageOpt.get();

            if (targetMessage.getReadBy() == null) {
                targetMessage.setReadBy(new ArrayList<>());
            }

            if (!targetMessage.getReadBy().contains(userId)) {
                targetMessage.getReadBy().add(userId);
                mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
            }

            simpMessagingTemplate.convertAndSend("/topic/chat/" + roomId,
                Map.of(
                    "type", "READ_UPDATE",
                    "messageId", targetMessage.getMessageId(),
                    "readBy", targetMessage.getReadBy()
                )
            );
        }
    }
    
    // ==============================
    //  HTTP API
    // ==============================
    @PostMapping("/chat/upload")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "ファイルが空です"));
        try {
            String imageUrl = chatService.saveImage(file, chatUploadDir, "/images/chat");
            return ResponseEntity.ok(Collections.singletonMap("imageUrl", imageUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "ファイルのアップロード中にエラーが発生しました"));
        }
    }

    @PostMapping("/rooms/uploadIcon")
    public ResponseEntity<Map<String, String>> uploadGroupIcon(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "ファイルが空です"));
        try {
            String iconUrl = chatService.saveImage(file, groupUploadDir, "/images/groups");
            return ResponseEntity.ok(Collections.singletonMap("iconUrl", iconUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "アイコンのアップロード中にエラーが発生しました"));
        }
    }

    @PostMapping("/rooms")
    public ChatRoom createRoom(@RequestBody ChatRoom requestData) throws IOException {
        File file = new File(ROOM_FILE);
        List<ChatRoom> rooms = file.exists() ? mapper.readValue(file, new TypeReference<>() {}) : new ArrayList<>();
        if (requestData.getMembers() != null && requestData.getMembers().size() == 2) {
            for (ChatRoom r : rooms) {
                if (r.getMembers() != null && r.getMembers().size() == 2 && r.getMembers().containsAll(requestData.getMembers())) return r;
            }
            ChatRoom newSingleRoom = new ChatRoom();
            newSingleRoom.setRoomId("r_" + UUID.randomUUID().toString().substring(0, 8));
            newSingleRoom.setCreatedAt(LocalDateTime.now().toString());
            newSingleRoom.setMembers(requestData.getMembers());
            String myUserId = requestData.getMembers().get(0);
            String otherId = requestData.getMembers().stream().filter(id -> !id.equals(myUserId)).findFirst().orElse("");
            newSingleRoom.setRoomName(getUserNameById(otherId));
            rooms.add(newSingleRoom);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);
            return newSingleRoom;
        } else if (requestData.getMembers() != null && requestData.getMembers().size() > 2) {
            ChatRoom newGroupRoom = new ChatRoom();
            newGroupRoom.setRoomId("g_" + UUID.randomUUID().toString().substring(0, 8));
            newGroupRoom.setCreatedAt(LocalDateTime.now().toString());
            newGroupRoom.setMembers(requestData.getMembers());
            newGroupRoom.setRoomName(requestData.getRoomName());
            newGroupRoom.setIcon(requestData.getIcon());
            rooms.add(newGroupRoom);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);
            return newGroupRoom;
        }
        throw new IOException("Invalid room creation request");
    }

    @GetMapping("/chat/{roomId}")
    public List<ChatMessage> getChatLogs(@PathVariable String roomId) throws IOException {
        File file = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (!file.exists()) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    @GetMapping("/rooms")
    public List<ChatRoom> getRooms() throws IOException {
        File file = new File(ROOM_FILE);
        if (!file.exists()) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    @DeleteMapping("/rooms/{roomId}")
    public boolean deleteRoom(@PathVariable String roomId) throws IOException {
        File file = new File(ROOM_FILE);
        if (!file.exists()) return false;
        List<ChatRoom> rooms = mapper.readValue(file, new TypeReference<>() {});
        rooms.removeIf(r -> r.getRoomId().equals(roomId));
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);
        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (logFile.exists()) logFile.delete();
        return true;
    }

    private String getUserIconById(String userId) {
        return userRepo.findById(userId).map(User::getIcon).filter(Objects::nonNull).orElse("/images/default-avatar.png");
    }

    private String getUserNameById(String userId) {
        return userRepo.findById(userId).map(User::getUserName).orElse("ユーザー");
    }
}