package com.cheers.office.board.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.cheers.office.board.model.Room;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RoomService {

    @Value("classpath:data/rooms.json")
    private Resource roomsJsonResource;

    private final CopyOnWriteArrayList<Room> rooms = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try (InputStream is = roomsJsonResource.getInputStream()) {
            List<Room> loadedRooms = objectMapper.readValue(is, new TypeReference<List<Room>>() {});
            this.rooms.clear();
            this.rooms.addAll(loadedRooms);
            System.out.println("Loaded " + this.rooms.size() + " rooms from rooms.json");
        } catch (IOException e) {
            System.err.println("Failed to load rooms from rooms.json: " + e.getMessage());
        }
    }

    public List<Room> findAllRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public Room findRoomById(String roomId) {
        return rooms.stream()
                .filter(room -> room.getRoomId().equals(roomId))
                .findFirst()
                .orElse(null);
    }
}