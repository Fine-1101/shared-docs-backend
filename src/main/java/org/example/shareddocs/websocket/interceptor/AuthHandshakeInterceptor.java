package org.example.shareddocs.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.utils.JwtUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器 - 用于认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtUtils jwtUtils;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.info("🔥🔥🔥 WebSocket握手请求: URI={}, Method={}", 
                request.getURI(), request.getMethod());
        
        // 从请求参数中获取token
        String token = extractToken(request);
        
        log.info("🔑 Token提取结果: {}", token != null ? "存在" : "null");
        
        if (token == null || token.isEmpty()) {
            log.warn("❌ WebSocket握手失败：缺少token");
            return false;
        }
        
        try {
            // 验证token
            if (!jwtUtils.validateToken(token)) {
                log.warn("❌ WebSocket握手失败：token无效");
                return false;
            }
            
            // 将用户信息存入attributes，供后续使用
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("token", token);
            
            log.info("✅ WebSocket握手成功：userId={}, username={}", userId, username);
            return true;
            
        } catch (Exception e) {
            log.error("❌ WebSocket握手异常：{}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理，可以留空
    }
    
    /**
     * 从请求中提取token
     */
    private String extractToken(ServerHttpRequest request) {
        // 从URL参数中获取token
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        
        // 也可以从Header中获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
}
