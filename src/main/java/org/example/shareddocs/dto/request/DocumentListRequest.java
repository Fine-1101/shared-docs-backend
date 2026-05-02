package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档列表查询请求
 */
@Data
public class DocumentListRequest {
    
    /**
     * 页码，默认1
     */
    private Integer page = 1;
    
    /**
     * 每页条数，默认20
     */
    private Integer pageSize = 20;
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 排序字段
     */
    private String sortBy = "updatedAt";
    
    /**
     * 排序方向
     */
    private String sortOrder = "desc";
}
