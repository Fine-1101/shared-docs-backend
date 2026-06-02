package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块上传结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResult {
    
    /**
     * 上传会话ID
     */
    private String uploadId;
    
    /**
     * 分块索引
     */
    private Integer chunkIndex;
    
    /**
     * 是否上传成功
     */
    private Boolean uploaded;
    
    /**
     * 上传进度 0-100
     */
    private Double progress;
}
