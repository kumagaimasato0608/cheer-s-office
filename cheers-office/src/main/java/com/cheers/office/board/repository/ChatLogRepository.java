package com.cheers.office.board.repository;
import java.util.List;

import com.cheers.office.board.model.Log;
// @Repository は付けません！
public interface ChatLogRepository {
    Log save(Log log);
    List<Log> findByRoomId(String roomId);
    List<Log> findLatestByRoomId(String roomId, int limit);
}