package com.cheers.office.config;

import java.security.Principal;
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

// 必要なインポート
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

/**
 * WebSocketの接続/切断イベントをリッスンし、オンラインユーザーの状態を管理するリスナー
 * (修正版：principal.getName() (Email) から UserRepository を使って UserId を取得する)
 */
@Component
public class WebSocketEventListener {

    // キー: WebSocket Session ID, バリュー: User ID
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    // UserRepository を注入
    private final UserRepository userRepository;

    /**
     * コンストラクタ (UserRepositoryをDI)
     */
    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /**
     * ユーザーがWebSocketに接続したとき
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        
        Principal principal = event.getUser();
        System.out.println("★ WebSocket Connection Attempt. Principal: " + principal);

        // principal (認証情報) が存在し、名前 (メールアドレス) があるか確認
        if (principal != null && principal.getName() != null) {
            
            String email = principal.getName(); // ログイン時のメールアドレスを取得
            System.out.println("Principal Name (Email) found: " + email);

            try {
                // メールアドレスでDB (JSONファイル) からユーザー情報を検索
                Optional<User> userOpt = userRepository.findByMailAddress(email); 

                if (userOpt.isPresent()) {
                    // ユーザーが見つかった場合、本当の UserID を取得
                    String userId = userOpt.get().getUserId(); 
                    
                    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
                    String sessionId = headerAccessor.getSessionId();

                    if (sessionId != null) {
                        // オンラインリストにセッションIDとUserIDを紐づけて追加
                        onlineUsers.put(sessionId, userId);
                        System.out.println("✅ WebSocket SUCCESS: User " + userId + " added (Session: " + sessionId + ")");
                        // 全クライアントに最新のオンラインリストを送信
                        broadcastOnlineUsers();
                    }
                } else {
                     System.out.println("⚠️ WebSocket WARNING: User not found in DB for email: " + email);
                }
            } catch (Exception e) {
                System.out.println("❌ WebSocket ERROR: Failed to find user by email: " + email);
                e.printStackTrace();
            }

        } else {
             System.out.println("⚠️ WebSocket WARNING: Principal was null or had no name. (Connection ignored)");
        }
    }

    /**
     * ユーザーがWebSocketから切断したとき
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        // このセッションIDがオンラインリストにあれば削除
        if (onlineUsers.containsKey(sessionId)) {
            String userId = onlineUsers.remove(sessionId);
            System.out.println("❌ WebSocket Disconnected: User " + userId + " removed (Session: " + sessionId + ")");
            
            // 全クライアントに最新のオンラインリストを送信
            broadcastOnlineUsers();
        }
    }

    /**
     * 現在オンラインのユニークなユーザーIDリストを /topic/onlineUsers にブロードキャストする。
     */
    private void broadcastOnlineUsers() {
        // 複数のブラウザで接続してもIDが重複しないようSetに変換
        Set<String> uniqueOnlineUserIds = onlineUsers.values().stream().collect(Collectors.toSet());
        
        System.out.println("Broadcasting Online Users (Count: " + uniqueOnlineUserIds.size() + "): " + uniqueOnlineUserIds);

        // home.htmlはこのIDリストを待っている
        messagingTemplate.convertAndSend("/topic/onlineUsers", uniqueOnlineUserIds);
    }
}
