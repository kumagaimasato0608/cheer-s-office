package com.cheers.office.board.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CalendarEvent;
import com.cheers.office.board.repository.CalendarEventRepository;

@RestController
public class CalendarController {

    private final CalendarEventRepository eventRepository;

    public CalendarController(CalendarEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // カレンダーに表示する全てのイベントを取得する
    @GetMapping("/api/events")
    public List<CalendarEvent> getEvents() {
        return eventRepository.findAll();
    }

    // 新しいイベントを保存する
    @PostMapping("/api/events")
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEvent event) {
        CalendarEvent savedEvent = eventRepository.save(event);
        return ResponseEntity.ok(savedEvent);
    }
}