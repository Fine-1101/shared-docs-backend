package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档内容响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContentResponse {
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 文档正文内容
     */
    private String content;
    
    /**
     * 当前版本号
     */
    private Integer version;
    
    /**
     * 内容哈希值（用于校验）
     */
    private String hash;
    
    /**
     * 更新时间
     */
    private String updatedAt;
}
