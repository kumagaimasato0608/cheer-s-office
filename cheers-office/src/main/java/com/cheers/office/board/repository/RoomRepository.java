package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.Room;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class RoomRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ROOMS_FILE_PATH = "src/main/resources/data/rooms.json";
    private final File roomsFile = new File(ROOMS_FILE_PATH);

    // ★★★ 排他制御用オブジェクト ★★★
    private final Object fileLock = new Object(); 

    public List<Room> findAll() {
        synchronized (fileLock) { 
            try {
                if (!roomsFile.exists() || roomsFile.length() == 0) {
                    return new ArrayList<>();
                }
                return objectMapper.readValue(roomsFile, new TypeReference<List<Room>>() {});
            } catch (IOException e) {
                System.err.println("ルームデータの読み込み中にエラーが発生しました: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    public void saveAll(List<Room> rooms) {
        synchronized (fileLock) { 
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(roomsFile, rooms);
            } catch (IOException e) {
                System.err.println("ルームデータの書き込み中にエラーが発生しました: " + e.getMessage());
            }
        }
    }

    public Optional<Room> findById(String roomId) {
        return findAll().stream()
                .filter(r -> r.getRoomId().equals(roomId))
                .findFirst();
    }
    
    /**
     * ルームを新規作成する
     */
    public Room save(Room room) {
        List<Room> rooms = findAll();
        
        if (room.getRoomId() == null || room.getRoomId().isEmpty()) {
            room.setRoomId(UUID.randomUUID().toString().substring(0, 6)); // 短いIDを生成
        }
        
        rooms.add(room);
        saveAll(rooms);
        return room;
    }
}