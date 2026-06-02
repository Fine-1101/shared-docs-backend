package org.example.shareddocs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.request.CreateDocumentRequest;
import org.example.shareddocs.dto.request.DocumentExportRequest;
import org.example.shareddocs.dto.request.UpdateDocumentRequest;
import org.example.shareddocs.dto.response.DocumentResponse;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentVersion;
import org.example.shareddocs.mapper.DocumentVersionMapper;
import org.example.shareddocs.service.DocumentExportService;
import org.example.shareddocs.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentExportService documentExportService;
    
    /**
     * 创建文档
     */
    @PostMapping
    public Result<Document> createDocument(HttpServletRequest request,
                                           @Valid @RequestBody CreateDocumentRequest createRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        Document document = documentService.createDocument(userId, createRequest.getTitle(), 
                                                          createRequest.getContent());
        return Result.success(document);
    }
    
    /**
     * 获取文档列表（支持分页、搜索、排序）
     */
    @GetMapping
    public Result<PageResult<DocumentResponse>> getDocuments(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") Integer page,
                                                              @RequestParam(defaultValue = "20") Integer pageSize,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                              @RequestParam(defaultValue = "desc") String sortOrder) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        PageResult<DocumentResponse> result = documentService.getUserDocumentsWithPage(userId, page, pageSize, keyword, sortBy, sortOrder);
        return Result.success(result);
    }
    
    /**
     * 获取文档详情（支持ID和UUID）
     */
    @GetMapping("/{docId}")
    public Result<DocumentResponse> getDocumentDetail(@PathVariable String docId) {
        DocumentResponse document;
        
        // 判断是数字ID还是UUID
        try {
            Long id = Long.parseLong(docId);
            document = documentService.getDocumentDetail(id);
        } catch (NumberFormatException e) {
            // 按UUID查询
            Document docByUuid = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
            if (docByUuid == null) {
                return Result.error(404, "文档不存在");
            }
            document = documentService.getDocumentDetail(docByUuid.getId());
        }
        
        return Result.success(document);
    }
    
    /**
     * 获取文档内容（支持ID和UUID）
     */
    @GetMapping("/{docId}/content")
    public Result<Map<String, Object>> getDocumentContent(@PathVariable String docId) {
        Document document;
        
        // 判断是数字ID还是UUID
        try {
            Long id = Long.parseLong(docId);
            document = documentService.getById(id);
        } catch (NumberFormatException e) {
            // 按UUID查询
            document = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
        }
        
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(404, "文档不存在");
        }
        
        // 从版本表获取最新版本号
        LambdaQueryWrapper<DocumentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(DocumentVersion::getDocumentId, document.getId())
                     .orderByDesc(DocumentVersion::getVersionNumber)
                     .last("LIMIT 1");
        DocumentVersion latestVersion = documentVersionMapper.selectOne(versionWrapper);
        Integer currentVersion = latestVersion != null ? latestVersion.getVersionNumber() : 0;
        
        Map<String, Object> data = new HashMap<>();
        data.put("documentId", document.getId());
        data.put("content", document.getContent() != null ? document.getContent() : "");
        data.put("version", currentVersion);
        data.put("hash", document.getContentHash());
        data.put("updatedAt", document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : null);
        return Result.success(data);
    }
    
    /**
     * 更新文档（支持ID和UUID）
     */
    @PutMapping("/{docId}")
    public Result<DocumentResponse> updateDocument(HttpServletRequest request,
                                       @PathVariable String docId,
                                       @Valid @RequestBody UpdateDocumentRequest updateRequest) {
        log.info(" 收到PUT请求 - Method: {}, URI: {}, docId: {}",
                request.getMethod(), request.getRequestURI(), docId);
        log.info("收到文档更新请求: docId={}, title={}, content长度={}", 
                docId, updateRequest.getTitle(), 
                updateRequest.getContent() != null ? updateRequest.getContent().length() : 0);
        
        Long documentId;
        
        // 判断是数字ID还是UUID
        try {
            documentId = Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 按UUID查询
            Document document = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
            if (document == null) {
                log.warn("文档不存在: docId={}", docId);
                return Result.error(404, "文档不存在");
            }
            documentId = document.getId();
            log.info("通过UUID找到文档ID: uuid={}, documentId={}", docId, documentId);
        }
        
        DocumentResponse updatedDoc = documentService.updateDocument(documentId, updateRequest);
        log.info("文档更新成功: documentId={}, updatedAt={}", documentId, updatedDoc.getUpdatedAt());
        return Result.success("更新成功", updatedDoc);
    }
    
    /**
     * 删除文档（支持ID和UUID）
     */
    @DeleteMapping("/{docId}")
    public Result<Void> deleteDocument(@PathVariable String docId) {
        Long documentId;
        
        // 判断是数字ID还是UUID
        try {
            documentId = Long.parseLong(docId);
            // 检查文档是否存在且未删除
            Document document = documentService.getById(documentId);
            if (document == null || document.getIsDeleted() == 1) {
                return Result.error(404, "文档不存在");
            }
        } catch (NumberFormatException e) {
            // 按UUID查询
            Document document = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
            if (document == null) {
                return Result.error(404, "文档不存在");
            }
            documentId = document.getId();
        }
        
        documentService.deleteDocument(documentId);
        return Result.successVoid("删除成功");
    }
    
    /**
     * 通过链接加入文档
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> joinDocument(HttpServletRequest request,
                                                     @RequestBody Map<String, String> requestBody) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        String docUuid = requestBody.get("docUuid");
        if (docUuid == null || docUuid.isEmpty()) {
            return Result.error(400, "文档UUID不能为空");
        }
        
        // 根据UUID查找文档
        Document document = documentService.lambdaQuery()
                .eq(org.example.shareddocs.entity.Document::getDocUuid, docUuid)
                .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                .one();
        
        if (document == null) {
            return Result.error(404, "文档不存在或已被删除");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("documentId", document.getId());
        data.put("docUuid", document.getDocUuid());
        data.put("title", document.getTitle());
        data.put("creatorId", document.getCreatorId());
        
        return Result.success("加入成功", data);
    }
    
    /**
     * 标记文档为共享状态
     */
    @PutMapping("/{docId}/mark-shared")
    public Result<Void> markDocumentAsShared(@PathVariable String docId) {
        log.info("收到标记文档共享请求: docId={}", docId);
        
        Long documentId;
        
        // 判断是数字ID还是UUID
        try {
            documentId = Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 按UUID查询
            Document document = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
            if (document == null) {
                log.warn("文档不存在: docId={}", docId);
                return Result.error(404, "文档不存在");
            }
            documentId = document.getId();
            log.info("通过UUID找到文档ID: uuid={}, documentId={}", docId, documentId);
        }
        
        // 更新文档的is_shared字段
        Document document = documentService.getById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(404, "文档不存在");
        }
        
        document.setIsShared(1);
        documentService.updateById(document);
        
        log.info("文档已标记为共享: documentId={}", documentId);
        return Result.successVoid("标记成功");
    }
    
    /**
     * 导出文档（支持 Markdown 和 HTML 格式）
     */
    @PostMapping("/{docId}/export")
    public ResponseEntity<byte[]> exportDocument(@PathVariable String docId,
                                                  @Valid @RequestBody DocumentExportRequest exportRequest) {
        log.info("收到文档导出请求: docId={}, format={}", docId, exportRequest.getFormat());
        
        Long documentId;
        
        // 判断是数字ID还是UUID
        try {
            documentId = Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 按UUID查询
            Document document = documentService.lambdaQuery()
                    .eq(org.example.shareddocs.entity.Document::getDocUuid, docId)
                    .eq(org.example.shareddocs.entity.Document::getIsDeleted, 0)
                    .one();
            if (document == null) {
                log.warn("文档不存在: docId={}", docId);
                throw new RuntimeException("文档不存在");
            }
            documentId = document.getId();
            log.info("通过UUID找到文档ID: uuid={}, documentId={}", docId, documentId);
        }
        
        // 调用导出服务
        return documentExportService.exportDocument(documentId, exportRequest.getFormat());
    }
}
