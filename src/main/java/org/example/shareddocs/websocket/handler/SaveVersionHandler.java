package org.example.shareddocs.websocket.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.enums.MessageType;
import org.example.shareddocs.dto.websocket.WebSocketMessage;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.service.VersionService;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

/**
 * 保存版本消息处理器
 * 用户点击保存按钮时触发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaveVersionHandler implements MessageHandler {
    
    private final WebSocketMessageSender messageSender;
    private final DocumentMapper documentMapper;
    private final VersionService versionService;
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        Long userId = message.getUserId();
        String documentIdStr = message.getDocumentId();
        
        // 验证文档ID
        if (documentIdStr == null || documentIdStr.isEmpty() || 
            "undefined".equalsIgnoreCase(documentIdStr) || 
            "null".equalsIgnoreCase(documentIdStr)) {
            log.warn("无效的文档ID: {}", documentIdStr);
            messageSender.sendError(session, 400, "无效的文档ID");
            return;
        }
        
        Long documentId;
        try {
            documentId = Long.valueOf(documentIdStr);
        } catch (NumberFormatException e) {
            log.warn("文档ID格式错误: {}", documentIdStr);
            messageSender.sendError(session, 400, "文档ID格式错误");
            return;
        }
        
        log.info(" 用户 {} 手动保存文档 {} 的版本", userId, documentId);
        
        try {
            // 获取当前文档
            Document document = documentMapper.selectById(documentId);
            if (document == null) {
                messageSender.sendError(session, 404, "文档不存在");
                return;
            }
            
            // 创建版本快照（内部会检查内容是否变化）
            var version = versionService.createVersionSnapshot(
                    documentId,
                    document.getContent(),
                    "用户手动保存",
                    userId
            );
            
            // 更新文档的 updatedAt 字段
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            
            if (version == null) {
                // 内容未变化，没有创建新版本
                messageSender.sendMessage(session, messageSender.createMessage(
                        MessageType.SYSTEM_NOTIFICATION,
                        documentId,
                        userId,
                        "内容未变化，未创建新版本"
                ));
                log.info("文档 {} 内容未变化，跳过版本创建", documentId);
            } else {
                // 成功创建版本
                messageSender.sendMessage(session, messageSender.createMessage(
                        MessageType.SYSTEM_NOTIFICATION,
                        documentId,
                        userId,
                        "版本 " + version.getVersionNumber() + " 已保存"
                ));
                log.info("用户 {} 成功保存文档 {} 的版本 {}", userId, documentId, version.getVersionNumber());
            }
            
        } catch (Exception e) {
            log.error("保存版本失败: documentId={}, userId={}, error={}",
                    documentId, userId, e.getMessage(), e);
            messageSender.sendError(session, 500, "保存版本失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getSupportedMessageType() {
        return "SAVE_VERSION";
    }
}
