package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线用户响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 状态：online、away、offline
     */
    private String status;
    
    /**
     * 光标位置
     */
    private Integer cursorPosition;
    
    /**
     * 加入时间
     */
    private String joinedAt;
}
