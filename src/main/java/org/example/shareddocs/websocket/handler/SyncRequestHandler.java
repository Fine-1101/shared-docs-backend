package org.example.shareddocs.websocket.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.SyncRequestData;
import org.example.shareddocs.dto.websocket.SyncResponseData;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentOperation;
import org.example.shareddocs.entity.DocumentVersion;
import org.example.shareddocs.entity.UserSession;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.mapper.DocumentOperationMapper;
import org.example.shareddocs.mapper.DocumentVersionMapper;
import org.example.shareddocs.mapper.UserSessionMapper;
import org.example.shareddocs.service.UserService;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 同步请求消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRequestHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final DocumentMapper documentMapper;
    private final DocumentOperationMapper operationMapper;
    private final DocumentVersionMapper versionMapper;
    private final UserSessionMapper userSessionMapper;
    private final UserService userService;
    
    @Override
    @Transactional
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        Long documentId = Long.valueOf(message.getDocumentId());
        SyncRequestData syncRequest = (SyncRequestData) message.getData();
            
        log.info("收到用户 {} 的同步请求，最后同步版本：{}", userId, syncRequest.getLastSyncedVersion());
            
        // 1. 获取文档当前内容和版本号
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("文档不存在：{}", documentId);
            return;
        }
            
        // 从版本表获取当前版本号
        LambdaQueryWrapper<DocumentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(DocumentVersion::getDocumentId, documentId)
                     .orderByDesc(DocumentVersion::getVersionNumber)
                     .last("LIMIT 1");
        DocumentVersion latestVersion = versionMapper.selectOne(versionWrapper);
        Integer currentVersion = latestVersion != null ? latestVersion.getVersionNumber() : 0;
        String currentContent = document.getContent() != null ? document.getContent() : "";
            
        // 2. 获取错过的操作列表（服务端有但客户端没有的操作）
        List<SyncResponseData.MissedOperation> missedOperations = getMissedOperations(
                documentId, syncRequest.getLastSyncedVersion());
            
        // 3. 处理离线期间的pending operations
        List<String> acceptedOperations = new ArrayList<>();
        List<SyncResponseData.RejectedOperation> rejectedOperations = new ArrayList<>();
            
        if (syncRequest.getPendingOperations() != null && !syncRequest.getPendingOperations().isEmpty()) {
            // 简化实现：接受所有操作并应用到文档
            for (var op : syncRequest.getPendingOperations()) {
                try {
                    // 应用操作到文档内容
                    String updatedContent = applyOperationToContent(currentContent, op);
                    currentContent = updatedContent;
                        
                    // 保存操作记录
                    DocumentOperation operation = new DocumentOperation();
                    operation.setDocumentId(documentId);
                    operation.setUserId(userId);
                    operation.setOperationType(op.getType());
                    operation.setPosition(op.getPosition());
                    operation.setLength(op.getLength());
                    operation.setContent(op.getContent());
                    operation.setVersionBefore(currentVersion);
                    operation.setVersionAfter(currentVersion + 1);
                    operation.setClientId(op.getOperationId());
                    operation.setCreatedAt(LocalDateTime.now());
                    operationMapper.insert(operation);
                        
                    acceptedOperations.add(op.getOperationId());
                    currentVersion++;
                        
                    log.debug("接受离线操作: operationId={}", op.getOperationId());
                } catch (Exception e) {
                    // 冲突或错误，拒绝该操作
                    rejectedOperations.add(SyncResponseData.RejectedOperation.builder()
                            .operationId(op.getOperationId())
                            .reason("操作应用失败: " + e.getMessage())
                            .build());
                    log.warn("拒绝离线操作: operationId={}, reason={}", op.getOperationId(), e.getMessage());
                }
            }
                
            // 更新文档内容
            if (!acceptedOperations.isEmpty()) {
                document.setContent(currentContent);
                documentMapper.updateById(document);
            }
        }
            
        // 4. 获取当前在线用户列表
        List<SyncResponseData.OnlineUser> onlineUsers = getOnlineUsers(documentId);
            
        // 构建同步响应
        SyncResponseData syncResponse = SyncResponseData.builder()
                .currentVersion(currentVersion)
                .currentContent(missedOperations.isEmpty() && acceptedOperations.isEmpty() ? null : currentContent)
                .missedOperations(missedOperations)
                .acceptedOperations(acceptedOperations)
                .rejectedOperations(rejectedOperations)
                .onlineUsers(onlineUsers)
                .build();
            
        WebSocketMessage responseMessage = messageSender.createMessage(
                MessageType.SYNC_RESPONSE, documentId, userId, syncResponse);
            
        messageSender.sendMessage(session, responseMessage);
        log.info("同步响应已发送给用户 {}，错过操作数：{}, 接受操作数：{}", 
                userId, missedOperations.size(), acceptedOperations.size());
    }
    
    /**
     * 获取错过的操作列表
     */
    private List<SyncResponseData.MissedOperation> getMissedOperations(Long documentId, Integer lastSyncedVersion) {
        if (lastSyncedVersion == null) {
            // 首次同步，返回空列表（客户端需要全量内容）
            return Collections.emptyList();
        }
        
        // 查询版本号大于lastSyncedVersion的操作
        LambdaQueryWrapper<DocumentOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentOperation::getDocumentId, documentId)
               .gt(DocumentOperation::getVersionAfter, lastSyncedVersion)
               .orderByAsc(DocumentOperation::getCreatedAt);
        
        List<DocumentOperation> operations = operationMapper.selectList(wrapper);
        
        return operations.stream()
                .map(op -> SyncResponseData.MissedOperation.builder()
                        .operationId(op.getId().toString())
                        .type(op.getOperationType())
                        .position(op.getPosition())
                        .content(op.getContent())
                        .length(op.getLength())
                        .userId(op.getUserId())
                        .version(op.getVersionAfter())
                        .timestamp(op.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取在线用户列表
     */
    private List<SyncResponseData.OnlineUser> getOnlineUsers(Long documentId) {
        // 从 user_sessions 表查询在线用户
        LambdaQueryWrapper<UserSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSession::getDocumentId, documentId)
               .eq(UserSession::getStatus, "online");
        
        List<UserSession> sessions = userSessionMapper.selectList(wrapper);
        
        return sessions.stream()
                .map(session -> {
                    try {
                        var userInfo = userService.getUserInfo(session.getUserId());
                        return SyncResponseData.OnlineUser.builder()
                                .userId(session.getUserId())
                                .nickname(userInfo.getNickname())
                                .avatarUrl(userInfo.getAvatarUrl())
                                .cursorPosition(session.getCursorPosition())
                                .build();
                    } catch (Exception e) {
                        log.warn("获取用户信息失败: userId={}", session.getUserId());
                        return null;
                    }
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 应用操作到文档内容
     */
    private String applyOperationToContent(String content, SyncRequestData.PendingOperation op) {
        if (content == null) {
            content = "";
        }
        
        switch (op.getType()) {
            case "insert":
                int insertPos = Math.min(op.getPosition(), content.length());
                return content.substring(0, insertPos) + op.getContent() + content.substring(insertPos);
                
            case "delete":
                int deleteStart = Math.min(op.getPosition(), content.length());
                int deleteEnd = Math.min(deleteStart + (op.getLength() != null ? op.getLength() : 0), content.length());
                return content.substring(0, deleteStart) + content.substring(deleteEnd);
                
            case "replace":
                int replaceStart = Math.min(op.getPosition(), content.length());
                int replaceEnd = Math.min(replaceStart + (op.getLength() != null ? op.getLength() : 0), content.length());
                return content.substring(0, replaceStart) + op.getContent() + content.substring(replaceEnd);
                
            default:
                log.warn("未知操作类型：{}", op.getType());
                return content;
        }
    }
    
    @Override
    public String getSupportedMessageType() {
        return "SYNC_REQUEST";
    }
}
