package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 完成上传请求
 */
@Data
public class UploadCompleteRequest {
    
    /**
     * 上传会话ID
     */
    @NotBlank(message = "上传会话ID不能为空")
    private String uploadId;
    
    /**
     * 完整文件哈希（校验用）
     */
    private String fileHash;
}
