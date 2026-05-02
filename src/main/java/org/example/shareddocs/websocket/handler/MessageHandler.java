package org.example.shareddocs.websocket.handler;

import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket消息处理器接口
 */
public interface MessageHandler {
    
    /**
     * 处理消息
     *
     * @param session WebSocket会话
     * @param message 消息对象
     */
    void handle(WebSocketSession session, WebSocketMessage message);
    
    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    String getSupportedMessageType();
}
