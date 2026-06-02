package org.example.shareddocs.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 消息类型枚举
 * 
 * ⚠️ 重要：所有消息类型的 code 字段使用小写+下划线格式（snake_case）
 * 例如："chat_message", "operation_broadcast", "user_joined"
 * 
 * 前端发送和接收消息时，type 字段必须使用小写下划线格式。
 * fromCode 方法兼容大写格式（如 "CHAT_MESSAGE"），但推荐使用小写。
 */
public enum MessageType {
    
    JOIN_DOCUMENT("join_document", "加入文档"),
    LEAVE_DOCUMENT("leave_document", "离开文档"),
    SAVE_VERSION("save_version", "保存版本"),  // ✅ 新增：手动保存版本
    OPERATION("operation", "编辑操作"),
    OPERATION_BROADCAST("operation_broadcast", "操作广播"),
    CURSOR_MOVE("cursor_move", "光标移动"),
    CURSOR_BROADCAST("cursor_broadcast", "光标广播"),
    USER_JOINED("user_joined", "用户加入"),
    USER_LEFT("user_left", "用户离开"),
    SYNC_REQUEST("sync_request", "同步请求"),
    SYNC_RESPONSE("sync_response", "同步响应"),
    HEARTBEAT("heartbeat", "心跳"),
    HEARTBEAT_ACK("heartbeat_ack", "心跳响应"),
    CHAT_MESSAGE("chat_message", "聊天消息"),
    CHAT_BROADCAST("chat_broadcast", "聊天广播"),
    SYSTEM_NOTIFICATION("system_notification", "系统通知"),
    ERROR("error", "错误");
    
    private final String code;
    private final String description;
    
    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    @JsonCreator
    public static MessageType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        
        // 先尝试精确匹配 code（小写+下划线）
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        
        // 再尝试匹配枚举 name（大写+下划线），提供兼容性
        try {
            return MessageType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + code + 
                ". Valid types: " + String.join(", ", 
                    java.util.Arrays.stream(values())
                        .map(MessageType::getCode)
                        .toArray(String[]::new)));
        }
    }
}
