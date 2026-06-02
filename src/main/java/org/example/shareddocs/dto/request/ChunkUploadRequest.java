package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分块上传请求
 */
@Data
public class ChunkUploadRequest {
    
    /**
     * 上传会话ID
     */
    @NotBlank(message = "上传会话ID不能为空")
    private String uploadId;
    
    /**
     * 分块索引（从0开始）
     */
    @NotNull(message = "分块索引不能为空")
    private Integer chunkIndex;
    
    /**
     * 分块MD5哈希
     */
    @NotBlank(message = "分块哈希不能为空")
    private String chunkHash;
    
    /**
     * 分块文件数据
     */
    @NotNull(message = "分块文件不能为空")
    private MultipartFile file;
}
