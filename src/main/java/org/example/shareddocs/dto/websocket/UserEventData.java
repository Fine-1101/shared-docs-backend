package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户事件数据（加入/离开）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventData {
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    /**
     * 当前在线总人数
     */
    private Integer onlineCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String nickname;
        private String avatarUrl;
    }
}
