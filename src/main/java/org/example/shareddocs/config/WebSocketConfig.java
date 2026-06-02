package org.example.shareddocs.config;

import lombok.RequiredArgsConstructor;
import org.example.shareddocs.websocket.MessageDispatcher;
import org.example.shareddocs.websocket.interceptor.AuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final WebSocketHandler webSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final MessageDispatcher messageDispatcher;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 支持两种路径格式：
        // 1. /ws - 基础路径
        // 2. /ws/{docId} - 带文档ID的路径
        registry.addHandler(webSocketHandler, "/ws")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
        
        registry.addHandler(webSocketHandler, "/ws/{docId}")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
