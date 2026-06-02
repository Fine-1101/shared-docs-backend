package org.example.shareddocs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.request.VersionCompareRequest;
import org.example.shareddocs.dto.request.VersionCreateRequest;
import org.example.shareddocs.dto.response.VersionCompareResponse;
import org.example.shareddocs.dto.response.VersionResponse;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentVersion;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.service.VersionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本管理控制器
 */
@RestController
@RequestMapping("/documents/{docId}/versions")
@RequiredArgsConstructor
public class VersionController {
    
    private final VersionService versionService;
    private final DocumentMapper documentMapper;
    
    /**
     * 根据docId（支持ID或UUID）获取文档ID
     */
    private Long resolveDocumentId(String docId) {
        try {
            // 尝试解析为数字ID
            return Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 按UUID查询
            LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Document::getDocUuid, docId)
                   .eq(Document::getIsDeleted, 0);
            Document document = documentMapper.selectOne(wrapper);
            if (document == null) {
                throw new RuntimeException("文档不存在");
            }
            return document.getId();
        }
    }
    
    /**
     * 获取版本列表（支持分页和时间筛选）
     */
    @GetMapping
    public Result<PageResult<VersionResponse>> getVersions(@PathVariable String docId,
                                                           @RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "20") Integer pageSize,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long documentId = resolveDocumentId(docId);
        PageResult<VersionResponse> result = versionService.getDocumentVersions(documentId, page, pageSize, startTime, endTime);
        return Result.success(result);
    }
    
    /**
     * 获取版本详情
     */
    @GetMapping("/{versionId}")
    public Result<Map<String, Object>> getVersionDetail(@PathVariable String docId,
                                                        @PathVariable Long versionId) {
        Long documentId = resolveDocumentId(docId);
        String content = versionService.getVersionContent(versionId);
        Map<String, Object> data = new HashMap<>();
        data.put("versionId", versionId);
        data.put("documentId", documentId);
        data.put("content", content);
        return Result.success(data);
    }
    
    /**
     * 创建版本快照
     */
    @PostMapping
    public Result<DocumentVersion> createVersion(HttpServletRequest request,
                                                 @PathVariable String docId,
                                                 @Valid @RequestBody VersionCreateRequest createRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        Long documentId = resolveDocumentId(docId);
        DocumentVersion version = versionService.createVersionSnapshot(
                documentId, "", createRequest.getDescription(), userId);
        return Result.success(version);
    }
    
    /**
     * 回滚到指定版本（旧接口）
     */
    @PostMapping("/{versionId}/rollback")
    public Result<Void> rollbackToVersion(@PathVariable String docId,
                                          @PathVariable Long versionId) {
        Long documentId = resolveDocumentId(docId);
        versionService.rollbackToVersion(documentId, versionId);
        return Result.successVoid("回滚成功");
    }
    
    /**
     * 恢复到指定版本
     */
    @PostMapping("/{versionId}/restore")
    public Result<Void> restoreVersion(@PathVariable String docId,
                                       @PathVariable Long versionId) {
        Long documentId = resolveDocumentId(docId);
        versionService.restoreVersion(documentId, versionId);
        return Result.successVoid("恢复成功");
    }
    
    /**
     * 对比版本差异
     */
    @GetMapping("/compare")
    public Result<VersionCompareResponse> compareVersions(@PathVariable String docId,
                                                          @RequestParam Integer versionA,
                                                          @RequestParam Integer versionB) {
        Long documentId = resolveDocumentId(docId);
        VersionCompareResponse response = versionService.compareVersions(documentId, versionA, versionB);
        return Result.success(response);
    }
    
    /**
     * 删除版本
     */
    @DeleteMapping("/{versionId}")
    public Result<Void> deleteVersion(@PathVariable String docId,
                                      @PathVariable Long versionId) {
        versionService.deleteVersion(versionId);
        return Result.successVoid("删除成功");
    }
}
