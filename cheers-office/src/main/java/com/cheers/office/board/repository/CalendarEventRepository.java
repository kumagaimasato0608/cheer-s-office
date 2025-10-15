package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.CalendarEvent;

public interface CalendarEventRepository {

    List<CalendarEvent> findAll();
    
    Optional<CalendarEvent> findById(String id);
    
    CalendarEvent save(CalendarEvent event);
    
    void delete(CalendarEvent event);
    
}