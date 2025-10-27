package com.cheers.office.board.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.ChatMessage;
import com.cheers.office.board.model.ChatRoom;
import com.cheers.office.board.model.ChatRoomDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {

    private final ObjectMapper mapper;

    // ===============================
    // application.properties から設定を取得
    // ===============================
    @Value("${app.room-file-path}")
    private String roomFilePath;

    @Value("${app.chat-log-dir}")
    private String chatLogDir;

    public ChatService(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    // ===============================
    // 画像アップロード共通処理
    // ===============================
    public String saveImage(MultipartFile file, String uploadDir, String urlPrefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = uploadPath.resolve(newFileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return urlPrefix + "/" + newFileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "png";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // ===============================
    // ルームをIDで検索
    // ===============================
    public Optional<ChatRoom> findRoomById(String roomId) {
        try {
            File file = new File(roomFilePath);
            if (file.exists() && file.length() > 0) {
                List<ChatRoom> rooms = mapper.readValue(file, new TypeReference<List<ChatRoom>>() {});
                return rooms.stream()
                        .filter(room -> roomId.equals(room.getRoomId()))
                        .findFirst();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ===============================
    // 未読数付きルーム一覧取得
    // ===============================
    public List<ChatRoomDto> getRoomsWithUnreadCount(String currentUserId) throws IOException {
        File file = new File(roomFilePath);
        if (!file.exists()) return new ArrayList<>();

        List<ChatRoom> rooms = mapper.readValue(file, new TypeReference<>() {});
        return rooms.stream()
            .filter(room -> room.getMembers() != null && room.getMembers().contains(currentUserId))
            .map(room -> {
                ChatRoomDto dto = new ChatRoomDto(room);
                try {
                    dto.setUnreadCount(countUnreadMessages(room.getRoomId(), currentUserId));
                } catch (IOException e) {
                    dto.setUnreadCount(0);
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    // ===============================
    // 未読メッセージ数をカウント
    // ===============================
    private int countUnreadMessages(String roomId, String userId) throws IOException {
        File logFile = new File(chatLogDir + "/room_" + roomId + ".json");
        if (!logFile.exists() || logFile.length() == 0) return 0;

        List<ChatMessage> messages = mapper.readValue(logFile, new TypeReference<>() {});
        return (int) messages.stream()
            .filter(msg ->
                !msg.getUserId().equals(userId) &&
                (msg.getReadBy() == null || !msg.getReadBy().contains(userId))
            )
            .count();
    }
}
