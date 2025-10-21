package com.cheers.office.board.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CalendarEvent;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.repository.CalendarEventRepository;

@RestController
public class CalendarController {

    private final CalendarEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CalendarController(CalendarEventRepository eventRepository, SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /** * ① カレンダーイベントを取得: 自身が作成したイベント、または共有されたイベントのみを返す
     */
    @GetMapping("/api/events")
    public List<CalendarEvent> getEvents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }
        
        String currentUserId = userDetails.getUser().getUserId();

        return eventRepository.findAll().stream()
            .filter(event -> 
                currentUserId.equals(event.getCreatedByUserId()) || // 自身が作成者
                (event.getSharedWithUserIds() != null && event.getSharedWithUserIds().contains(currentUserId)) // 共有されている
            )
            .collect(Collectors.toList());
    }

    /**
     * 新しいイベントを保存/更新する（作成者IDと権限チェックを追加）
     */
    @PostMapping("/api/events")
    public ResponseEntity<CalendarEvent> createEvent(
        @RequestBody CalendarEvent event,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }
        
        String currentUserId = userDetails.getUser().getUserId();
        boolean isNew = (event.getId() == null || event.getId().isEmpty());

        if (isNew) {
             // 新規作成時: 作成者IDを設定
             event.setCreatedByUserId(currentUserId);
        } else {
             // ★★★ 更新時: 既存のイベントを取得し、作成者でなければ更新を拒否する ★★★
             Optional<CalendarEvent> existingEventOpt = eventRepository.findById(event.getId());

             if (existingEventOpt.isPresent()) {
                 CalendarEvent existingEvent = existingEventOpt.get();

                 // 認可チェック: 作成者でない、かつ管理権限もない場合は更新不可
                 if (!currentUserId.equals(existingEvent.getCreatedByUserId()) && !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                      // 編集権限がない場合はFORBIDDENを返す
                     return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); 
                 }
                 
                 // 作成者IDは既存のものを維持（誤って上書きされないように）
                 event.setCreatedByUserId(existingEvent.getCreatedByUserId());
             } else {
                 // IDがあるが既存データが見つからない場合も、処理を続行せずエラー
                 return ResponseEntity.notFound().build();
             }
        }

        // データを保存（IDがあれば更新、なければ新規作成）
        CalendarEvent savedEvent = eventRepository.save(event);
        
        // WebSocket通知: イベントが更新されたことをクライアントに通知
        messagingTemplate.convertAndSend("/topic/calendar", savedEvent);

        return ResponseEntity.ok(savedEvent);
    }
    
    /**
     * イベントを削除する（自分のイベントのみ削除可能）
     */
    @DeleteMapping("/api/events/{id}")
    public ResponseEntity<Void> deleteEvent(
        @PathVariable String id, 
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        Optional<CalendarEvent> eventOpt = eventRepository.findById(id);

        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }

        CalendarEvent event = eventOpt.get();
        String currentUserId = userDetails.getUser().getUserId();

        // 認可チェック: 作成者でなければ削除不可
        if (!currentUserId.equals(event.getCreatedByUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); 
        }

        eventRepository.delete(event);
        
        // WebSocket通知: 削除されたことをクライアントに通知
        messagingTemplate.convertAndSend("/topic/calendar", event);

        return ResponseEntity.noContent().build();
    }
}