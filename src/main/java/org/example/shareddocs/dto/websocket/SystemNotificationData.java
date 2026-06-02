package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统通知消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationData {
    
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 消息类型：system
     */
    private String type;
    
    /**
     * 通知类型：user_joined、user_left、version_created、document_saved
     */
    private String notificationType;
    
    /**
     * 通知文本
     */
    private String content;
    
    /**
     * 关联用户ID
     */
    private Long relatedUserId;
    
    /**
     * 关联数据
     */
    private Object relatedData;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
