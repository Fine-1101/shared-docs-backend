package org.example.shareddocs.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.*;
import org.example.shareddocs.websocket.handler.MessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息分发器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSessionManager sessionManager;
    private final List<MessageHandler> messageHandlers;
    private final WebSocketMessageSender messageSender;
    
    // 消息类型到处理器的映射
    private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<>();
    
    // 消息类型到data类型的映射
    private final Map<String, Class<?>> dataTypeMap = new HashMap<>();
    
    // 标记是否已初始化
    private volatile boolean initialized = false;
    
    /**
     * 初始化处理器映射
     */
    public void init() {
        if (initialized) {
            return;
        }
        
        synchronized (this) {
            if (initialized) {
                return;
            }
            
            for (MessageHandler handler : messageHandlers) {
                handlerMap.put(handler.getSupportedMessageType(), handler);
                log.info("注册消息处理器：{}", handler.getSupportedMessageType());
            }
            
            // 初始化消息类型到data类型的映射
            initDataTypeMap();
            
            initialized = true;
            log.info("消息分发器初始化完成，共注册 {} 个处理器", handlerMap.size());
        }
    }
    
    /**
     * 初始化消息类型到data类型的映射
     */
    private void initDataTypeMap() {
        dataTypeMap.put("JOIN_DOCUMENT", JoinDocumentData.class);
        dataTypeMap.put("LEAVE_DOCUMENT", null);  // 没有data
        dataTypeMap.put("OPERATION", OperationData.class);
        dataTypeMap.put("CURSOR_MOVE", CursorMoveData.class);
        dataTypeMap.put("SYNC_REQUEST", SyncRequestData.class);
        dataTypeMap.put("HEARTBEAT", null);  // 没有data
        dataTypeMap.put("CHAT_MESSAGE", ChatMessageData.class);
        log.debug("消息类型映射初始化完成，共 {} 个类型", dataTypeMap.size());
    }
    
    /**
     * 解析消息
     */
    public WebSocketMessage parseMessage(String payload) throws IOException {
        // 先解析为JsonNode，以便手动处理data字段
        JsonNode rootNode = objectMapper.readTree(payload);
        
        // 提取消息类型
        String typeStr = rootNode.get("type").asText();
        MessageType type = MessageType.fromCode(typeStr);
        
        // 创建WebSocketMessage对象，但不包括data字段
        WebSocketMessage message = new WebSocketMessage();
        message.setType(type);
        
        if (rootNode.has("messageId")) {
            message.setMessageId(rootNode.get("messageId").asText());
        }
        if (rootNode.has("documentId")) {
            message.setDocumentId(rootNode.get("documentId").asText());
        }
        if (rootNode.has("userId")) {
            message.setUserId(rootNode.get("userId").asLong());
        }
        if (rootNode.has("username")) {
            message.setUsername(rootNode.get("username").asText());
        }
        if (rootNode.has("version")) {
            message.setVersion(rootNode.get("version").asInt());
        }
        if (rootNode.has("timestamp")) {
            message.setTimestamp(rootNode.get("timestamp").asLong());
        }
        
        // 根据消息类型反序列化data字段
        if (rootNode.has("data") && !rootNode.get("data").isNull()) {
            Class<?> dataClass = dataTypeMap.get(type.name());
            if (dataClass != null) {
                Object data = objectMapper.treeToValue(rootNode.get("data"), dataClass);
                message.setData(data);
                log.debug("✅ 成功反序列化data字段: type={}, class={}", type.name(), dataClass.getSimpleName());
            } else {
                log.warn("⚠️ 消息类型 {} 没有对应的data类型映射", type.name());
            }
        }
        
        return message;
    }
    
    /**
     * 分发消息
     */
    public void dispatch(WebSocketSession session, WebSocketMessage message) {
        MessageType type = message.getType();
        String messageTypeStr = type.name();
        
        MessageHandler handler = handlerMap.get(messageTypeStr);
        if (handler != null) {
            try {
                handler.handle(session, message);
            } catch (Exception e) {
                log.error("❌ 处理消息失败，类型：{}，错误：{}", messageTypeStr, e.getMessage(), e);
                messageSender.sendError(session, 500, "消息处理失败：" + e.getMessage());
            }
        } else {
            log.warn("未找到消息处理器：{}", messageTypeStr);
            messageSender.sendError(session, 400, "不支持的消息类型：" + messageTypeStr);
        }
    }
    
    /**
     * 发送消息给指定会话（委托给messageSender）
     */
    public void sendMessage(WebSocketSession session, WebSocketMessage message) {
        messageSender.sendMessage(session, message);
    }
    
    /**
     * 广播消息给文档的所有用户（委托给messageSender）
     */
    public void broadcastToDocument(Long documentId, WebSocketMessage message) {
        messageSender.broadcastToDocument(documentId, message);
    }
    

}
