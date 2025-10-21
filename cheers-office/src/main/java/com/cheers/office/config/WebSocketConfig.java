package com.cheers.office.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketおよびSTOMPメッセージブローカーの設定
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // クライアントが接続するエンドポイント（SockJSを使用）
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // wsエンドポイントを追加し、SockJSを有効にする
        registry.addEndpoint("/ws").withSockJS();
    }

    // メッセージブローカーの設定
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic がブロードキャスト用プレフィックス
        registry.enableSimpleBroker("/topic");
        // /app が @MessageMapping アノテーションを持つサーバー側エンドポイント用プレフィックス
        registry.setApplicationDestinationPrefixes("/app");
    }
}