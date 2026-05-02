package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本快照实体
 */
@Data
@TableName("document_versions")
public class DocumentVersion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Integer versionNumber;
    
    private String contentSnapshot;
    
    private String contentHash;
    
    private String changeDescription;
    
    private Long createdBy;
    
    private LocalDateTime createdAt;
}
