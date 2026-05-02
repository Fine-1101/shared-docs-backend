package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光标移动消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorMoveData {
    
    /**
     * 光标位置
     */
    private Integer position;
    
    /**
     * 选区起始位置
     */
    private Integer selectionStart;
    
    /**
     * 选区结束位置
     */
    private Integer selectionEnd;
    
    /**
     * 客户端ID
     */
    private String clientId;
}
