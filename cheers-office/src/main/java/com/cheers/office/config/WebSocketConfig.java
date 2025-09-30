package com.cheers.office.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocketメッセージ処理を有効化
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMPエンドポイントを登録
     * WebSocketクライアントが接続する最初のURL
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // /ws のエンドポイントを登録。SockJSを有効にし、ブラウザの互換性を高める
        registry.addEndpoint("/ws").withSockJS(); 
    }

    /**
     * メッセージブローカーを設定
     * どこにメッセージを送り、どこから受け取るかを定義
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic/ はブローカー宛先 (クライアントが購読し、サーバーがブロードキャストする)
        registry.enableSimpleBroker("/topic"); 
        
        // /app/ はアプリケーション宛先 (クライアントがサーバーの @Controller にメッセージを送る)
        registry.setApplicationDestinationPrefixes("/app");
    }
}