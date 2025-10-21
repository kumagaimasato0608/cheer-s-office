package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.CalendarEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFileCalendarEventRepositoryImpl implements CalendarEventRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File eventFile;

    // ✅ application.propertiesからファイルパスを読み取る
    public JsonFileCalendarEventRepositoryImpl(
        @Value("${app.event-file-path:src/main/resources/data/calendar_events.json}") String eventFilePath
    ) {
        this.eventFile = new File(eventFilePath);
        // ディレクトリがなければ作成
        if (!eventFile.getParentFile().exists()) {
            eventFile.getParentFile().mkdirs();
        }
        // ファイルがなければ空リストで初期化
        if (!eventFile.exists()) {
            try {
                mapper.writeValue(eventFile, new ArrayList<CalendarEvent>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<CalendarEvent> readAll() {
        try {
            if (!eventFile.exists() || eventFile.length() == 0) return new ArrayList<>();
            return mapper.readValue(eventFile, new TypeReference<List<CalendarEvent>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void writeAll(List<CalendarEvent> events) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(eventFile, events);
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
            event.setId(UUID.randomUUID().toString());
            events.add(event);
        } else {
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
