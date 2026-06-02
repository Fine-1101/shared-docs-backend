package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光标广播消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorBroadcastData {
    
    /**
     * 光标位置
     */
    private Integer position;
    
    /**
     * 选区起始（有选区时）
     */
    private Integer selectionStart;
    
    /**
     * 选区结束（有选区时）
     */
    private Integer selectionEnd;
    
    /**
     * 用户简要信息
     */
    private UserInfo userInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
        private String avatarUrl;
        private String color;
    }
}
