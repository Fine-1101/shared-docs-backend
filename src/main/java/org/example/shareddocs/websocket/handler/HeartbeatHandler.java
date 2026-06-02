package org.example.shareddocs.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.HeartbeatAckData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

/**
 * 心跳消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final DocumentIdResolver documentIdResolver;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = documentIdResolver.resolve(message.getDocumentId());
        if (documentId == null) {
            log.warn("心跳消息中无效的文档ID: {}", message.getDocumentId());
            return;
        }
        
        log.debug("收到用户 {} 的心跳，文档: {}", userId, documentId);
        
        // 更新最后活跃时间
        session.getAttributes().put("lastActiveTime", Instant.now().toEpochMilli());
        
        // 发送心跳确认
        HeartbeatAckData ackData = HeartbeatAckData.builder()
                .serverTime(Instant.now().toEpochMilli())
                .build();
        
        WebSocketMessage ackMessage = messageSender.createMessage(
                MessageType.HEARTBEAT_ACK, documentId, null, ackData);
        
        messageSender.sendMessage(session, ackMessage);
    }
    
    @Override
    public String getSupportedMessageType() {
        return "HEARTBEAT";
    }
}
