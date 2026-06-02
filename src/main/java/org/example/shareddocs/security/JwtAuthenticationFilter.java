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
            "/api/ws",  // WebSocket 握手请求，由 AuthHandshakeInterceptor 处理
            "/api/media"  // 媒体文件访问（图片、视频等），需要公开访问支持
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        //  关键:处理 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Handling CORS preflight request for: {}", requestUri);
            setCorsHeaders(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 设置 CORS headers
        setCorsHeaders(response);

        // 白名单路径跳过认证
        if (isWhiteListPath(requestUri, request.getMethod())) {
            log.debug("White list path, skip authentication: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        log.info(" JwtAuthenticationFilter EXECUTED for URI: {}", requestUri);

        try {
            // 从请求头获取Token
            String token = getTokenFromRequest(request);

            log.info("JWT过滤器 - URI: {}, Token存在: {}", requestUri, token != null);

            if (token != null) {
                log.info("Token前缀: {}", token.substring(0, Math.min(20, token.length())) + "...");

                if (jwtUtils.validateToken(token)) {
                    // Token有效，将用户信息存入请求属性
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);

                    request.setAttribute("userId", userId);
                    request.setAttribute("username", username);

                    log.info("用户认证成功: userId={}, username={}", userId, username);
                } else {
                    log.warn("Token验证失败: URI={}", requestUri);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token无效或已过期");
                    return;
                }
            } else {
                log.warn("未找到Token: URI={}", requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
                return;
            }
        } catch (Exception e) {
            log.error("JWT认证异常: URI={}, 错误={}", requestUri, e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "认证失败: " + e.getMessage());
            return;
        }

        log.info("过滤器通过,继续执行请求链: {}", requestUri);
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
     * 从请求头中提取Token，如果请求头中没有，则尝试从URL参数中获取
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 优先从 Header 获取
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 尝试从 URL 参数获取（用于 <img> 标签等无法携带 Header 的场景）
        String paramToken = request.getParameter("token");
        if (paramToken != null && !paramToken.trim().isEmpty()) {
            log.debug("从 URL 参数中获取到 Token");
            return paramToken;
        }

        return null;
    }

    /**
     * 检查是否是白名单路径
     * 注意：/api/media 路径只对 GET 请求开放（查看媒体文件），上传/删除等操作仍需认证
     */
    private boolean isWhiteListPath(String requestUri, String httpMethod) {
        // 对于 /api/media 路径，只有 GET 请求才跳过认证
        if (requestUri.startsWith("/api/media")) {
            return "GET".equalsIgnoreCase(httpMethod);
        }
        
        // 其他白名单路径完全开放
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