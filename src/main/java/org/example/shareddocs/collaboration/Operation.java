package org.example.shareddocs.collaboration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编辑操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operation {
    
    /**
     * 操作类型
     */
    private OperationType type;
    
    /**
     * 操作位置
     */
    private Integer position;
    
    /**
     * 操作长度（删除时使用）
     */
    private Integer length;
    
    /**
     * 操作内容（插入时使用）
     */
    private String content;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 克隆操作
     */
    public Operation clone() {
        return Operation.builder()
                .type(this.type)
                .position(this.position)
                .length(this.length)
                .content(this.content)
                .clientId(this.clientId)
                .version(this.version)
                .build();
    }
}
