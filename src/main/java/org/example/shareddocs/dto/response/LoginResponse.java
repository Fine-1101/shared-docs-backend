package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    private String avatarUrl;
    
    private String token;
    
    private String refreshToken;
    
    private Long expiresIn;
}
