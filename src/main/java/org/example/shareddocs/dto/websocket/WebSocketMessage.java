package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.shareddocs.common.enums.MessageType;

/**
 * WebSocket消息基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息ID（用于追踪）
     */
    private String messageId;
    
    /**
     * 文档ID（支持Long和UUID字符串）
     */
    private String documentId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 文档版本号（用于操作同步）
     */
    private Integer version;
    
    /**
     * 消息时间戳
     */
    private Long timestamp;
    
    /**
     * 消息数据（具体内容根据type不同而不同）
     */
    private Object data;
}
