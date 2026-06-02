package org.example.shareddocs.websocket.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.OperationBroadcastData;
import org.example.shareddocs.dto.websocket.OperationData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentOperation;
import org.example.shareddocs.entity.DocumentVersion;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.mapper.DocumentOperationMapper;
import org.example.shareddocs.mapper.DocumentVersionMapper;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 编辑操作消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final DocumentMapper documentMapper;
    private final DocumentOperationMapper operationMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentIdResolver documentIdResolver;
    private final org.example.shareddocs.collaboration.OperationTransform operationTransform;
    
    @Override
    @Transactional
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = documentIdResolver.resolve(message.getDocumentId());
        if (documentId == null) {
            log.error("无效的文档ID: {}", message.getDocumentId());
            messageSender.sendError(session, 400, "无效的文档ID");
            return;
        }
        
        Integer version = message.getVersion();
        OperationData operationData = (OperationData) message.getData();
        
        log.debug("收到用户 {} 的编辑操作：类型={}, 位置={}", userId, operationData.getType(), operationData.getPosition());
        
        // 1. 获取当前文档和版本号
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("文档不存在: {}", documentId);
            return;
        }
        
        // 2. 获取当前最新版本号
        LambdaQueryWrapper<DocumentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(DocumentVersion::getDocumentId, documentId)
                     .orderByDesc(DocumentVersion::getVersionNumber)
                     .last("LIMIT 1");
        DocumentVersion latestVersion = versionMapper.selectOne(versionWrapper);
        int currentVersion = latestVersion != null ? latestVersion.getVersionNumber() : 0;
        
        // 3. OT算法变换（如果版本不匹配，进行变换）
        boolean transformed = false;
        OperationData transformedOp = operationData;
        if (version != null && version < currentVersion) {
            log.info("版本不匹配，客户端版本={}, 服务端版本={}，执行OT变换", version, currentVersion);
            
            // 获取错过的操作并进行变换
            LambdaQueryWrapper<DocumentOperation> opWrapper = new LambdaQueryWrapper<>();
            opWrapper.eq(DocumentOperation::getDocumentId, documentId)
                    .gt(DocumentOperation::getVersionAfter, version)
                    .orderByAsc(DocumentOperation::getVersionAfter);
            List<DocumentOperation> missedOps = operationMapper.selectList(opWrapper);
            
            if (!missedOps.isEmpty()) {
                log.debug("找到{}个错过的操作，开始OT变换", missedOps.size());
                
                // 将OperationData转换为Operation对象
                org.example.shareddocs.collaboration.Operation clientOp = convertToOperation(operationData);
                
                // 依次与每个错过的操作进行变换
                for (DocumentOperation missedOp : missedOps) {
                    org.example.shareddocs.collaboration.Operation serverOp = convertToOperation(missedOp);
                    
                    // 执行OT变换
                    var transformResult = operationTransform.transform(clientOp, serverOp);
                    clientOp = transformResult.getTransformedOp1();
                    
                    log.debug("OT变换后操作: position={}, type={}", 
                             clientOp.getPosition(), clientOp.getType());
                }
                
                // 将变换后的Operation转换回OperationData
                transformedOp = convertToOperationData(clientOp);
                transformed = true;
                log.info("OT变换完成，原位置={}, 新位置={}", 
                        operationData.getPosition(), transformedOp.getPosition());
            }
        }
        
        // 4. 应用操作到文档内容
        String currentContent = document.getContent() != null ? document.getContent() : "";
        String newContent = applyOperation(currentContent, transformedOp);
        document.setContent(newContent);
        document.setUpdatedAt(LocalDateTime.now());  // ← 更新文档修改时间
        
        log.info("准备保存文档内容: documentId={}, 内容长度={}, 标题={}", 
                documentId, newContent.length(), document.getTitle());
        
        int updateResult = documentMapper.updateById(document);
        
        log.info("文档内容保存结果: documentId={}, 更新行数={}, 新内容前50字符={}", 
                documentId, updateResult, 
                newContent.length() > 50 ? newContent.substring(0, 50) : newContent);
        
        // 5. 保存操作记录到数据库
        int newVersion = currentVersion + 1;
        DocumentOperation operation = new DocumentOperation();
        operation.setDocumentId(documentId);
        operation.setUserId(userId);
        operation.setOperationType(transformedOp.getType());
        operation.setPosition(transformedOp.getPosition());
        operation.setLength(transformedOp.getLength());
        operation.setContent(transformedOp.getContent());
        operation.setCursorPosition(transformedOp.getCursorAfter());
        operation.setVersionBefore(currentVersion);
        operation.setVersionAfter(newVersion);
        operation.setClientId(operationData.getOperationId());
        operation.setCreatedAt(LocalDateTime.now());
        operationMapper.insert(operation);
        
        log.debug("操作已保存到数据库，operationId={}, 新版本号={}", operationData.getOperationId(), newVersion);
        
        // 6. 广播操作给其他用户
        OperationBroadcastData broadcastData = OperationBroadcastData.builder()
                .operationId(operationData.getOperationId())
                .type(transformedOp.getType())
                .position(transformedOp.getPosition())
                .content(transformedOp.getContent())
                .length(transformedOp.getLength())
                .transformed(transformed)
                .userId(userId)
                .version(newVersion)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        
        WebSocketMessage broadcastMessage = messageSender.createMessage(
                MessageType.OPERATION_BROADCAST, documentId, userId, broadcastData);
        broadcastMessage.setVersion(newVersion);
        
        messageSender.broadcastToDocument(documentId, broadcastMessage);
        log.debug("编辑操作已广播到文档 {}，新版本号={}", documentId, newVersion);
    }
    
    /**
     * 应用操作到文档内容（简化实现）
     */
    private String applyOperation(String content, OperationData operation) {
        if (content == null) {
            content = "";
        }
        
        switch (operation.getType()) {
            case "insert":
                // 在指定位置插入内容
                int insertPos = Math.min(operation.getPosition(), content.length());
                return content.substring(0, insertPos) + operation.getContent() + content.substring(insertPos);
                
            case "delete":
                // 从指定位置删除指定长度
                int deleteStart = Math.min(operation.getPosition(), content.length());
                int deleteEnd = Math.min(deleteStart + (operation.getLength() != null ? operation.getLength() : 0), content.length());
                return content.substring(0, deleteStart) + content.substring(deleteEnd);
                
            case "replace":
                // 替换指定位置的内容
                int replaceStart = Math.min(operation.getPosition(), content.length());
                int replaceEnd = Math.min(replaceStart + (operation.getLength() != null ? operation.getLength() : 0), content.length());
                return content.substring(0, replaceStart) + operation.getContent() + content.substring(replaceEnd);
                
            default:
                log.warn("未知操作类型：{}", operation.getType());
                return content;
        }
    }
    
    /**
     * 将OperationData转换为Operation对象
     */
    private org.example.shareddocs.collaboration.Operation convertToOperation(OperationData data) {
        org.example.shareddocs.collaboration.OperationType type = 
            org.example.shareddocs.collaboration.OperationType.valueOf(data.getType().toUpperCase());
        
        return org.example.shareddocs.collaboration.Operation.builder()
                .type(type)
                .position(data.getPosition())
                .length(data.getLength())
                .content(data.getContent())
                .clientId(data.getOperationId())
                .build();
    }
    
    /**
     * 将DocumentOperation转换为Operation对象
     */
    private org.example.shareddocs.collaboration.Operation convertToOperation(DocumentOperation op) {
        org.example.shareddocs.collaboration.OperationType type = 
            org.example.shareddocs.collaboration.OperationType.valueOf(op.getOperationType().toUpperCase());
        
        return org.example.shareddocs.collaboration.Operation.builder()
                .type(type)
                .position(op.getPosition())
                .length(op.getLength())
                .content(op.getContent())
                .clientId(op.getClientId())
                .version(op.getVersionAfter())
                .build();
    }
    
    /**
     * 将Operation转换为OperationData
     */
    private OperationData convertToOperationData(org.example.shareddocs.collaboration.Operation op) {
        return OperationData.builder()
                .operationId(op.getClientId())
                .type(op.getType().name().toLowerCase())
                .position(op.getPosition())
                .length(op.getLength())
                .content(op.getContent())
                .cursorAfter(null) // 由客户端决定
                .build();
    }
    
    @Override
    public String getSupportedMessageType() {
        return "OPERATION";
    }
}
