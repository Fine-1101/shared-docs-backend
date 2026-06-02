package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作广播消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationBroadcastData {
    
    /**
     * 操作唯一ID
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
     * 是否经过了服务端变换
     */
    private Boolean transformed;
    
    /**
     * 操作用户ID
     */
    private Long userId;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
