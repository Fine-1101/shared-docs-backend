package org.example.shareddocs.service;

import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.request.ChunkUploadRequest;
import org.example.shareddocs.dto.request.UploadCompleteRequest;
import org.example.shareddocs.dto.request.UploadInitRequest;
import org.example.shareddocs.dto.response.ChunkUploadResult;
import org.example.shareddocs.dto.response.MediaAssetResponse;
import org.example.shareddocs.dto.response.UploadInitResponse;
import org.example.shareddocs.dto.response.UploadResponse;
import org.example.shareddocs.entity.MediaAsset;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体服务接口
 */
public interface MediaService {
    
    /**
     * 上传文件（简单上传）
     */
    UploadResponse uploadFile(Long documentId, Long userId, MultipartFile file);
    
    /**
     * 初始化分块上传
     */
    UploadInitResponse initChunkUpload(UploadInitRequest request, Long userId);
    
    /**
     * 上传分块
     */
    ChunkUploadResult uploadChunk(ChunkUploadRequest request);
    
    /**
     * 完成上传（合并分块）
     */
    MediaAssetResponse completeUpload(UploadCompleteRequest request, Long userId);
    
    /**
     * 取消上传
     */
    void cancelUpload(String uploadId, Long userId);
    
    /**
     * 获取上传状态
     */
    UploadInitResponse getUploadStatus(String uploadId);
    
    /**
     * 获取文档媒体列表（支持分页和类型筛选）
     */
    PageResult<MediaAssetResponse> getDocumentMediaList(Long documentId, String mediaType, 
                                                        Integer page, Integer pageSize);
    
    /**
     * 获取文件URL
     */
    String getFileUrl(Long mediaId);
    
    /**
     * 根据ID获取媒体资源
     */
    MediaAsset getMediaAssetById(Long mediaId);
    
    /**
     * 删除文件
     */
    void deleteFile(Long mediaId);
}
