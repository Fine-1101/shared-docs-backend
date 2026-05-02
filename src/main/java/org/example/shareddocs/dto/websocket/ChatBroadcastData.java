package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天广播数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBroadcastData {
    
    /**
     * 服务端生成的消息ID
     */
    private Long id;
    
    /**
     * 消息类型：chat
     */
    private String type;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * @的用户ID列表
     */
    private java.util.List<Long> mentionUsers;
    
    /**
     * 创建时间
     */
    private String createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
        private String avatarUrl;
    }
}
