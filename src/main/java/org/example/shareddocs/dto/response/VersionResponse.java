package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档版本响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    
    private Long id;
    
    private Long documentId;
    
    private Integer versionNumber;
    
    private String changeDescription;
    
    private Long createdBy;
    
    private String creatorName;
    
    private LocalDateTime createdAt;
    
    private Boolean isCurrent;  // 是否为当前版本
}
