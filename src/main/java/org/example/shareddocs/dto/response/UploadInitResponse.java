package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 初始化上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitResponse {
    
    /**
     * 上传会话ID
     */
    private String uploadId;
    
    /**
     * 媒体记录ID
     */
    private Long mediaId;
    
    /**
     * 确认的分块大小
     */
    private Long chunkSize;
    
    /**
     * 总分块数
     */
    private Integer totalChunks;
    
    /**
     * 已上传的分块索引（断点续传）
     */
    private List<Integer> uploadedChunks;
    
    /**
     * 状态：pending、uploading、completed
     */
    private String status;
}
