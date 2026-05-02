package org.example.shareddocs.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.collaboration.Operation;
import org.example.shareddocs.dto.websocket.SyncResponseData;
import org.example.shareddocs.service.DocumentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 增量同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncrementalSyncService {
    
    private final DocumentService documentService;
    
    /**
     * 文档操作历史记录
     * key: documentId, value: 操作列表
     */
    private final Map<Long, List<Operation>> operationHistory = new ConcurrentHashMap<>();
    
    /**
     * 获取增量同步数据
     */
    public SyncResponseData getIncrementalSync(Long documentId, Integer clientVersion) {
        List<Operation> operations = operationHistory.getOrDefault(documentId, new ArrayList<>());
        
        // 如果客户端版本为null或0，返回全量同步
        if (clientVersion == null || clientVersion == 0) {
            // 从数据库获取完整文档内容
            String fullContent = documentService.getDocumentContent(documentId);
            
            return SyncResponseData.builder()
                    .currentVersion(operations.size())
                    .currentContent(fullContent != null ? fullContent : "")
                    .missedOperations(new ArrayList<>())
                    .acceptedOperations(new ArrayList<>())
                    .rejectedOperations(new ArrayList<>())
                    .onlineUsers(new ArrayList<>())
                    .build();
        }
        
        // 获取客户端缺失的操作
        List<Operation> missingOperations = new ArrayList<>();
        if (clientVersion < operations.size()) {
            missingOperations = operations.subList(clientVersion, operations.size());
        }
        
        // 转换为MissedOperation
        List<SyncResponseData.MissedOperation> missedOpList = missingOperations.stream()
                .map(this::convertToMissedOperation)
                .collect(Collectors.toList());
        
        return SyncResponseData.builder()
                .currentVersion(operations.size())
                .missedOperations(missedOpList)
                .acceptedOperations(new ArrayList<>())
                .rejectedOperations(new ArrayList<>())
                .onlineUsers(new ArrayList<>())
                .build();
    }
    
    /**
     * 记录操作
     */
    public void recordOperation(Long documentId, Operation operation) {
        operationHistory.computeIfAbsent(documentId, k -> new ArrayList<>())
                       .add(operation);
        
        log.debug("记录操作: documentId={}, version={}", documentId, 
                 operationHistory.get(documentId).size());
    }
    
    /**
     * 清理历史操作（保留最近N条）
     */
    public void cleanupHistory(Long documentId, int keepCount) {
        List<Operation> operations = operationHistory.get(documentId);
        if (operations != null && operations.size() > keepCount) {
            int removeCount = operations.size() - keepCount;
            operations.subList(0, removeCount).clear();
            log.debug("清理历史操作: documentId={}, 保留{}条", documentId, keepCount);
        }
    }
    
    /**
     * 将Operation转换为MissedOperation
     */
    private SyncResponseData.MissedOperation convertToMissedOperation(Operation operation) {
        return SyncResponseData.MissedOperation.builder()
                .operationId(operation.getClientId()) // TODO: 使用实际的operationId
                .type(operation.getType() != null ? operation.getType().name() : null)
                .position(operation.getPosition())
                .length(operation.getLength())
                .content(operation.getContent())
                .userId(null) // TODO: 从operation中获取userId
                .version(operation.getVersion())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
