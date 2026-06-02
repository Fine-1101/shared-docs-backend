package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档操作记录实体
 */
@Data
@TableName("document_operations")
public class DocumentOperation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Long userId;
    
    private String operationType;
    
    private Integer position;
    
    private Integer length;
    
    private String content;
    
    private Integer cursorPosition;
    
    private Integer versionBefore;
    
    private Integer versionAfter;
    
    private String clientId;
    
    private LocalDateTime createdAt;
}
