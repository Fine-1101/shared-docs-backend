package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体资源实体
 */
@Data
@TableName("media_assets")
public class MediaAsset {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private Long uploaderId;
    
    private String assetType;
    
    private String filename;
    
    private String storagePath;
    
    private String mimeType;
    
    private Long fileSize;
    
    private String fileHash;
    
    private Integer width;
    
    private Integer height;
    
    private String placeholderText;
    
    private String uploadStatus;
    
    private LocalDateTime createdAt;
}
