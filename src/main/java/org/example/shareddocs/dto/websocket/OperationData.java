package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编辑操作消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationData {
    
    /**
     * 操作唯一ID（客户端生成）
     */
    private String operationId;
    
    /**
     * 操作类型：insert、delete、replace
     */
    private String type;
    
    /**
     * 操作起始位置
     */
    private Integer position;
    
    /**
     * 插入/替换内容（insert/replace时）
     */
    private String content;
    
    /**
     * 删除/替换长度（delete/replace时）
     */
    private Integer length;
    
    /**
     * 操作后光标位置
     */
    private Integer cursorAfter;
}
