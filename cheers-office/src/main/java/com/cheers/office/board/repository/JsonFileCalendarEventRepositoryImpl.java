package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.CalendarEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFileCalendarEventRepositoryImpl implements CalendarEventRepository {

    private static final String DATA_DIR = "src/main/resources/data/";
    private static final String FILE_PATH = DATA_DIR + "calendar_events.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonFileCalendarEventRepositoryImpl() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                // 初期ファイル作成 (空リスト)
                mapper.writeValue(file, new ArrayList<CalendarEvent>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<CalendarEvent> readAll() {
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<List<CalendarEvent>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void writeAll(List<CalendarEvent> events) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), events);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CalendarEvent> findAll() {
        return readAll();
    }

    @Override
    public Optional<CalendarEvent> findById(String id) {
        return readAll().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    @Override
    public CalendarEvent save(CalendarEvent event) {
        List<CalendarEvent> events = readAll();
        
        if (event.getId() == null || event.getId().isEmpty()) {
            // 新規作成
            event.setId(UUID.randomUUID().toString());
            events.add(event);
        } else {
            // 更新
            events = events.stream()
                    .map(e -> e.getId().equals(event.getId()) ? event : e)
                    .collect(Collectors.toList());
        }

        writeAll(events);
        return event;
    }

    @Override
    public void delete(CalendarEvent event) {
        List<CalendarEvent> events = readAll();
        events.removeIf(e -> e.getId().equals(event.getId()));
        writeAll(events);
    }
}