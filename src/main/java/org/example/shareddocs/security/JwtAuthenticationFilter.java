package org.example.shareddocs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.utils.JwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    // 白名单路径 - 这些接口不需要认证
    private static final Set<String> WHITE_LIST = Set.of(
            "/api/users/login",
            "/api/users/register",
            "/api/ws"  // WebSocket 握手请求，由 AuthHandshakeInterceptor 处理
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 🔥 关键:处理 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Handling CORS preflight request for: {}", requestUri);
            setCorsHeaders(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 设置 CORS headers
        setCorsHeaders(response);

        // 白名单路径跳过认证
        if (isWhiteListPath(requestUri)) {
            log.debug("⏭️ White list path, skip authentication: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        log.info("🔥🔥🔥 JwtAuthenticationFilter EXECUTED for URI: {}", requestUri);

        try {
            // 从请求头获取Token
            String token = getTokenFromRequest(request);

            log.info("🔐 JWT过滤器 - URI: {}, Token存在: {}", requestUri, token != null);

            if (token != null) {
                log.info("🔑 Token前缀: {}", token.substring(0, Math.min(20, token.length())) + "...");

                if (jwtUtils.validateToken(token)) {
                    // Token有效，将用户信息存入请求属性
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);

                    request.setAttribute("userId", userId);
                    request.setAttribute("username", username);

                    log.info("✅ 用户认证成功: userId={}, username={}", userId, username);
                } else {
                    log.warn("❌ Token验证失败: URI={}", requestUri);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效或已过期");
                    return;
                }
            } else {
                log.warn("⚠️ 未找到Token: URI={}", requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
                return;
            }
        } catch (Exception e) {
            log.error("❌ JWT认证异常: URI={}, 错误={}", requestUri, e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "认证失败: " + e.getMessage());
            return;
        }

        log.info("✅ 过滤器通过,继续执行请求链: {}", requestUri);
        filterChain.doFilter(request, response);
    }

    /**
     * 设置 CORS headers
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    /**
     * 从请求头中提取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 检查是否是白名单路径
     */
    private boolean isWhiteListPath(String requestUri) {
        if (WHITE_LIST.contains(requestUri)) {
            return true;
        }

        for (String path : WHITE_LIST) {
            if (requestUri.startsWith(path + "/")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"data\":null,\"message\":\"%s\"}",
                status == HttpServletResponse.SC_UNAUTHORIZED ? 401 : status,
                message
        ));
        response.getWriter().flush();
    }
}