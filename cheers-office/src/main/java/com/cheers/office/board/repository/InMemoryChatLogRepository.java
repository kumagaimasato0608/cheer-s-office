package com.cheers.office.board.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.Log; // ★この行を追加する

@Repository
public class InMemoryChatLogRepository implements ChatLogRepository {

    private final Map<String, CopyOnWriteArrayList<Log>> roomLogs = new ConcurrentHashMap<>();

    @Override
    public Log save(Log log) {
        if (log.getId() == null) {
            log.setId(UUID.randomUUID().toString());
        }
        if (log.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now());
        }

        roomLogs.computeIfAbsent(log.getRoomId(), k -> new CopyOnWriteArrayList<>()).add(log);
        return log;
    }

    @Override
    public List<Log> findByRoomId(String roomId) {
        return Collections.unmodifiableList(roomLogs.getOrDefault(roomId, new CopyOnWriteArrayList<>()));
    }

    @Override
    public List<Log> findLatestByRoomId(String roomId, int limit) {
        List<Log> logs = findByRoomId(roomId);
        if (logs.size() <= limit) {
            return logs;
        }
        return logs.subList(logs.size() - limit, logs.size());
    }
}