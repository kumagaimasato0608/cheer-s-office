package com.cheers.office.board.repository;

import java.util.List;

import com.cheers.office.board.model.CalendarEvent;

public interface CalendarEventRepository {
    List<CalendarEvent> findAll();
    CalendarEvent save(CalendarEvent event);
}