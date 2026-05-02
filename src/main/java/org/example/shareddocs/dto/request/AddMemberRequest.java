package org.example.shareddocs.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 添加成员请求DTO
 */
@Data
public class AddMemberRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "角色不能为空")
    private String role; // OWNER/EDITOR/VIEWER
}
