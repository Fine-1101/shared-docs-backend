package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入文档消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinDocumentData {
    
    /**
     * 初始光标位置
     */
    private Integer cursorPosition;
    
    /**
     * 客户端信息
     */
    private ClientInfo clientInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        /**
         * 平台：web/desktop/mobile
         */
        private String platform;
        
        /**
         * 客户端版本
         */
        private String version;
    }
}
