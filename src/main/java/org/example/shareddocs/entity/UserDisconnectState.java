package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户断线状态记录实体
 */
@Data
@TableName("user_disconnect_state")
public class UserDisconnectState {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long documentId;
    
    private Integer cursorPosition;
    
    private String localPendingOps;
    
    private Integer lastSyncedVersion;
    
    private LocalDateTime disconnectedAt;
    
    private LocalDateTime reconnectedAt;
}
