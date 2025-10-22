package com.cheers.office.config;

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
// ★★★ UserServiceをインポートする（全ユーザーを取得するため） ★★★
// import com.cheers.office.user.service.UserService; 

/**
 * WebSocketの接続/切断イベントをリッスンし、オンラインユーザーの状態を管理するリスナー
 */
@Component
public class WebSocketEventListener {

    // キー: WebSocket Session ID, バリュー: User ID
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate; 
    
    // ★★★ 依存性の注入: 全ユーザー情報を取得するためのサービス (仮定) ★★★
    // private final UserService userService; 

    // コンストラクタを修正 (UserServiceの注入が必要な場合は有効にする)
    // public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, UserService userService) {
    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        // this.userService = userService; 
    }

    // ユーザーがWebSocketに接続したとき (ログイン後、/wsに接続した時)
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

                        // 接続イベント発生時、全クライアントにリストを即時ブロードキャスト
                        broadcastOnlineUsers();
                    }
                }
            });
    }

    // ユーザーがWebSocketから切断したとき (ログアウト、ブラウザ/アプリを閉じた、ネットワーク切断)
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        if (onlineUsers.containsKey(sessionId)) {
            String userId = onlineUsers.remove(sessionId);
            System.out.println("❌ WebSocket Disconnected: User " + userId + " (Session: " + sessionId + ")");

            broadcastOnlineUsers();
        }
    }

    /**
     * 現在オンラインのユニークなユーザーの詳細情報リストを /topic/onlineUsers にブロードキャストする。
     */
    private void broadcastOnlineUsers() {
        Set<String> uniqueOnlineUserIds = onlineUsers.values().stream().collect(Collectors.toSet());
        
        // ★★★ 修正ポイント ★★★
        // 1. 全ユーザー情報を取得する (ここでは global variable の allUsers を使う)
        //    本来は Spring Data Repository/Service から取得する必要がありますが、
        //    ここではクライアント側のJSで使われている「すべてのユーザー情報」をサーバー側でも使えると仮定します。
        
        // ★★★ サーバー側でのダミーリスト生成 (実際に動作させるにはAPIから取得が必要) ★★★
        // List<User> allUsersFromService = userService.findAllUsers(); 
        
        // 2. オンラインのユーザーIDと照合して、詳細なUserオブジェクトのリストを作成
        //    ここでは、UserオブジェクトのJSON表現がブロードキャストされます。
        //    以下は、Userオブジェクトに適切なgetterがあることを前提とした例です。
        
        // ★★★ 以下の実装では、クライアント側のJSで使われている allUsers がサーバー側でも使える必要があります。
        //     もし使えない場合、このメソッドを修正するか、クライアント側で処理を完結させる必要があります。
        
        // 現状、クライアントJSがフィルタリングしているため、ここではIDリストのみブロードキャストを維持します。
        // クライアント側でフィルタリングすることで、サーバー側の依存関係を減らせます。
        // -> クライアント側で処理を継続するため、ここでは変更を最小限に留めます。
        
        // ★★★ 変更なし: IDリストをブロードキャストするロジックを維持 ★★★
        messagingTemplate.convertAndSend("/topic/onlineUsers", uniqueOnlineUserIds);
    }
}