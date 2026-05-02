package org.example.shareddocs.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.UserEventData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.example.shareddocs.websocket.WebSocketSessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

/**
 * 离开文档消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveDocumentHandler implements MessageHandler {
    
    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageSender messageSender;
    private final UserMapper userMapper;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        String documentIdStr = message.getDocumentId();
        
        // 验证文档ID
        if (documentIdStr == null || documentIdStr.isEmpty() || 
            "undefined".equalsIgnoreCase(documentIdStr) || 
            "null".equalsIgnoreCase(documentIdStr)) {
            log.warn("⚠️ 无效的文档ID: {}", documentIdStr);
            messageSender.sendError(session, 400, "无效的文档ID");
            return;
        }
        
        Long documentId;
        try {
            documentId = Long.valueOf(documentIdStr);
        } catch (NumberFormatException e) {
            log.warn("⚠️ 文档ID格式错误: {}", documentIdStr);
            messageSender.sendError(session, 400, "文档ID格式错误");
            return;
        }
        
        log.info("用户 {} 离开文档 {}", userId, documentId);
        
        // 从文档中移除会话
        sessionManager.removeSessionFromDocument(documentId, session.getId());
        
        // 广播用户离开事件
        broadcastUserLeft(documentId, userId);
        
        // 关闭会话
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("关闭WebSocket会话失败：{}", e.getMessage(), e);
        }
    }
    
    @Override
    public String getSupportedMessageType() {
        return "LEAVE_DOCUMENT";
    }
    
    /**
     * 广播用户离开事件
     */
    private void broadcastUserLeft(Long documentId, Long userId) {
        // 从数据库获取用户信息
        User user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : "user" + userId;
        String nickname = user != null ? user.getNickname() : "用户" + userId;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;
        
        UserEventData.UserInfo userInfo = UserEventData.UserInfo.builder()
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .build();
        
        UserEventData eventData = UserEventData.builder()
                .user(userInfo)
                .onlineCount(sessionManager.getDocumentSessionCount(documentId))
                .build();
        
        WebSocketMessage broadcastMessage = messageSender.createMessage(
                MessageType.USER_LEFT, documentId, userId, eventData);
        
        messageSender.broadcastToDocument(documentId, broadcastMessage);
    }
}
