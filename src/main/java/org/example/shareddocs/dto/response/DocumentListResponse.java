package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListResponse {
    
    /**
     * 总条数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 文档列表
     */
    private List<DocumentItem> list;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentItem {
        private Long id;
        private String docUuid;
        private String title;
        private Long creatorId;
        private String creatorName;
        private Integer version;
        private String createdAt;
        private String updatedAt;
        private String lastAccessAt;
    }
}
