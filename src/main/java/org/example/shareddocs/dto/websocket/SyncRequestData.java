package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 同步请求数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequestData {
    
    /**
     * 最后同步的版本号
     */
    private Integer lastSyncedVersion;
    
    /**
     * 离线期间产生的操作列表
     */
    private List<PendingOperation> pendingOperations;
    
    /**
     * 本地内容哈希
     */
    private String localContentHash;
    
    /**
     * 当前光标位置
     */
    private Integer cursorPosition;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingOperation {
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
         * 插入/替换内容
         */
        private String content;
        
        /**
         * 删除/替换长度
         */
        private Integer length;
        
        /**
         * 客户端时间戳
         */
        private Long clientTimestamp;
    }
}
