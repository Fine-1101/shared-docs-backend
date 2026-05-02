package org.example.shareddocs.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建文档请求DTO
 */
@Data
public class CreateDocumentRequest {
    
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    private String content;
}
