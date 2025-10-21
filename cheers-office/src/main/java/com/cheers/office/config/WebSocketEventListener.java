package com.cheers.office.config; // configパッケージに配置することを想定

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.cheers.office.board.model.CustomUserDetails; 

/**
 * WebSocketの接続/切断イベントをリッスンし、オンラインユーザーの状態を管理するリスナー
 */
@Component
public class WebSocketEventListener {

    // キー: WebSocket Session ID, バリュー: User ID
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate; 

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ユーザーがWebSocketに接続したとき
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Optional.ofNullable(event.getUser())
            .ifPresent(principal -> {
                if (principal instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) principal;
                    String userId = userDetails.getUser().getUserId();
                    
                    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
                    String sessionId = headerAccessor.getSessionId();

                    if (sessionId != null) {
                        onlineUsers.put(sessionId, userId);
                        
                        System.out.println("✅ WebSocket Connected: User " + userId + " (Session: " + sessionId + ")");

                        // ★★★ 修正: 接続イベント発生時、全クライアントにリストを即時ブロードキャスト ★★★
                        broadcastOnlineUsers();
                    }
                }
            });
    }

    // ユーザーがWebSocketから切断したとき
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        if (onlineUsers.containsKey(sessionId)) {
            onlineUsers.remove(sessionId);
            System.out.println("❌ WebSocket Disconnected: Session " + sessionId);

            broadcastOnlineUsers();
        }
    }

    /**
     * 現在オンラインのユニークなユーザーIDリストを /topic/onlineUsers にブロードキャストする。
     */
    private void broadcastOnlineUsers() {
        Set<String> uniqueOnlineUserIds = onlineUsers.values().stream().collect(Collectors.toSet());

        // ★★★ この処理が実行されると、home.htmlのJSがリストを受信し表示を更新する ★★★
        messagingTemplate.convertAndSend("/topic/onlineUsers", uniqueOnlineUserIds);
    }
}