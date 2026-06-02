package org.example.shareddocs.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.ErrorData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket消息发送服务
 * 负责消息的序列化和发送，避免循环依赖
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageSender {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSessionManager sessionManager;
    
    /**
     * 发送消息给指定会话
     */
    public void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session != null && session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                // 同步发送消息，避免并发写入导致 WebSocket 状态异常
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 广播消息给文档的所有用户
     */
    public void broadcastToDocument(Long documentId, WebSocketMessage message) {
        var sessions = sessionManager.getDocumentSessions(documentId);
        log.info("开始广播消息到文档 {}, 会话数: {}", documentId, sessions.size());
        
        sessions.forEach(sessionId -> {
            WebSocketSession session = sessionManager.getSession(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
                log.debug("消息已发送给会话 {}", sessionId);
            } else {
                log.warn("会话 {} 不存在或已关闭", sessionId);
            }
        });
        
        log.info("广播完成，共 {} 个会话", sessions.size());
    }
    
    /**
     * 发送错误消息
     */
    public void sendError(WebSocketSession session, Integer code, String message) {
        ErrorData errorData = ErrorData.builder()
                .code(code)
                .message(message)
                .build();
        
        WebSocketMessage errorMessage = WebSocketMessage.builder()
                .type(MessageType.ERROR)
                .messageId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toEpochMilli())
                .data(errorData)
                .build();
        
        sendMessage(session, errorMessage);
    }
    
    /**
     * 创建消息对象
     */
    public WebSocketMessage createMessage(MessageType type, Long documentId, 
                                         Long userId, Object data) {
        return WebSocketMessage.builder()
                .type(type)
                .messageId(UUID.randomUUID().toString())
                .documentId(String.valueOf(documentId))
                .userId(userId)
                .timestamp(Instant.now().toEpochMilli())
                .data(data)
                .build();
    }
}
