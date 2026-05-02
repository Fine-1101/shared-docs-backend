package org.example.shareddocs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分块上传记录实体
 */
@Data
@TableName("upload_chunks")
public class UploadChunk {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long mediaId;
    
    private Integer chunkIndex;
    
    private Integer chunkSize;
    
    private String chunkHash;
    
    private String storagePath;
    
    private Integer isUploaded;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
