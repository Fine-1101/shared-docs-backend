package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体资源响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetResponse {
    
    /**
     * 媒体ID
     */
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 媒体类型：image、video
     */
    private String mediaType;
    
    /**
     * 访问URL
     */
    private String url;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 文档中插入的占位符文本
     */
    private String placeholderText;
    
    /**
     * 宽度
     */
    private Integer width;
    
    /**
     * 高度
     */
    private Integer height;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 上传者名称
     */
    private String uploaderName;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
