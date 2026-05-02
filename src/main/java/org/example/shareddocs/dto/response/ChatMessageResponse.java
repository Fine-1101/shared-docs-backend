package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    
    private Long id;
    
    private Long documentId;
    
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    private String avatarUrl;
    
    private String messageType;
    
    private String content;
    
    private LocalDateTime createdAt;
}
