package org.example.shareddocs.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.CursorBroadcastData;
import org.example.shareddocs.dto.websocket.CursorMoveData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

/**
 * 光标移动消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CursorMoveHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final UserMapper userMapper;
    private final DocumentIdResolver documentIdResolver;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = documentIdResolver.resolve(message.getDocumentId());
        if (documentId == null) {
            log.warn("光标移动消息中无效的文档ID: {}", message.getDocumentId());
            return;
        }
        
        CursorMoveData cursorData = (CursorMoveData) message.getData();
        
        log.debug("收到用户 {} 的光标移动：{}", userId, cursorData);
        
        // 更新session中的光标位置
        session.getAttributes().put("cursorPosition", cursorData.getPosition());
        if (cursorData.getSelectionStart() != null) {
            session.getAttributes().put("selectionStart", cursorData.getSelectionStart());
        }
        if (cursorData.getSelectionEnd() != null) {
            session.getAttributes().put("selectionEnd", cursorData.getSelectionEnd());
        }
        
        // 广播光标位置给其他用户
        // 从数据库获取用户信息
        User user = userMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "用户" + userId;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;
        
        CursorBroadcastData broadcastData = CursorBroadcastData.builder()
                .position(cursorData.getPosition())
                .selectionStart(cursorData.getSelectionStart())
                .selectionEnd(cursorData.getSelectionEnd())
                .userInfo(CursorBroadcastData.UserInfo.builder()
                        .nickname(nickname)
                        .avatarUrl(avatarUrl)
                        .color(generateUserColor(userId))
                        .build())
                .build();
        
        WebSocketMessage broadcastMessage = messageSender.createMessage(
                MessageType.CURSOR_BROADCAST, documentId, userId, broadcastData);
        
        messageSender.broadcastToDocument(documentId, broadcastMessage);
    }
    
    @Override
    public String getSupportedMessageType() {
        return "CURSOR_MOVE";
    }
    
    /**
     * 根据用户ID生成固定的颜色
     */
    private String generateUserColor(Long userId) {
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", 
                "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E2"
        };
        return colors[(int) (userId % colors.length)];
    }
}
