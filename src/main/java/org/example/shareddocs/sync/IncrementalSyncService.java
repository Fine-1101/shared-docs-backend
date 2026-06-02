package org.example.shareddocs.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.collaboration.Operation;
import org.example.shareddocs.dto.websocket.SyncResponseData;
import org.example.shareddocs.entity.DocumentOperation;
import org.example.shareddocs.mapper.DocumentOperationMapper;
import org.example.shareddocs.service.DocumentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncrementalSyncService {
    
    private final DocumentService documentService;
    private final DocumentOperationMapper operationMapper;
    
    /**
     * 获取增量同步数据
     */
    public SyncResponseData getIncrementalSync(Long documentId, Integer clientVersion) {
        // 如果客户端版本为null或0，返回全量同步
        if (clientVersion == null || clientVersion == 0) {
            // 从数据库获取完整文档内容
            String fullContent = documentService.getDocumentContent(documentId);
            
            // 获取当前最新版本号
            LambdaQueryWrapper<DocumentOperation> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.eq(DocumentOperation::getDocumentId, documentId)
                    .orderByDesc(DocumentOperation::getVersionAfter)
                    .last("LIMIT 1");
            DocumentOperation latestOp = operationMapper.selectOne(versionWrapper);
            int currentVersion = latestOp != null ? latestOp.getVersionAfter() : 0;
            
            return SyncResponseData.builder()
                    .currentVersion(currentVersion)
                    .currentContent(fullContent != null ? fullContent : "")
                    .missedOperations(new ArrayList<>())
                    .acceptedOperations(new ArrayList<>())
                    .rejectedOperations(new ArrayList<>())
                    .onlineUsers(new ArrayList<>())
                    .build();
        }
        
        // 从数据库查询错过的操作
        LambdaQueryWrapper<DocumentOperation> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(DocumentOperation::getDocumentId, documentId)
                .gt(DocumentOperation::getVersionAfter, clientVersion)
                .orderByAsc(DocumentOperation::getVersionAfter);
        List<DocumentOperation> missedOps = operationMapper.selectList(opWrapper);
        
        log.debug("文档 {} 从版本 {} 开始有 {} 个错过的操作", documentId, clientVersion, missedOps.size());
        
        // 转换为MissedOperation
        List<SyncResponseData.MissedOperation> missedOpList = missedOps.stream()
                .map(this::convertToMissedOperation)
                .collect(Collectors.toList());
        
        // 获取当前最新版本号
        int currentVersion = clientVersion + missedOps.size();
        
        return SyncResponseData.builder()
                .currentVersion(currentVersion)
                .missedOperations(missedOpList)
                .acceptedOperations(new ArrayList<>())
                .rejectedOperations(new ArrayList<>())
                .onlineUsers(new ArrayList<>())
                .build();
    }
    
    /**
     * 清理历史操作（保留最近N条）
     */
    public void cleanupHistory(Long documentId, int keepCount) {
        // 从数据库删除旧的操作记录
        LambdaQueryWrapper<DocumentOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentOperation::getDocumentId, documentId)
                .orderByDesc(DocumentOperation::getVersionAfter)
                .last("LIMIT " + keepCount + ", 18446744073709551615"); // MySQL的大数表示无限
        
        List<DocumentOperation> oldOps = operationMapper.selectList(wrapper);
        if (!oldOps.isEmpty()) {
            List<Long> idsToDelete = oldOps.stream()
                    .map(DocumentOperation::getId)
                    .collect(Collectors.toList());
            operationMapper.deleteBatchIds(idsToDelete);
            log.info("清理历史操作: documentId={}, 删除{}条记录", documentId, idsToDelete.size());
        }
    }
    
    /**
     * 将DocumentOperation转换为MissedOperation
     */
    private SyncResponseData.MissedOperation convertToMissedOperation(DocumentOperation operation) {
        return SyncResponseData.MissedOperation.builder()
                .operationId(operation.getClientId())
                .type(operation.getOperationType())
                .position(operation.getPosition())
                .length(operation.getLength())
                .content(operation.getContent())
                .userId(operation.getUserId())
                .version(operation.getVersionAfter())
                .timestamp(operation.getCreatedAt() != null ? 
                        operation.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                        System.currentTimeMillis())
                .build();
    }
}
