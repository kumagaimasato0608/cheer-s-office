package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.CalendarEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFileCalendarEventRepositoryImpl implements CalendarEventRepository {

    private final ObjectMapper objectMapper;
    private final File eventFile;
    private final CopyOnWriteArrayList<CalendarEvent> events;

    public JsonFileCalendarEventRepositoryImpl(ObjectMapper objectMapper, @Value("${app.event-file-path:src/main/resources/data/events.json}") String eventFilePath) {
        this.objectMapper = objectMapper;
        this.eventFile = new File(eventFilePath);
        this.events = new CopyOnWriteArrayList<>();
        loadEvents();
    }

    private void loadEvents() {
        if (eventFile.exists() && eventFile.length() > 0) {
            try {
                this.events.addAll(objectMapper.readValue(eventFile, new TypeReference<List<CalendarEvent>>() {}));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void saveEventsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(eventFile, events);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public List<CalendarEvent> findAll() {
        return events;
    }

    @Override
    public CalendarEvent save(CalendarEvent event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(UUID.randomUUID().toString());
        }
        // 既存のイベントがあれば更新、なければ追加（今回は簡単のため追加のみ）
        events.add(event);
        saveEventsToFile();
        return event;
    }
}