package org.example.shareddocs.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 */
@Slf4j
@Component
public class DocumentWebSocketHandler extends AbstractWebSocketHandler {
    
    private final WebSocketSessionManager sessionManager;
    private final MessageDispatcher messageDispatcher;
    private final WebSocketMessageSender messageSender;
    
    public DocumentWebSocketHandler(WebSocketSessionManager sessionManager, 
                                   MessageDispatcher messageDispatcher,
                                   WebSocketMessageSender messageSender) {
        this.sessionManager = sessionManager;
        this.messageDispatcher = messageDispatcher;
        this.messageSender = messageSender;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: sessionId={}, attributes={}",
                session.getId(), session.getAttributes());
        
        // 从session attributes中获取用户ID
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.addSession(session, userId);
            log.info("会话已关联用户: userId={}", userId);
        } else {
            sessionManager.addSession(session);
            log.warn("会话未关联用户");
        }
        
        // 初始化消息分发器（只执行一次）
        messageDispatcher.init();
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("收到文本消息: {}", payload);
            
            // 解析消息
            WebSocketMessage wsMessage = messageDispatcher.parseMessage(payload);
            
            // 分发消息
            messageDispatcher.dispatch(session, wsMessage);
            
        } catch (Exception e) {
            log.error("处理文本消息失败: {}", e.getMessage(), e);
            messageSender.sendError(session, 500, "消息处理失败: " + e.getMessage());
        }
    }
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        log.warn("收到二进制消息，当前不支持。请前端改用 JSON 文本格式发送消息。");
        messageSender.sendError(session, 400, "不支持二进制消息，请使用 JSON 文本格式");
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
        sessionManager.removeSession(session);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 忽略应用关闭时的连接中断异常，这属于正常现象
        if (exception instanceof IOException && exception.getCause() instanceof ClosedChannelException) {
            log.debug("WebSocket连接已关闭（应用关闭时正常现象）: {}", session.getId());
            return;
        }
        
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        sessionManager.removeSession(session);
    }
}
