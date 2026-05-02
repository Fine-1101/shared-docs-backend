package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档成员关联实体
 */
@Data
@TableName("document_members")
public class DocumentMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Long userId;
    
    private String role;
    
    private LocalDateTime joinedAt;
    
    private LocalDateTime lastAccessAt;
}
