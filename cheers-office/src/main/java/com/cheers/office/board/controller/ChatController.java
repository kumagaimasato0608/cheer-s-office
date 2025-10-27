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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.cheers.office.board.model.ChatRoomDto;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.board.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ChatController {

    // ===============================
    // ファイルパス設定（application.properties から取得）
    // ===============================
    @Value("${app.room-file-path}")
    private String roomFilePath;

    @Value("${app.chat-log-dir}")
    private String chatLogDir;

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

    // ===============================
    // メッセージ送信
    // ===============================
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

        File logFile = new File(chatLogDir + "/room_" + roomId + ".json");
        synchronized (this) {
            List<ChatMessage> logs = logFile.exists() && logFile.length() > 0
                    ? mapper.readValue(logFile, new TypeReference<>() {})
                    : new ArrayList<>();
            logs.add(msg);
            logFile.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
        }

        simpMessagingTemplate.convertAndSend("/topic/chat/" + roomId, msg);

        Optional<ChatRoom> roomOpt = chatService.findRoomById(roomId);
        roomOpt.ifPresent(room -> {
            for (String memberId : room.getMembers()) {
                if (!memberId.equals(msg.getUserId())) {
                    simpMessagingTemplate.convertAndSend(
                            "/topic/notifications/" + memberId,
                            Map.of("type", "NEW_MESSAGE", "roomId", roomId)
                    );
                }
            }
        });
    }

    // ===============================
    // 既読更新（単一メッセージ）
    // ===============================
    @MessageMapping("/chat/{roomId}/read")
    public void markAsRead(@DestinationVariable String roomId, @Payload Map<String, String> payload) throws IOException {
        String userId = payload.get("userId");
        String messageId = payload.get("messageId");
        if (userId == null || messageId == null) return;

        File logFile = new File(chatLogDir + "/room_" + roomId + ".json");
        if (!logFile.exists()) return;

        synchronized (this) {
            List<ChatMessage> logs = mapper.readValue(logFile, new TypeReference<>() {});
            for (ChatMessage msg : logs) {
                if (messageId.equals(msg.getMessageId())) {
                    if (msg.getReadBy() == null) msg.setReadBy(new ArrayList<>());
                    if (!msg.getReadBy().contains(userId)) {
                        msg.getReadBy().add(userId);
                        mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
                    }
                    simpMessagingTemplate.convertAndSend("/topic/chat/" + roomId,
                            Map.of("type", "READ_UPDATE", "messageId", msg.getMessageId(), "readBy", msg.getReadBy()));
                    break;
                }
            }
        }
    }

    // ===============================
    // 既読更新（全メッセージ）
    // ===============================
    @MessageMapping("/chat/{roomId}/markAllAsRead")
    public void markAllMessagesAsRead(@DestinationVariable String roomId, @Payload Map<String, String> payload) throws IOException {
        String userId = payload.get("userId");
        if (userId == null) return;

        File logFile = new File(chatLogDir + "/room_" + roomId + ".json");
        if (!logFile.exists() || logFile.length() == 0) return;

        synchronized (this) {
            List<ChatMessage> logs = mapper.readValue(logFile, new TypeReference<>() {});
            boolean updated = false;

            for (ChatMessage msg : logs) {
                if (!userId.equals(msg.getUserId())) {
                    if (msg.getReadBy() == null) msg.setReadBy(new ArrayList<>());
                    if (!msg.getReadBy().contains(userId)) {
                        msg.getReadBy().add(userId);
                        updated = true;
                    }
                }
            }

            if (updated) mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
        }
    }

    // ===============================
    // チャット画像アップロード
    // ===============================
    @PostMapping("/chat/upload")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "ファイルが空です"));
        try {
            String imageUrl = chatService.saveImage(file, chatUploadDir, "/images/chat");
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "アップロードエラー"));
        }
    }

    // ===============================
    // グループアイコンアップロード
    // ===============================
    @PostMapping("/rooms/uploadIcon")
    public ResponseEntity<Map<String, String>> uploadGroupIcon(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "ファイルが空です"));
        try {
            String iconUrl = chatService.saveImage(file, groupUploadDir, "/images/groups");
            return ResponseEntity.ok(Map.of("iconUrl", iconUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "アップロードエラー"));
        }
    }

    // ===============================
    // ルーム作成
    // ===============================
    @PostMapping("/rooms")
    public ChatRoom createRoom(@RequestBody ChatRoom requestData) throws IOException {
        synchronized (this) {
            File file = new File(roomFilePath);
            List<ChatRoom> rooms = file.exists()
                    ? mapper.readValue(file, new TypeReference<>() {})
                    : new ArrayList<>();

            if (requestData.getMembers() != null && requestData.getMembers().size() == 2) {
                for (ChatRoom r : rooms) {
                    if (r.getMembers() != null && r.getMembers().size() == 2 &&
                        r.getMembers().containsAll(requestData.getMembers())) return r;
                }
                ChatRoom newSingleRoom = new ChatRoom();
                newSingleRoom.setRoomId("r_" + UUID.randomUUID().toString().substring(0, 8));
                newSingleRoom.setCreatedAt(LocalDateTime.now().toString());
                newSingleRoom.setMembers(requestData.getMembers());
                String myUserId = requestData.getMembers().get(0);
                String otherId = requestData.getMembers().stream()
                        .filter(id -> !id.equals(myUserId)).findFirst().orElse("");
                newSingleRoom.setRoomName(getUserNameById(otherId));
                rooms.add(newSingleRoom);
                file.getParentFile().mkdirs();
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
                file.getParentFile().mkdirs();
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);
                return newGroupRoom;
            }
        }
        throw new IOException("Invalid room creation request");
    }

    // ===============================
    // ルーム一覧取得
    // ===============================
    @GetMapping("/rooms")
    public List<ChatRoomDto> getRooms(@AuthenticationPrincipal CustomUserDetails loginUser) throws IOException {
        if (loginUser == null) return Collections.emptyList();
        User currentUser = loginUser.getUser();
        if (currentUser == null || currentUser.getUserId() == null) return Collections.emptyList();
        return chatService.getRoomsWithUnreadCount(currentUser.getUserId());
    }

    // ===============================
    // チャットログ取得
    // ===============================
    @GetMapping("/chat/{roomId}")
    public List<ChatMessage> getChatLogs(@PathVariable String roomId) throws IOException {
        File file = new File(chatLogDir + "/room_" + roomId + ".json");
        if (!file.exists()) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    // ===============================
    // ルーム削除
    // ===============================
    @DeleteMapping("/rooms/{roomId}")
    public boolean deleteRoom(@PathVariable String roomId) throws IOException {
        synchronized (this) {
            File file = new File(roomFilePath);
            if (!file.exists()) return false;
            List<ChatRoom> rooms = mapper.readValue(file, new TypeReference<>() {});
            rooms.removeIf(r -> r.getRoomId().equals(roomId));
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms);
            File logFile = new File(chatLogDir + "/room_" + roomId + ".json");
            if (logFile.exists()) logFile.delete();
            return true;
        }
    }

    // ===============================
    // ユーザー情報補助
    // ===============================
    private String getUserIconById(String userId) {
        return userRepo.findById(userId)
                .map(User::getIcon)
                .filter(Objects::nonNull)
                .orElse("/images/default_icon.png");
    }

    private String getUserNameById(String userId) {
        return userRepo.findById(userId)
                .map(User::getUserName)
                .orElse("ユーザー");
    }
}
