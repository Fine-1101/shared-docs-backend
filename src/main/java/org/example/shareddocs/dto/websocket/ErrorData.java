package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorData {
    
    /**
     * 错误代码
     */
    private Integer code;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 原始消息ID（用于关联）
     */
    private String originalMessageId;
}
