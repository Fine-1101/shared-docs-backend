package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 初始化上传请求
 */
@Data
public class UploadInitRequest {
    
    /**
     * 所属文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String filename;
    
    /**
     * 文件总大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
    
    /**
     * 文件SHA-256哈希
     */
    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;
    
    /**
     * MIME类型
     */
    @NotBlank(message = "MIME类型不能为空")
    private String mimeType;
    
    /**
     * 媒体类型：image、video
     */
    @NotBlank(message = "媒体类型不能为空")
    private String mediaType;
    
    /**
     * 分块大小（字节），默认5MB
     */
    private Long chunkSize = 5242880L;
}
