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
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.service.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {
    
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
    public void updateDocument(Long documentId, UpdateDocumentRequest request) {
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
        
        return DocumentResponse.builder()
                .id(document.getId())
                .docUuid(document.getDocUuid())
                .title(document.getTitle())
                .content(document.getContent())
                .creatorId(document.getCreatorId())
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
        return documents.stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .docUuid(doc.getDocUuid())
                        .title(doc.getTitle())
                        .creatorId(doc.getCreatorId())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResult<DocumentResponse> getUserDocumentsWithPage(Long userId, Integer page, Integer pageSize,
                                                                 String keyword, String sortBy, String sortOrder) {
        // 构建查询条件
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getCreatorId, userId)
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
        
        // 分页查询
        Page<Document> pageParam = new Page<>(page, pageSize);
        Page<Document> resultPage = page(pageParam, wrapper);
        
        // 转换为响应DTO
        List<DocumentResponse> documentList = resultPage.getRecords().stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .docUuid(doc.getDocUuid())
                        .title(doc.getTitle())
                        .creatorId(doc.getCreatorId())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return new PageResult<>(resultPage.getTotal(), page, pageSize, documentList);
    }
}
