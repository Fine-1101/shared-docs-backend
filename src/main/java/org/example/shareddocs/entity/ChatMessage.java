package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_messages")
public class ChatMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Long userId;
    
    private String messageType;
    
    private String content;
    
    private LocalDateTime createdAt;
}
