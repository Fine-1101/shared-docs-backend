package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档成员响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    
    private Long id;
    
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    private String avatarUrl;
    
    private String role;
    
    private LocalDateTime joinedAt;
    
    private LocalDateTime lastAccessAt;
}
