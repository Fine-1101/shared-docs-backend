package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.exception.NotFoundException;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.request.UpdateDocumentRequest;
import org.example.shareddocs.dto.response.DocumentResponse;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentOperation;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.mapper.DocumentOperationMapper;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.service.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {
    
    private final UserMapper userMapper;
    private final DocumentOperationMapper documentOperationMapper;
    
    @Override
    @Transactional
    public Document createDocument(Long userId, String title, String content) {
        Document document = new Document();
        document.setDocUuid(UUID.randomUUID().toString().replace("-", ""));
        document.setTitle(title != null ? title : "未命名文档");
        document.setContent(content != null ? content : "");
        document.setCreatorId(userId);
        document.setIsDeleted(0);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        save(document);
        return document;
    }
    
    @Override
    public String getDocumentContent(Long documentId) {
        Document document = getById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new NotFoundException("文档不存在");
        }
        return document.getContent();
    }
    
    @Override
    @Transactional
    public DocumentResponse updateDocument(Long documentId, UpdateDocumentRequest request) {
        Document document = getById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new NotFoundException("文档不存在");
        }
        
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        document.setUpdatedAt(LocalDateTime.now());
        updateById(document);
        
        // 返回更新后的文档信息
        User creator = userMapper.selectById(document.getCreatorId());
        String creatorName = creator != null ? creator.getUsername() : null;
        
        return DocumentResponse.builder()
                .id(document.getId())
                .docUuid(document.getDocUuid())
                .title(document.getTitle())
                .creatorId(document.getCreatorId())
                .creatorName(creatorName)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
    
    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = getById(documentId);
        if (document == null) {
            throw new NotFoundException("文档不存在");
        }
        
        document.setIsDeleted(1);
        document.setUpdatedAt(LocalDateTime.now());
        updateById(document);
    }
    
    @Override
    public DocumentResponse getDocumentDetail(Long documentId) {
        Document document = getById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new NotFoundException("文档不存在");
        }
        
        // 查询创建者信息
        User creator = userMapper.selectById(document.getCreatorId());
        String creatorName = creator != null ? creator.getUsername() : null;
        
        return DocumentResponse.builder()
                .id(document.getId())
                .docUuid(document.getDocUuid())
                .title(document.getTitle())
                .content(document.getContent())
                .creatorId(document.getCreatorId())
                .creatorName(creatorName)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
    
    @Override
    public List<DocumentResponse> getUserDocuments(Long userId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getCreatorId, userId)
               .eq(Document::getIsDeleted, 0)
               .orderByDesc(Document::getUpdatedAt);
        
        List<Document> documents = list(wrapper);
        
        // 批量查询用户信息（这里只有一个用户，就是当前用户）
        User currentUser = userMapper.selectById(userId);
        String creatorName = currentUser != null ? currentUser.getUsername() : null;
        
        return documents.stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .docUuid(doc.getDocUuid())
                        .title(doc.getTitle())
                        .creatorId(doc.getCreatorId())
                        .creatorName(creatorName)
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResult<DocumentResponse> getUserDocumentsWithPage(Long userId, Integer page, Integer pageSize,
                                                                 String keyword, String sortBy, String sortOrder) {
        // 1. 获取用户有权限访问的所有文档ID
        //    - 用户自己创建的文档
        //    - 用户编辑过的文档（通过UUID访问并编辑）
        List<Long> accessibleDocIds = getAccessibleDocumentIds(userId);
        
        if (accessibleDocIds.isEmpty()) {
            // 没有任何文档，返回空结果
            return new PageResult<>(0L, page, pageSize, List.of());
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Document::getId, accessibleDocIds)
               .eq(Document::getIsDeleted, 0);
        
        // 关键词搜索（标题）
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Document::getTitle, keyword);
        }
        
        // 排序
        if ("asc".equalsIgnoreCase(sortOrder)) {
            if ("createdAt".equals(sortBy)) {
                wrapper.orderByAsc(Document::getCreatedAt);
            } else if ("title".equals(sortBy)) {
                wrapper.orderByAsc(Document::getTitle);
            } else {
                wrapper.orderByAsc(Document::getUpdatedAt);
            }
        } else {
            if ("createdAt".equals(sortBy)) {
                wrapper.orderByDesc(Document::getCreatedAt);
            } else if ("title".equals(sortBy)) {
                wrapper.orderByDesc(Document::getTitle);
            } else {
                wrapper.orderByDesc(Document::getUpdatedAt);
            }
        }
        
        // 3. 分页查询
        Page<Document> pageParam = new Page<>(page, pageSize);
        Page<Document> resultPage = page(pageParam, wrapper);
        
        // 4. 获取所有创建者ID
        List<Long> creatorIds = resultPage.getRecords().stream()
                .map(Document::getCreatorId)
                .distinct()
                .collect(Collectors.toList());
        
        // 5. 批量查询用户信息
        Map<Long, User> userMap = Map.of();
        if (!creatorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(creatorIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        }
        
        // 6. 转换为响应DTO，并计算role字段
        final Map<Long, User> finalUserMap = userMap;
        final Long currentUserId = userId;
        List<DocumentResponse> documentList = resultPage.getRecords().stream()
                .map(doc -> {
                    User creator = finalUserMap.get(doc.getCreatorId());
                    String creatorName = creator != null ? creator.getUsername() : null;
                    
                    // 计算role字段：如果当前用户是创建者，则role为'owner'，否则为null
                    String role = doc.getCreatorId().equals(currentUserId) ? "owner" : null;

                    return DocumentResponse.builder()
                            .id(doc.getId())
                            .docUuid(doc.getDocUuid())
                            .title(doc.getTitle())
                            .creatorId(doc.getCreatorId())
                            .creatorName(creatorName)
                            .createdAt(doc.getCreatedAt())
                            .updatedAt(doc.getUpdatedAt())
                            .role(role)
                            .build();
                })
                .collect(Collectors.toList());
        
        return new PageResult<>(resultPage.getTotal(), page, pageSize, documentList);
    }
    
    /**
     * 获取用户有权限访问的所有文档ID
     * 包括：1. 用户自己创建的文档  2. 用户编辑过的文档（通过UUID访问）
     */
    private List<Long> getAccessibleDocumentIds(Long userId) {
        // 1. 查询用户自己创建的文档
        LambdaQueryWrapper<Document> myDocsWrapper = new LambdaQueryWrapper<>();
        myDocsWrapper.eq(Document::getCreatorId, userId)
                     .eq(Document::getIsDeleted, 0)
                     .select(Document::getId);
        List<Document> myDocs = list(myDocsWrapper);
        List<Long> myDocIds = myDocs.stream()
                .map(Document::getId)
                .collect(Collectors.toList());
        
        // 2. 查询用户编辑过的文档（通过 document_operations 表）
        LambdaQueryWrapper<DocumentOperation> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(DocumentOperation::getUserId, userId)
                 .select(DocumentOperation::getDocumentId);
        List<DocumentOperation> operations = documentOperationMapper.selectList(opWrapper);
        List<Long> editedDocIds = operations.stream()
                .map(DocumentOperation::getDocumentId)
                .distinct()
                .collect(Collectors.toList());
        
        // 3. 合并去重
        List<Long> allDocIds = new java.util.ArrayList<>();
        allDocIds.addAll(myDocIds);
        allDocIds.addAll(editedDocIds);
        
        return allDocIds.stream().distinct().collect(Collectors.toList());
    }
}
