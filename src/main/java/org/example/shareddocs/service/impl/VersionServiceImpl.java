package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.exception.NotFoundException;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.common.utils.DiffUtils;
import org.example.shareddocs.dto.response.VersionCompareResponse;
import org.example.shareddocs.dto.response.VersionDiffItem;
import org.example.shareddocs.dto.response.VersionResponse;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentVersion;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.mapper.DocumentVersionMapper;
import org.example.shareddocs.service.UserService;
import org.example.shareddocs.service.VersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 版本服务实现类
 */
@Service
@RequiredArgsConstructor
public class VersionServiceImpl extends ServiceImpl<DocumentVersionMapper, DocumentVersion> implements VersionService {
    
    private final DocumentMapper documentMapper;
    private final UserService userService;
    
    @Override
    @Transactional
    public DocumentVersion createVersionSnapshot(Long documentId, String content, 
                                                String changeDescription, Long userId) {
        // 获取当前最大版本号
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId)
               .orderByDesc(DocumentVersion::getVersionNumber)
               .last("LIMIT 1");
        DocumentVersion lastVersion = getOne(wrapper);
        
        int nextVersion = (lastVersion != null ? lastVersion.getVersionNumber() : 0) + 1;
        
        // 创建新版本
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setVersionNumber(nextVersion);
        version.setContentSnapshot(content);
        version.setChangeDescription(changeDescription != null ? changeDescription : "自动保存");
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        
        save(version);
        return version;
    }
    
    @Override
    public PageResult<VersionResponse> getDocumentVersions(Long documentId, Integer page, Integer pageSize,
                                                           LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId);
        
        // 时间筛选
        if (startTime != null) {
            wrapper.ge(DocumentVersion::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(DocumentVersion::getCreatedAt, endTime);
        }
        
        // 按版本号降序排列（最新的在前）
        wrapper.orderByDesc(DocumentVersion::getVersionNumber);
        
        // 分页查询
        Page<DocumentVersion> pageParam = new Page<>(page, pageSize);
        Page<DocumentVersion> resultPage = page(pageParam, wrapper);
        
        List<DocumentVersion> versions = resultPage.getRecords();
        
        // 获取文档的当前最新版本号
        Document document = documentMapper.selectById(documentId);
        Integer currentVersionNumber = null;
        if (document != null) {
            // 查询最新版本号
            LambdaQueryWrapper<DocumentVersion> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.eq(DocumentVersion::getDocumentId, documentId)
                        .orderByDesc(DocumentVersion::getVersionNumber)
                        .last("LIMIT 1");
            DocumentVersion latestVersion = getOne(latestWrapper);
            if (latestVersion != null) {
                currentVersionNumber = latestVersion.getVersionNumber();
            }
        }
        
        // 转换为响应DTO
        final Integer finalCurrentVersionNumber = currentVersionNumber;
        List<VersionResponse> responseList = versions.stream()
                .map(v -> VersionResponse.builder()
                        .id(v.getId())
                        .documentId(v.getDocumentId())
                        .versionNumber(v.getVersionNumber())
                        .changeDescription(v.getChangeDescription())
                        .createdBy(v.getCreatedBy())
                        .createdAt(v.getCreatedAt())
                        .isCurrent(finalCurrentVersionNumber != null && v.getVersionNumber().equals(finalCurrentVersionNumber))
                        .build())
                .collect(Collectors.toList());
        
        return new PageResult<>(resultPage.getTotal(), page, pageSize, responseList);
    }
    
    @Override
    public String getVersionContent(Long versionId) {
        DocumentVersion version = getById(versionId);
        if (version == null) {
            throw new NotFoundException("版本不存在");
        }
        return version.getContentSnapshot();
    }
    
    @Override
    @Transactional
    public void rollbackToVersion(Long documentId, Long versionId) {
        DocumentVersion version = getById(versionId);
        if (version == null || !version.getDocumentId().equals(documentId)) {
            throw new NotFoundException("版本不存在");
        }
        
        // 更新文档内容为指定版本
        Document document = documentMapper.selectById(documentId);
        if (document != null) {
            document.setContent(version.getContentSnapshot());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
        }
    }
    
    @Override
    @Transactional
    public void restoreVersion(Long documentId, Long versionId) {
        // 获取版本文本
        DocumentVersion version = getById(versionId);
        if (version == null || !version.getDocumentId().equals(documentId)) {
            throw new NotFoundException("版本不存在");
        }
        
        // 恢复文档内容到该版本
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new NotFoundException("文档不存在");
        }
        
        // 更新文档内容
        document.setContent(version.getContentSnapshot());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }
    
    @Override
    public VersionCompareResponse compareVersions(Long documentId, Integer versionA, Integer versionB) {
        // 查询两个版本
        LambdaQueryWrapper<DocumentVersion> wrapperA = new LambdaQueryWrapper<>();
        wrapperA.eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getVersionNumber, versionA);
        DocumentVersion verA = getOne(wrapperA);
        
        LambdaQueryWrapper<DocumentVersion> wrapperB = new LambdaQueryWrapper<>();
        wrapperB.eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getVersionNumber, versionB);
        DocumentVersion verB = getOne(wrapperB);
        
        if (verA == null || verB == null) {
            throw new NotFoundException("版本不存在");
        }
        
        // 计算差异
        List<VersionDiffItem> diffs = DiffUtils.computeDiff(
                verA.getContentSnapshot(), 
                verB.getContentSnapshot()
        );
        
        // 计算摘要
        VersionCompareResponse.DiffSummary summary = DiffUtils.calculateSummary(diffs);
        
        // 获取创建者信息
        String creatorA = "未知用户";
        String creatorB = "未知用户";
        try {
            creatorA = userService.getUserInfo(verA.getCreatedBy()).getNickname();
            creatorB = userService.getUserInfo(verB.getCreatedBy()).getNickname();
        } catch (Exception e) {
            // 忽略错误，使用默认值
        }
        
        // 构建响应
        return VersionCompareResponse.builder()
                .versionA(VersionCompareResponse.VersionInfo.builder()
                        .versionNumber(verA.getVersionNumber())
                        .createdAt(verA.getCreatedAt().toString())
                        .creatorName(creatorA)
                        .build())
                .versionB(VersionCompareResponse.VersionInfo.builder()
                        .versionNumber(verB.getVersionNumber())
                        .createdAt(verB.getCreatedAt().toString())
                        .creatorName(creatorB)
                        .build())
                .diff(diffs)
                .summary(summary)
                .build();
    }
    
    @Override
    @Transactional
    public void deleteVersion(Long versionId) {
        DocumentVersion version = getById(versionId);
        if (version == null) {
            throw new NotFoundException("版本不存在");
        }
        
        removeById(versionId);
    }
}
