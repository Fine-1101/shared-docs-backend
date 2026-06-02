package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    
    private Long id;
    
    private String docUuid;
    
    private String title;
    
    private String content;
    
    private Long creatorId;
    
    private String creatorName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Integer memberCount;

    private String role;
}
