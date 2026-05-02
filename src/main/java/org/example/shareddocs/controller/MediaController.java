package org.example.shareddocs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.request.ChunkUploadRequest;
import org.example.shareddocs.dto.request.UploadCompleteRequest;
import org.example.shareddocs.dto.request.UploadInitRequest;
import org.example.shareddocs.dto.response.ChunkUploadResult;
import org.example.shareddocs.dto.response.MediaAssetResponse;
import org.example.shareddocs.dto.response.UploadInitResponse;
import org.example.shareddocs.dto.response.UploadResponse;
import org.example.shareddocs.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 媒体控制器
 */
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {
    
    private final MediaService mediaService;
    
    /**
     * 上传文件（简单上传）
     */
    @PostMapping("/documents/{docId}/upload")
    public Result<UploadResponse> uploadFile(HttpServletRequest request,
                                             @PathVariable Long docId,
                                             @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        UploadResponse response = mediaService.uploadFile(docId, userId, file);
        return Result.success(response);
    }
    
    /**
     * 初始化分块上传
     */
    @PostMapping("/upload/init")
    public Result<UploadInitResponse> initChunkUpload(HttpServletRequest request,
                                                       @Valid @RequestBody UploadInitRequest initRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        UploadInitResponse response = mediaService.initChunkUpload(initRequest, userId);
        return Result.success(response);
    }
    
    /**
     * 上传分块
     */
    @PostMapping("/upload/chunk")
    public Result<ChunkUploadResult> uploadChunk(@Valid @ModelAttribute ChunkUploadRequest chunkRequest) {
        ChunkUploadResult result = mediaService.uploadChunk(chunkRequest);
        return Result.success(result);
    }
    
    /**
     * 完成上传（合并分块）
     */
    @PostMapping("/upload/complete")
    public Result<MediaAssetResponse> completeUpload(HttpServletRequest request,
                                                      @Valid @RequestBody UploadCompleteRequest completeRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        MediaAssetResponse response = mediaService.completeUpload(completeRequest, userId);
        return Result.success(response);
    }
    
    /**
     * 取消上传
     */
    @PostMapping("/upload/cancel")
    public Result<Void> cancelUpload(HttpServletRequest request,
                                      @RequestBody UploadCompleteRequest cancelRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        mediaService.cancelUpload(cancelRequest.getUploadId(), userId);
        return Result.successVoid("取消成功");
    }
    
    /**
     * 获取上传状态
     */
    @GetMapping("/upload/status/{uploadId}")
    public Result<UploadInitResponse> getUploadStatus(@PathVariable String uploadId) {
        UploadInitResponse response = mediaService.getUploadStatus(uploadId);
        return Result.success(response);
    }
    
    /**
     * 获取文档媒体列表
     */
    @GetMapping("/documents/{docId}/media")
    public Result<PageResult<MediaAssetResponse>> getDocumentMediaList(@PathVariable Long docId,
                                                                        @RequestParam(required = false) String mediaType,
                                                                        @RequestParam(defaultValue = "1") Integer page,
                                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<MediaAssetResponse> result = mediaService.getDocumentMediaList(docId, mediaType, page, pageSize);
        return Result.success(result);
    }
    
    /**
     * 获取文件
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> getFile(@PathVariable Long mediaId) {
        try {
            String url = mediaService.getFileUrl(mediaId);
            if (url == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(url.replace("/api/media/", ""));
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{mediaId}")
    public Result<Void> deleteFile(@PathVariable Long mediaId) {
        mediaService.deleteFile(mediaId);
        return Result.successVoid("删除成功");
    }
}
