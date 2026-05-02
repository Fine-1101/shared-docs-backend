package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话实体
 */
@Data
@TableName("user_sessions")
public class UserSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long documentId;
    
    private String sessionToken;
    
    private String connectionId;
    
    private Integer cursorPosition;
    
    private String status;
    
    private LocalDateTime lastHeartbeat;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
