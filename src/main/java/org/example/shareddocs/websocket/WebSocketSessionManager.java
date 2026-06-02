package org.example.shareddocs.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 */
@Slf4j
@Component
public class WebSocketSessionManager {
    
    /**
     * 所有活跃的WebSocket会话
     * key: sessionId, value: WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * 文档ID到会话集合的映射
     * key: documentId, value: sessionId集合
     */
    private final Map<Long, Set<String>> documentSessions = new ConcurrentHashMap<>();
    
    /**
     * 会话ID到用户ID的映射
     * key: sessionId, value: userId
     */
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 用户ID到会话ID集合的映射
     * key: userId, value: sessionId集合
     */
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    /**
     * 添加会话
     */
    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("添加会话: {}, 当前会话数: {}", session.getId(), sessions.size());
    }
    
    /**
     * 添加会话并关联用户
     */
    public void addSession(WebSocketSession session, Long userId) {
        sessions.put(session.getId(), session);
        sessionUserMap.put(session.getId(), userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                   .add(session.getId());
        log.info("添加会话: {}, 用户: {}, 当前会话数: {}", session.getId(), userId, sessions.size());
    }
    
    /**
     * 移除会话
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        // 从用户会话映射中移除
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> userSessIds = userSessions.get(userId);
            if (userSessIds != null) {
                userSessIds.remove(sessionId);
                if (userSessIds.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        
        // 从所有文档会话中移除
        documentSessions.forEach((docId, sessionIds) -> sessionIds.remove(sessionId));
        
        log.info("移除会话: {}, 用户: {}, 剩余会话数: {}", sessionId, userId, sessions.size());
    }
    
    /**
     * 将会话加入文档
     */
    public void addSessionToDocument(Long documentId, String sessionId, Long userId) {
        documentSessions.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet())
                       .add(sessionId);
        sessionUserMap.put(sessionId, userId);
        log.info("会话 {} (用户{}) 加入文档 {}", sessionId, userId, documentId);
    }
    
    /**
     * 从文档中移除会话
     */
    public void removeSessionFromDocument(Long documentId, String sessionId) {
        Set<String> sessionIds = documentSessions.get(documentId);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                documentSessions.remove(documentId);
            }
            log.info("会话 {} 离开文档 {}", sessionId, documentId);
        }
    }
    
    /**
     * 获取文档的所有会话
     */
    public Set<String> getDocumentSessions(Long documentId) {
        return documentSessions.getOrDefault(documentId, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * 获取文档的会话数量
     */
    public int getDocumentSessionCount(Long documentId) {
        return getDocumentSessions(documentId).size();
    }
    
    /**
     * 获取会话
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 根据用户ID获取会话
     */
    public Set<String> getUserSessions(Long userId) {
        return userSessions.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * 获取会话对应的用户ID
     */
    public Long getUserIdBySession(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
    
    /**
     * 获取文档在线用户数
     */
    public int getDocumentOnlineCount(Long documentId) {
        return getDocumentSessions(documentId).size();
    }
    
    /**
     * 获取文档的在线用户ID列表
     */
    public Set<Long> getDocumentOnlineUsers(Long documentId) {
        Set<String> sessionIds = getDocumentSessions(documentId);
        return sessionIds.stream()
                .map(sessionUserMap::get)
                .filter(userId -> userId != null)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * 移除用户的所有会话
     */
    public void removeUserSessions(Long userId) {
        Set<String> userSessIds = userSessions.get(userId);
        if (userSessIds != null) {
            // 复制一份，避免并发修改异常
            Set<String> sessionIdsCopy = ConcurrentHashMap.newKeySet();
            sessionIdsCopy.addAll(userSessIds);
            
            for (String sessionId : sessionIdsCopy) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.error("关闭会话失败: {}", sessionId, e);
                    }
                }
                removeSession(session != null ? session : null);
            }
            
            log.info("已移除用户 {} 的所有 {} 个会话", userId, sessionIdsCopy.size());
        }
    }
}
