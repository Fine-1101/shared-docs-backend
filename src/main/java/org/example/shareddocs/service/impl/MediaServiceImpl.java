package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.exception.BusinessException;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.request.ChunkUploadRequest;
import org.example.shareddocs.dto.request.UploadCompleteRequest;
import org.example.shareddocs.dto.request.UploadInitRequest;
import org.example.shareddocs.dto.response.ChunkUploadResult;
import org.example.shareddocs.dto.response.MediaAssetResponse;
import org.example.shareddocs.dto.response.UploadInitResponse;
import org.example.shareddocs.dto.response.UploadResponse;
import org.example.shareddocs.entity.MediaAsset;
import org.example.shareddocs.entity.UploadChunk;
import org.example.shareddocs.mapper.MediaAssetMapper;
import org.example.shareddocs.mapper.UploadChunkMapper;
import org.example.shareddocs.service.MediaService;
import org.example.shareddocs.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 媒体服务实现类（本地存储版本）
 * 生产环境建议使用MinIO或云存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl extends ServiceImpl<MediaAssetMapper, MediaAsset> implements MediaService {
    
    @Value("${media.upload-path:./uploads}")
    private String uploadPath;
    
    @Value("${media.chunk-size:5242880}")
    private Long defaultChunkSize;
    
    private final UploadChunkMapper uploadChunkMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    
    private static final String UPLOAD_SESSION_PREFIX = "upload:session:";
    private static final long UPLOAD_SESSION_TTL = 3600; // 1小时
    
    @Override
    @Transactional
    public UploadResponse uploadFile(Long documentId, Long userId, MultipartFile file) {
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String filename = UUID.randomUUID().toString() + extension;
            
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath, String.valueOf(documentId));
            Files.createDirectories(uploadDir);
            
            // 保存文件
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            
            // 保存记录到数据库
            MediaAsset mediaAsset = new MediaAsset();
            mediaAsset.setDocumentId(documentId);
            mediaAsset.setUploaderId(userId);
            mediaAsset.setAssetType(getAssetType(file.getContentType()));
            mediaAsset.setFilename(originalFilename);
            mediaAsset.setStoragePath(filePath.toString());
            mediaAsset.setMimeType(file.getContentType());
            mediaAsset.setFileSize(file.getSize());
            mediaAsset.setUploadStatus("COMPLETED");
            mediaAsset.setCreatedAt(LocalDateTime.now());
            
            save(mediaAsset);
            
            // 构建响应
            return UploadResponse.builder()
                    .mediaId(mediaAsset.getId())
                    .filename(originalFilename)
                    .storagePath(filePath.toString())
                    .url("/api/media/" + mediaAsset.getId())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadStatus("COMPLETED")
                    .build();
                    
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getFileUrl(Long mediaId) {
        MediaAsset mediaAsset = getById(mediaId);
        if (mediaAsset == null) {
            return null;
        }
        return "/api/media/" + mediaId;
    }
    
    @Override
    @Transactional
    public void deleteFile(Long mediaId) {
        MediaAsset mediaAsset = getById(mediaId);
        if (mediaAsset != null) {
            // 删除物理文件
            try {
                Files.deleteIfExists(Paths.get(mediaAsset.getStoragePath()));
            } catch (IOException e) {
                // 记录日志但不影响数据库删除
            }
            
            // 删除数据库记录
            removeById(mediaId);
        }
    }
    
    /**
     * 获取资源类型
     */
    private String getAssetType(String mimeType) {
        if (mimeType == null) {
            return "unknown";
        }
        if (mimeType.startsWith("image/")) {
            return "image";
        } else if (mimeType.startsWith("video/")) {
            return "video";
        } else if (mimeType.startsWith("audio/")) {
            return "audio";
        }
        return "file";
    }
    
    @Override
    @Transactional
    public UploadInitResponse initChunkUpload(UploadInitRequest request, Long userId) {
        // 创建媒体记录
        MediaAsset mediaAsset = new MediaAsset();
        mediaAsset.setDocumentId(request.getDocumentId());
        mediaAsset.setUploaderId(userId);
        mediaAsset.setAssetType(request.getMediaType());
        mediaAsset.setFilename(request.getFilename());
        mediaAsset.setMimeType(request.getMimeType());
        mediaAsset.setFileSize(request.getFileSize());
        mediaAsset.setFileHash(request.getFileHash());
        mediaAsset.setUploadStatus("uploading");
        mediaAsset.setCreatedAt(LocalDateTime.now());
        save(mediaAsset);
        
        // 生成上传会话ID
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        
        // 计算总分块数
        long chunkSize = request.getChunkSize() != null ? request.getChunkSize() : defaultChunkSize;
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);
        
        // 创建分块目录
        Path chunkDir = Paths.get(uploadPath, "chunks", uploadId);
        try {
            Files.createDirectories(chunkDir);
        } catch (IOException e) {
            throw new BusinessException("创建分块目录失败: " + e.getMessage());
        }
        
        // 保存上传会话信息到Redis
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("uploadId", uploadId);
        sessionInfo.put("mediaId", mediaAsset.getId());
        sessionInfo.put("documentId", request.getDocumentId());
        sessionInfo.put("userId", userId);
        sessionInfo.put("filename", request.getFilename());
        sessionInfo.put("fileSize", request.getFileSize());
        sessionInfo.put("fileHash", request.getFileHash());
        sessionInfo.put("mimeType", request.getMimeType());
        sessionInfo.put("mediaType", request.getMediaType());
        sessionInfo.put("chunkSize", chunkSize);
        sessionInfo.put("totalChunks", totalChunks);
        sessionInfo.put("uploadedChunks", new ArrayList<Integer>());
        sessionInfo.put("status", "uploading");
        sessionInfo.put("createdAt", System.currentTimeMillis());
        
        redisTemplate.opsForValue().set(
            UPLOAD_SESSION_PREFIX + uploadId, 
            sessionInfo, 
            UPLOAD_SESSION_TTL, 
            TimeUnit.SECONDS
        );
        
        log.info("初始化分块上传成功: uploadId={}, mediaId={}, totalChunks={}", 
                uploadId, mediaAsset.getId(), totalChunks);
        
        return UploadInitResponse.builder()
                .uploadId(uploadId)
                .mediaId(mediaAsset.getId())
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .uploadedChunks(new ArrayList<>())
                .status("uploading")
                .build();
    }
    
    @Override
    @Transactional
    public ChunkUploadResult uploadChunk(ChunkUploadRequest request) {
        String uploadId = request.getUploadId();
        Integer chunkIndex = request.getChunkIndex();
        MultipartFile file = request.getFile();
        
        // 获取上传会话信息
        Map<String, Object> sessionInfo = getSessionInfo(uploadId);
        if (sessionInfo == null) {
            throw new BusinessException("上传会话不存在或已过期");
        }
        
        if (!"uploading".equals(sessionInfo.get("status"))) {
            throw new BusinessException("上传会话状态异常: " + sessionInfo.get("status"));
        }
        
        Long mediaId = ((Number) sessionInfo.get("mediaId")).longValue();
        Long chunkSize = ((Number) sessionInfo.get("chunkSize")).longValue();
        Integer totalChunks = (Integer) sessionInfo.get("totalChunks");
        List<Integer> uploadedChunks = (List<Integer>) sessionInfo.get("uploadedChunks");
        
        // 验证分块索引
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new BusinessException("分块索引超出范围");
        }
        
        try {
            // 保存分块文件
            Path chunkDir = Paths.get(uploadPath, "chunks", uploadId);
            Path chunkPath = chunkDir.resolve("chunk_" + chunkIndex);
            Files.write(chunkPath, file.getBytes());
            
            // 记录分块上传信息到数据库
            LambdaQueryWrapper<UploadChunk> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UploadChunk::getMediaId, mediaId)
                   .eq(UploadChunk::getChunkIndex, chunkIndex);
            UploadChunk existingChunk = uploadChunkMapper.selectOne(wrapper);
            
            if (existingChunk == null) {
                UploadChunk uploadChunk = new UploadChunk();
                uploadChunk.setMediaId(mediaId);
                uploadChunk.setChunkIndex(chunkIndex);
                uploadChunk.setChunkSize((int) file.getSize());
                uploadChunk.setChunkHash(request.getChunkHash());
                uploadChunk.setStoragePath(chunkPath.toString());
                uploadChunk.setIsUploaded(1);
                uploadChunk.setCreatedAt(LocalDateTime.now());
                uploadChunk.setUpdatedAt(LocalDateTime.now());
                uploadChunkMapper.insert(uploadChunk);
            } else {
                existingChunk.setChunkHash(request.getChunkHash());
                existingChunk.setStoragePath(chunkPath.toString());
                existingChunk.setIsUploaded(1);
                existingChunk.setUpdatedAt(LocalDateTime.now());
                uploadChunkMapper.updateById(existingChunk);
            }
            
            // 更新已上传分块列表
            if (!uploadedChunks.contains(chunkIndex)) {
                uploadedChunks.add(chunkIndex);
                sessionInfo.put("uploadedChunks", uploadedChunks);
                redisTemplate.opsForValue().set(
                    UPLOAD_SESSION_PREFIX + uploadId, 
                    sessionInfo, 
                    UPLOAD_SESSION_TTL, 
                    TimeUnit.SECONDS
                );
            }
            
            // 计算进度
            double progress = (uploadedChunks.size() * 100.0) / totalChunks;
            
            log.debug("分块上传成功: uploadId={}, chunkIndex={}, progress={}%", 
                    uploadId, chunkIndex, String.format("%.2f", progress));
            
            return ChunkUploadResult.builder()
                    .uploadId(uploadId)
                    .chunkIndex(chunkIndex)
                    .uploaded(true)
                    .progress(progress)
                    .build();
                    
        } catch (IOException e) {
            log.error("分块上传失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            throw new BusinessException("分块上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public MediaAssetResponse completeUpload(UploadCompleteRequest request, Long userId) {
        String uploadId = request.getUploadId();
        
        // 获取上传会话信息
        Map<String, Object> sessionInfo = getSessionInfo(uploadId);
        if (sessionInfo == null) {
            throw new BusinessException("上传会话不存在或已过期");
        }
        
        Long mediaId = ((Number) sessionInfo.get("mediaId")).longValue();
        Long documentId = ((Number) sessionInfo.get("documentId")).longValue();
        Integer totalChunks = (Integer) sessionInfo.get("totalChunks");
        List<Integer> uploadedChunks = (List<Integer>) sessionInfo.get("uploadedChunks");
        String filename = (String) sessionInfo.get("filename");
        String mimeType = (String) sessionInfo.get("mimeType");
        String mediaType = (String) sessionInfo.get("mediaType");
        Long fileSize = ((Number) sessionInfo.get("fileSize")).longValue();
        
        // 验证所有分块是否已上传
        if (uploadedChunks.size() != totalChunks) {
            throw new BusinessException(
                String.format("分块上传未完成，已上传%d/%d个分块", uploadedChunks.size(), totalChunks)
            );
        }
        
        try {
            // 合并分块文件
            Path chunkDir = Paths.get(uploadPath, "chunks", uploadId);
            Path mergedFilePath = Paths.get(uploadPath, String.valueOf(documentId), 
                                           UUID.randomUUID().toString() + getFileExtension(filename));
            Files.createDirectories(mergedFilePath.getParent());
            
            try (RandomAccessFile mergedFile = new RandomAccessFile(mergedFilePath.toFile(), "rw")) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkPath = chunkDir.resolve("chunk_" + i);
                    byte[] chunkData = Files.readAllBytes(chunkPath);
                    mergedFile.write(chunkData);
                }
            }
            
            // 更新媒体记录
            MediaAsset mediaAsset = getById(mediaId);
            if (mediaAsset == null) {
                throw new BusinessException("媒体记录不存在");
            }
            mediaAsset.setStoragePath(mergedFilePath.toString());
            mediaAsset.setUploadStatus("completed");
            mediaAsset.setPlaceholderText(generatePlaceholderText(mediaAsset));
            updateById(mediaAsset);
            
            // 清理分块文件和会话
            deleteDirectory(chunkDir.toFile());
            redisTemplate.delete(UPLOAD_SESSION_PREFIX + uploadId);
            
            log.info("分块上传完成: uploadId={}, mediaId={}", uploadId, mediaId);
            
            // 构建响应
            return buildMediaAssetResponse(mediaAsset);
            
        } catch (IOException e) {
            log.error("合并分块失败: uploadId={}", uploadId, e);
            throw new BusinessException("合并分块失败: " + e.getMessage());
        }
    }
    
    @Override
    public void cancelUpload(String uploadId, Long userId) {
        Map<String, Object> sessionInfo = getSessionInfo(uploadId);
        if (sessionInfo == null) {
            return; // 会话不存在，无需处理
        }
        
        Long mediaId = ((Number) sessionInfo.get("mediaId")).longValue();
        
        // 删除媒体记录
        removeById(mediaId);
        
        // 删除分块记录
        LambdaQueryWrapper<UploadChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UploadChunk::getMediaId, mediaId);
        uploadChunkMapper.delete(wrapper);
        
        // 删除分块文件
        Path chunkDir = Paths.get(uploadPath, "chunks", uploadId);
        deleteDirectory(chunkDir.toFile());
        
        // 删除会话
        redisTemplate.delete(UPLOAD_SESSION_PREFIX + uploadId);
        
        log.info("取消上传: uploadId={}, mediaId={}", uploadId, mediaId);
    }
    
    @Override
    public UploadInitResponse getUploadStatus(String uploadId) {
        Map<String, Object> sessionInfo = getSessionInfo(uploadId);
        if (sessionInfo == null) {
            throw new BusinessException("上传会话不存在或已过期");
        }
        
        List<Integer> uploadedChunks = (List<Integer>) sessionInfo.get("uploadedChunks");
        Integer totalChunks = (Integer) sessionInfo.get("totalChunks");
        Long chunkSize = ((Number) sessionInfo.get("chunkSize")).longValue();
        Long mediaId = ((Number) sessionInfo.get("mediaId")).longValue();
        String status = (String) sessionInfo.get("status");
        
        return UploadInitResponse.builder()
                .uploadId(uploadId)
                .mediaId(mediaId)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .uploadedChunks(uploadedChunks)
                .status(status)
                .build();
    }
    
    @Override
    public PageResult<MediaAssetResponse> getDocumentMediaList(Long documentId, String mediaType,
                                                               Integer page, Integer pageSize) {
        // 构建查询条件
        LambdaQueryWrapper<MediaAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaAsset::getDocumentId, documentId);
        
        // 媒体类型筛选
        if (mediaType != null && !mediaType.isEmpty()) {
            wrapper.eq(MediaAsset::getAssetType, mediaType);
        }
        
        // 只返回已完成的上传
        wrapper.eq(MediaAsset::getUploadStatus, "completed");
        
        // 按创建时间降序
        wrapper.orderByDesc(MediaAsset::getCreatedAt);
        
        // 分页查询
        Page<MediaAsset> pageParam = new Page<>(page, pageSize);
        Page<MediaAsset> resultPage = page(pageParam, wrapper);
        
        // 转换为响应DTO
        List<MediaAssetResponse> responseList = resultPage.getRecords().stream()
                .map(this::buildMediaAssetResponse)
                .collect(Collectors.toList());
        
        return new PageResult<>(resultPage.getTotal(), page, pageSize, responseList);
    }
    
    /**
     * 获取上传会话信息
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSessionInfo(String uploadId) {
        Object sessionObj = redisTemplate.opsForValue().get(UPLOAD_SESSION_PREFIX + uploadId);
        if (sessionObj instanceof Map) {
            return (Map<String, Object>) sessionObj;
        }
        return null;
    }
    
    /**
     * 生成占位符文本
     */
    private String generatePlaceholderText(MediaAsset mediaAsset) {
        return "![" + mediaAsset.getFilename() + "](/api/media/" + mediaAsset.getId() + ")";
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    /**
     * 构建媒体资源响应
     */
    private MediaAssetResponse buildMediaAssetResponse(MediaAsset mediaAsset) {
        String uploaderName = "未知用户";
        try {
            uploaderName = userService.getUserInfo(mediaAsset.getUploaderId()).getNickname();
        } catch (Exception e) {
            log.warn("获取上传者信息失败: userId={}", mediaAsset.getUploaderId());
        }
        
        return MediaAssetResponse.builder()
                .id(mediaAsset.getId())
                .documentId(mediaAsset.getDocumentId())
                .filename(mediaAsset.getFilename())
                .fileSize(mediaAsset.getFileSize())
                .mimeType(mediaAsset.getMimeType())
                .mediaType(mediaAsset.getAssetType())
                .url("/api/media/" + mediaAsset.getId())
                .placeholderText(mediaAsset.getPlaceholderText())
                .width(mediaAsset.getWidth())
                .height(mediaAsset.getHeight())
                .uploaderId(mediaAsset.getUploaderId())
                .uploaderName(uploaderName)
                .createdAt(mediaAsset.getCreatedAt() != null ? 
                          mediaAsset.getCreatedAt().toString() : null)
                .build();
    }
}
