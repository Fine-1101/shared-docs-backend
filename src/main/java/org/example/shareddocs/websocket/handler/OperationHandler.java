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
    
    @Override
    @Transactional
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        
        // 解析文档ID（支持Long和UUID）
        Long documentId = documentIdResolver.resolve(message.getDocumentId());
        if (documentId == null) {
            log.error("❌ 无效的文档ID: {}", message.getDocumentId());
            messageSender.sendError(session, 400, "无效的文档ID");
            return;
        }
        
        Integer version = message.getVersion();
        OperationData operationData = (OperationData) message.getData();
        
        log.debug("🔥 收到用户 {} 的编辑操作：类型={}, 位置={}", userId, operationData.getType(), operationData.getPosition());
        
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
        
        // 3. OT算法变换（简化实现：如果版本不匹配，需要变换）
        boolean transformed = false;
        OperationData transformedOp = operationData;
        if (version != null && version < currentVersion) {
            // TODO: 实现完整的OT变换逻辑
            // 这里简化处理，直接应用操作
            log.warn("版本不匹配，客户端版本={}, 服务端版本={}，需要进行OT变换", version, currentVersion);
            transformed = true;
        }
        
        // 4. 应用操作到文档内容
        String currentContent = document.getContent() != null ? document.getContent() : "";
        String newContent = applyOperation(currentContent, transformedOp);
        document.setContent(newContent);
        
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
    
    @Override
    public String getSupportedMessageType() {
        return "OPERATION";
    }
}
