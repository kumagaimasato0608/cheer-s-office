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
    private static final String DATA_DIR = "src/main/resources/data/";
    private static final String ROOM_FILE = DATA_DIR + "rooms.json";
    private static final String CHAT_DIR = DATA_DIR + "chat_logs/";

    public ChatService(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    public String saveImage(MultipartFile file, String uploadDir, String urlPrefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return urlPrefix + "/" + newFileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "png";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public Optional<ChatRoom> findRoomById(String roomId) {
        try {
            File file = new File(ROOM_FILE);
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
    
    /**
     * 未読メッセージ数を含むチャットルームのリストを取得します。
     * @param currentUserId 現在ログインしているユーザーのID
     * @return 未読数付きのルームDTOリスト
     */
    public List<ChatRoomDto> getRoomsWithUnreadCount(String currentUserId) throws IOException {
        File file = new File(ROOM_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<ChatRoom> rooms = mapper.readValue(file, new TypeReference<>() {});

        return rooms.stream()
        	.filter(room -> room.getMembers() != null && room.getMembers().contains(currentUserId))	
            .map(room -> {
                ChatRoomDto dto = new ChatRoomDto(room);
                try {
                    int unreadCount = countUnreadMessages(room.getRoomId(), currentUserId);
                    dto.setUnreadCount(unreadCount);
                } catch (IOException e) {
                    dto.setUnreadCount(0);
                    e.printStackTrace();
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * 指定されたルームの未読メッセージ数をカウントします。
     * @param roomId ルームID
     * @param userId ユーザーID
     * @return 未読メッセージ数
     */
    private int countUnreadMessages(String roomId, String userId) throws IOException {
        File logFile = new File(CHAT_DIR + "room_" + roomId + ".json");
        if (!logFile.exists() || logFile.length() == 0) {
            return 0;
        }

        List<ChatMessage> messages = mapper.readValue(logFile, new TypeReference<>() {});
        
        long count = messages.stream()
            .filter(msg -> 
                !msg.getUserId().equals(userId) && 
                (msg.getReadBy() == null || !msg.getReadBy().contains(userId))
            )
            .count();
            
        return (int) count;
    }
}