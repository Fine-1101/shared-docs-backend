package org.example.shareddocs.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页响应结果
 */
@Data
public class PageResult<T> {
    
    private Long total;
    
    private Integer page;
    
    private Integer pageSize;
    
    private List<T> list;
    
    private Boolean hasMore;
    
    public PageResult() {
    }
    
    public PageResult(Long total, Integer page, Integer pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
        this.hasMore = (long) page * pageSize < total;
    }
    
    public PageResult(Long total, Integer page, Integer pageSize, List<T> list, Boolean hasMore) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
        this.hasMore = hasMore;
    }
}
