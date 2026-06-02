package org.example.shareddocs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.response.VersionCompareResponse;
import org.example.shareddocs.dto.response.VersionResponse;
import org.example.shareddocs.entity.DocumentVersion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本服务接口
 */
public interface VersionService extends IService<DocumentVersion> {
    
    /**
     * 创建版本快照
     */
    DocumentVersion createVersionSnapshot(Long documentId, String content, 
                                         String changeDescription, Long userId);
    
    /**
     * 获取文档版本列表（支持分页和时间筛选）
     */
    PageResult<VersionResponse> getDocumentVersions(Long documentId, Integer page, Integer pageSize,
                                                    LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取指定版本内容
     */
    String getVersionContent(Long versionId);
    
    /**
     * 回滚到指定版本
     */
    void rollbackToVersion(Long documentId, Long versionId);
    
    /**
     * 恢复到指定版本（将版本文本恢复到当前文档）
     */
    void restoreVersion(Long documentId, Long versionId);
    
    /**
     * 对比两个版本的差异
     */
    VersionCompareResponse compareVersions(Long documentId, Integer versionA, Integer versionB);
    
    /**
     * 删除指定版本
     */
    void deleteVersion(Long versionId);
}
