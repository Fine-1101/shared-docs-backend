package org.example.shareddocs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.request.UpdateDocumentRequest;
import org.example.shareddocs.dto.response.DocumentResponse;
import org.example.shareddocs.entity.Document;

import java.util.List;

public interface DocumentService extends IService<Document> {
    
    /**
     * 创建文档
     */
    Document createDocument(Long userId, String title, String content);
    
    /**
     * 获取文档内容
     */
    String getDocumentContent(Long documentId);
    
    /**
     * 更新文档
     */
    void updateDocument(Long documentId, UpdateDocumentRequest request);
    
    /**
     * 删除文档
     */
    void deleteDocument(Long documentId);
    
    /**
     * 获取文档详情
     */
    DocumentResponse getDocumentDetail(Long documentId);
    
    /**
     * 获取用户文档列表（旧版本，不分页）
     */
    List<DocumentResponse> getUserDocuments(Long userId);
    
    /**
     * 获取用户文档列表（支持分页、搜索、排序）
     */
    PageResult<DocumentResponse> getUserDocumentsWithPage(Long userId, Integer page, Integer pageSize, 
                                                          String keyword, String sortBy, String sortOrder);
}
