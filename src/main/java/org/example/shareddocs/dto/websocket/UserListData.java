package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户列表广播数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListData {
    
    /**
     * 在线用户列表
     */
    private List<OnlineUser> users;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineUser {
        private Long userId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private Integer cursorPosition;
        private String clientId;
    }
}
