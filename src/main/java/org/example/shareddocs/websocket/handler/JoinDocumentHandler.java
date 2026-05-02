package org.example.shareddocs.websocket.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.JoinDocumentData;
import org.example.shareddocs.dto.websocket.UserEventData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.example.shareddocs.websocket.WebSocketSessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

/**
 * 加入文档消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinDocumentHandler implements MessageHandler {
    
    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageSender messageSender;
    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = parseDocumentId(message.getDocumentId());
        if (documentId == null) {
            log.error("❌ 无效的文档ID: {}", message.getDocumentId());
            messageSender.sendError(session, 400, "无效的文档ID");
            return;
        }
        
        JoinDocumentData data = (JoinDocumentData) message.getData();
        
        log.info("🔥 用户 {} 请求加入文档 {}", userId, documentId);
        
        // 将用户会话添加到文档
        sessionManager.addSessionToDocument(documentId, session.getId(), userId);
        
        // 存储用户信息到session
        session.getAttributes().put("documentId", documentId);
        session.getAttributes().put("userId", userId);
        if (data != null && data.getCursorPosition() != null) {
            session.getAttributes().put("cursorPosition", data.getCursorPosition());
        }
        
        // 广播用户加入事件
        broadcastUserJoined(documentId, userId);
        
        // 发送加入成功响应
        WebSocketMessage response = messageSender.createMessage(
                MessageType.JOIN_DOCUMENT, documentId, userId, null);
        
        messageSender.sendMessage(session, response);
        log.info("✅ 用户 {} 成功加入文档 {}", userId, documentId);
    }
    
    @Override
    public String getSupportedMessageType() {
        return "JOIN_DOCUMENT";
    }
    
    /**
     * 解析文档ID（支持Long和UUID字符串）
     */
    private Long parseDocumentId(String docId) {
        // 验证无效值
        if (docId == null || docId.trim().isEmpty() || 
            "undefined".equalsIgnoreCase(docId) || 
            "null".equalsIgnoreCase(docId)) {
            log.warn("⚠️ 无效的文档ID: {}", docId);
            return null;
        }
        
        try {
            // 尝试解析为Long
            return Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 如果是UUID格式，查询数据库获取Long ID
            log.debug("文档ID是UUID格式，尝试查询: {}", docId);
            LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Document::getDocUuid, docId)
                   .eq(Document::getIsDeleted, 0);
            Document document = documentMapper.selectOne(wrapper);
            
            if (document != null) {
                log.debug("通过UUID找到文档ID: uuid={}, id={}", docId, document.getId());
                return document.getId();
            } else {
                log.warn("未找到UUID对应的文档: {}", docId);
                return null;
            }
        }
    }
    
    /**
     * 广播用户加入事件
     */
    private void broadcastUserJoined(Long documentId, Long userId) {
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
                MessageType.USER_JOINED, documentId, userId, eventData);
        
        messageSender.broadcastToDocument(documentId, broadcastMessage);
    }
}
