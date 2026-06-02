package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    
    private Long mediaId;
    
    private String filename;
    
    private String storagePath;
    
    private String url;
    
    private Long fileSize;
    
    private String mimeType;
    
    private String uploadStatus; // UPLOADING/COMPLETED/FAILED
}
