package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.CalendarEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFileCalendarEventRepositoryImpl implements CalendarEventRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File eventFile;

    // ★★★ @Value の設定名を修正しました ★★★
    public JsonFileCalendarEventRepositoryImpl(
        // Javaが探す名前を、propertiesファイルに合わせて "app.calendar-file-path" に変更
        // 安全でないデフォルト値 (:src/...) を削除
        @Value("${app.calendar-file-path}") String eventFilePath
    ) {
        // ★★★ 修正はここまで ★★★

        this.eventFile = new File(eventFilePath);
        // ディレクトリがなければ作成
        File parentDir = eventFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        // ファイルがなければ空リストで初期化
        if (!eventFile.exists()) {
            try {
                // Ensure the parent directory exists before writing
                if (parentDir != null && !parentDir.exists()) {
                   parentDir.mkdirs();
                }
                mapper.writeValue(eventFile, new ArrayList<CalendarEvent>());
            } catch (IOException e) {
                // Consider more robust error handling or logging
                System.err.println("Failed to create initial calendar event file: " + eventFilePath);
                e.printStackTrace();
            }
        }
    }

    private List<CalendarEvent> readAll() {
        try {
            if (!eventFile.exists() || eventFile.length() == 0) {
                 return new ArrayList<>(); // Return empty list if file doesn't exist or is empty
            }
            return mapper.readValue(eventFile, new TypeReference<List<CalendarEvent>>() {});
        } catch (IOException e) {
            System.err.println("Error reading calendar event file: " + eventFile.getAbsolutePath());
            e.printStackTrace();
            // Return empty list on error to prevent application crash, but log the error
            return new ArrayList<>();
        }
    }

    private void writeAll(List<CalendarEvent> events) {
        try {
             // Ensure parent directory exists before writing
            File parentDir = eventFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(eventFile, events);
        } catch (IOException e) {
            System.err.println("Error writing calendar event file: " + eventFile.getAbsolutePath());
            e.printStackTrace();
             // Consider throwing a custom exception or handling the error more gracefully
        }
    }

    @Override
    public List<CalendarEvent> findAll() {
        return readAll();
    }

    @Override
    public Optional<CalendarEvent> findById(String id) {
        // Added null check for id
        if (id == null) {
            return Optional.empty();
        }
        return readAll().stream()
                .filter(e -> id.equals(e.getId())) // Use equals safely
                .findFirst();
    }

    @Override
    public CalendarEvent save(CalendarEvent event) {
        if (event == null) {
             throw new IllegalArgumentException("Cannot save null event");
        }
        List<CalendarEvent> events = readAll();
        boolean found = false; // Flag to check if event was updated

        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(UUID.randomUUID().toString());
            events.add(event); // Add new event
            found = true; // Mark as added (implicitly found/handled)
        } else {
            // Use loop for explicit update check and replacement
            List<CalendarEvent> updatedEvents = new ArrayList<>();
            for (CalendarEvent existingEvent : events) {
                if (event.getId().equals(existingEvent.getId())) {
                    updatedEvents.add(event); // Replace with the new event data
                    found = true;
                } else {
                    updatedEvents.add(existingEvent); // Keep the existing event
                }
            }
            // If the event ID was provided but not found in the list, add it as a new event
            if (!found) {
                 updatedEvents.add(event);
            }
            events = updatedEvents; // Assign the potentially modified list back
        }

        writeAll(events);
        return event;
    }

    @Override
    public void delete(CalendarEvent event) {
        if (event == null || event.getId() == null) {
            return; // Cannot delete null event or event without ID
        }
        List<CalendarEvent> events = readAll();
        boolean removed = events.removeIf(e -> event.getId().equals(e.getId())); // Use equals safely
        if (removed) {
            writeAll(events); // Only write if something was actually removed
        }
    }
}