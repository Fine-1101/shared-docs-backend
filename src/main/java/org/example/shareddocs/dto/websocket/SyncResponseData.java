package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 同步响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponseData {
    
    /**
     * 服务端当前版本号
     */
    private Integer currentVersion;
    
    /**
     * 全量内容（首次同步或差异过大时）
     */
    private String currentContent;
    
    /**
     * 错过的操作列表
     */
    private List<MissedOperation> missedOperations;
    
    /**
     * 服务端接受的离线操作ID列表
     */
    private List<String> acceptedOperations;
    
    /**
     * 被拒绝的操作（冲突无法解决）
     */
    private List<RejectedOperation> rejectedOperations;
    
    /**
     * 当前在线用户列表
     */
    private List<OnlineUser> onlineUsers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissedOperation {
        private String operationId;
        private String type;
        private Integer position;
        private String content;
        private Integer length;
        private Long userId;
        private Integer version;
        private Long timestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedOperation {
        private String operationId;
        private String reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineUser {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private Integer cursorPosition;
    }
}
