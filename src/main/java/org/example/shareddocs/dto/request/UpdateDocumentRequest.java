package org.example.shareddocs.dto.request;

import lombok.Data;

/**
 * 更新文档请求DTO
 */
@Data
public class UpdateDocumentRequest {
    
    private String title;
    
    private String content;
    
    private Integer version;
}
