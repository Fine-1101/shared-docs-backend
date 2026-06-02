package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest {
    
    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 原密码（修改密码时需要）
     */
    private String oldPassword;
    
    /**
     * 新密码（修改密码时需要）
     */
    @Size(min = 6, message = "密码长度至少6位")
    private String newPassword;
}
