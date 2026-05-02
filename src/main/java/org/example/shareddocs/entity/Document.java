package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@TableName("documents")
public class Document {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String docUuid;
    
    private String title;
    
    private String content;
    
    private String contentHash;
    
    private Long creatorId;
    
    private Integer isDeleted;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
