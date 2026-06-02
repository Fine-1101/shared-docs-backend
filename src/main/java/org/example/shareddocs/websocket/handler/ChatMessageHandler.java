package org.example.shareddocs.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.ChatBroadcastData;
import org.example.shareddocs.dto.websocket.ChatMessageData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.service.ChatService;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

/**
 * 聊天消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final UserMapper userMapper;
    private final ChatService chatService;
    private final DocumentIdResolver documentIdResolver;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = documentIdResolver.resolve(message.getDocumentId());
        if (documentId == null) {
            log.warn(" 聊天消息中无效的文档ID: {}", message.getDocumentId());
            return;
        }
        
        ChatMessageData chatData = (ChatMessageData) message.getData();
        
        log.debug("收到用户 {} 的聊天消息：{}", userId, chatData.getContent());
        
        // 保存聊天消息到数据库
        org.example.shareddocs.entity.ChatMessage savedMessage = chatService.sendMessage(
                documentId, userId, chatData.getContent());
        
        // 从数据库获取用户信息
        User user = userMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "用户" + userId;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;
        
        // 广播聊天消息给文档中的所有用户
        ChatBroadcastData broadcastData = ChatBroadcastData.builder()
                .id(savedMessage.getId())
                .type("chat")
                .userId(userId)
                .userInfo(ChatBroadcastData.UserInfo.builder()
                        .nickname(nickname)
                        .avatarUrl(avatarUrl)
                        .build())
                .content(chatData.getContent())
                .mentionUsers(chatData.getMentionUsers())
                .createdAt(savedMessage.getCreatedAt() != null ? 
                          savedMessage.getCreatedAt().toString() : Instant.now().toString())
                .build();
        
        WebSocketMessage broadcastMessage = messageSender.createMessage(
                MessageType.CHAT_BROADCAST, documentId, null, broadcastData);
        
        messageSender.broadcastToDocument(documentId, broadcastMessage);
        log.debug("聊天消息已广播到文档 {}", documentId);
    }
    
    @Override
    public String getSupportedMessageType() {
        return "CHAT_MESSAGE";
    }
}
