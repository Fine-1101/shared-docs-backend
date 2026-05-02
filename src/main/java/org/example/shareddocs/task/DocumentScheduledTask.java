package org.example.shareddocs.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentOperation;
import org.example.shareddocs.entity.UploadChunk;
import org.example.shareddocs.mapper.DocumentOperationMapper;
import org.example.shareddocs.mapper.UploadChunkMapper;
import org.example.shareddocs.service.DocumentService;
import org.example.shareddocs.service.VersionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 文档定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentScheduledTask {
    
    private final DocumentService documentService;
    private final VersionService versionService;
    private final DocumentOperationMapper operationMapper;
    private final UploadChunkMapper uploadChunkMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${media.upload-path:./uploads}")
    private String uploadPath;
    
    @Value("${document.operation-retention-days:30}")
    private int operationRetentionDays;
    
    @Value("${upload.chunk-retention-hours:24}")
    private int chunkRetentionHours;
    
    /**
     * 自动保存任务（每30秒执行一次）
     */
    @Scheduled(fixedRate = 30000)
    public void autoSaveDocuments() {
        log.debug("执行自动保存任务");
        // 从 Redis 缓存中获取待保存的文档并持久化
        // 注：当前实现中，操作已经实时保存到数据库，这里可以作为扩展点
        // 如果需要实现内存中的文档内容缓存定期同步，可以在此添加逻辑
    }
    
    /**
     * 创建版本快照任务（每10分钟执行一次）
     * 只有当文档内容发生变化时才会保存新版本
     */
    @Scheduled(fixedRate = 600000)  // 10分钟 = 600000毫秒
    public void createVersionSnapshots() {
        log.info("🔄 执行版本快照任务");
        
        // 获取所有活跃文档
        List<Document> documents = documentService.list();
        int savedCount = 0;
        int skippedCount = 0;
        
        for (Document doc : documents) {
            if (doc.getIsDeleted() == 0) {
                try {
                    // 检查内容是否发生变化
                    if (shouldCreateSnapshot(doc)) {
                        // 为文档创建版本快照
                        versionService.createVersionSnapshot(
                                doc.getId(),
                                doc.getContent(),
                                "自动保存",
                                doc.getCreatorId()
                        );
                        savedCount++;
                        log.info("✅ 为文档 {} 创建版本快照: title={}, contentLength={}", 
                                doc.getId(), doc.getTitle(), 
                                doc.getContent() != null ? doc.getContent().length() : 0);
                    } else {
                        skippedCount++;
                        log.debug("⏭️ 跳过文档 {}：内容未变化", doc.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ 为文档 {} 创建版本快照失败: {}", doc.getId(), e.getMessage(), e);
                }
            }
        }
        
        log.info("📊 版本快照任务完成: 总计={}, 保存={}, 跳过={}", 
                documents.size(), savedCount, skippedCount);
    }
    
    /**
     * 判断是否应该创建快照（内容是否发生变化）
     */
    private boolean shouldCreateSnapshot(Document document) {
        // 如果内容为空，不创建快照
        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            return false;
        }
        
        // 查询最新的版本快照
        LambdaQueryWrapper<org.example.shareddocs.entity.DocumentVersion> wrapper = 
                new LambdaQueryWrapper<>();
        wrapper.eq(org.example.shareddocs.entity.DocumentVersion::getDocumentId, document.getId())
               .orderByDesc(org.example.shareddocs.entity.DocumentVersion::getVersionNumber)
               .last("LIMIT 1");
        
        org.example.shareddocs.entity.DocumentVersion latestVersion = 
                versionService.getOne(wrapper);
        
        // 如果没有历史版本，需要创建第一个快照
        if (latestVersion == null) {
            log.debug("文档 {} 没有历史版本，需要创建初始快照", document.getId());
            return true;
        }
        
        // 比较当前内容与最新版本快照的内容
        String currentContent = document.getContent();
        String snapshotContent = latestVersion.getContentSnapshot();
        
        // 内容不同则创建新快照
        boolean hasChanged = !currentContent.equals(snapshotContent);
        
        if (hasChanged) {
            log.debug("文档 {} 内容已变化: 当前长度={}, 快照长度={}", 
                    document.getId(), 
                    currentContent != null ? currentContent.length() : 0,
                    snapshotContent != null ? snapshotContent.length() : 0);
        }
        
        return hasChanged;
    }
    
    /**
     * 清理过期数据任务（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredData() {
        log.info("执行过期数据清理任务");
        
        try {
            // 1. 清理过期的操作历史
            cleanupOldOperations();
            
            // 2. 清理未完成的上传分块
            cleanupIncompleteUploads();
            
            // 3. 清理过期的 Redis 会话
            cleanupExpiredSessions();
            
            log.info("过期数据清理任务完成");
        } catch (Exception e) {
            log.error("过期数据清理任务失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期的操作记录
     */
    private void cleanupOldOperations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(operationRetentionDays);
        
        LambdaQueryWrapper<DocumentOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(DocumentOperation::getCreatedAt, cutoffTime);
        
        int deletedCount = operationMapper.delete(wrapper);
        log.info("清理过期操作记录: {} 条", deletedCount);
    }
    
    /**
     * 清理未完成的上传分块
     */
    private void cleanupIncompleteUploads() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(chunkRetentionHours);
        
        // 查询过期的分块记录
        LambdaQueryWrapper<UploadChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(UploadChunk::getCreatedAt, cutoffTime)
               .eq(UploadChunk::getIsUploaded, 0);
        
        List<UploadChunk> expiredChunks = uploadChunkMapper.selectList(wrapper);
        
        // 删除分块文件和记录
        for (UploadChunk chunk : expiredChunks) {
            try {
                // 删除物理文件
                if (chunk.getStoragePath() != null) {
                    Files.deleteIfExists(Paths.get(chunk.getStoragePath()));
                }
                // 删除数据库记录
                uploadChunkMapper.deleteById(chunk.getId());
            } catch (Exception e) {
                log.warn("删除过期分块失败: chunkId={}", chunk.getId(), e);
            }
        }
        
        log.info("清理过期上传分块: {} 个", expiredChunks.size());
    }
    
    /**
     * 清理过期的 Redis 会话
     */
    private void cleanupExpiredSessions() {
        try {
            // 清理过期的上传会话
            Set<String> keys = redisTemplate.keys("upload:session:*");
            if (keys != null && !keys.isEmpty()) {
                // Redis 已设置 TTL，会自动过期，这里可以手动检查并删除
                log.debug("检查上传会话: {} 个", keys.size());
            }
        } catch (Exception e) {
            log.warn("清理 Redis 会话失败: {}", e.getMessage());
        }
    }
}
