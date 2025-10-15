package com.cheers.office.board.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus; // ResponseEntityのステータスコード用
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; // ★ WebSocket用に追加
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
    private final SimpMessagingTemplate messagingTemplate; // ★ 追加

    // ★ コンストラクタ修正
    public CalendarController(CalendarEventRepository eventRepository, SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // カレンダーに表示する全てのイベントを取得する (★ 修正: 認証チェックは不要、全件返す)
    @GetMapping("/api/events")
    public List<CalendarEvent> getEvents() {
        // FullCalendarは認証情報を渡さないので、一旦全件返す仕様に戻す。
        // フロント側でフィルタリングするか、セキュリティ設定を修正する必要があるが、
        // 今回はJSONベースのシンプル実装を優先。
        return eventRepository.findAll();
    }

    /**
     * 新しいイベントを保存/更新する（作成者IDを自動設定）
     */
    @PostMapping("/api/events")
    public ResponseEntity<CalendarEvent> createEvent(
        @RequestBody CalendarEvent event,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }
        
        // 新規作成時のみ作成者IDを設定
        if (event.getId() == null || event.getId().isEmpty()) {
             event.setCreatedByUserId(userDetails.getUser().getUserId());
        }

        CalendarEvent savedEvent = eventRepository.save(event);
        
        // ★ WebSocket通知: イベントが更新されたことをクライアントに通知
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
        
        // ★ WebSocket通知: 削除されたことをクライアントに通知
        messagingTemplate.convertAndSend("/topic/calendar", event);

        return ResponseEntity.noContent().build();
    }
}