package org.example.shareddocs.config;

import lombok.RequiredArgsConstructor;
import org.example.shareddocs.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);

        // 🔥 关键修复:匹配所有路径,确保 /api/documents 等请求都能经过过滤器
        registration.addUrlPatterns("/*");

        registration.setOrder(1);
        return registration;
    }
}