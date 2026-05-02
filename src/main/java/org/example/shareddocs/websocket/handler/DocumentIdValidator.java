package org.example.shareddocs.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.websocket.WebSocketMessageSender;
import org.springframework.web.socket.WebSocketSession;

/**
 * 文档ID验证工具类
 */
@Slf4j
public class DocumentIdValidator {
    
    /**
     * 验证文档ID是否有效
     * 
     * @param documentId 文档ID字符串
     * @return 是否有效
     */
    public static boolean isValid(String documentId) {
        return documentId != null && 
               !documentId.trim().isEmpty() && 
               !"undefined".equalsIgnoreCase(documentId) && 
               !"null".equalsIgnoreCase(documentId);
    }
    
    /**
     * 验证文档ID，如果无效则发送错误消息
     * 
     * @param session WebSocket会话
     * @param documentId 文档ID字符串
     * @param messageSender 消息发送器
     * @return 是否有效
     */
    public static boolean validateAndSendError(WebSocketSession session, 
                                                String documentId,
                                                WebSocketMessageSender messageSender) {
        if (!isValid(documentId)) {
            log.warn("⚠️ 无效的文档ID: {}", documentId);
            messageSender.sendError(session, 400, "无效的文档ID");
            return false;
        }
        return true;
    }
    
    /**
     * 尝试将文档ID字符串转换为Long
     * 
     * @param documentId 文档ID字符串
     * @return Long类型的文档ID，如果转换失败返回null
     */
    public static Long parseToLong(String documentId) {
        if (!isValid(documentId)) {
            return null;
        }
        
        try {
            return Long.parseLong(documentId);
        } catch (NumberFormatException e) {
            log.debug("文档ID不是数字格式: {}", documentId);
            return null;
        }
    }
}
