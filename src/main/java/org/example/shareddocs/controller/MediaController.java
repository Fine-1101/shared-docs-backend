package org.example.shareddocs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 媒体控制器
 */
@Slf4j
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
            String storagePath = mediaService.getFileUrl(mediaId);
            if (storagePath == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(storagePath);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.error("文件不存在或不可读: {}", storagePath);
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件的 MIME 类型
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String mimeType = Files.probeContentType(filePath);
            if (mimeType != null) {
                mediaType = MediaType.parseMediaType(mimeType);
            }
            
            // 图片类型直接在浏览器显示，其他类型下载
            if (mediaType.includes(MediaType.IMAGE_JPEG) || 
                mediaType.includes(MediaType.IMAGE_PNG) ||
                mediaType.includes(MediaType.IMAGE_GIF) ||
                mediaType.getSubtype().startsWith("image")) {
                // 图片：内联显示
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                // 其他文件：下载
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                               "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
                    
        } catch (MalformedURLException e) {
            log.error("文件URL格式错误", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("获取文件失败", e);
            return ResponseEntity.internalServerError().build();
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
