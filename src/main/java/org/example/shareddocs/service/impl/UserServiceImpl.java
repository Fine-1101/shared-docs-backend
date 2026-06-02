package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.common.exception.BusinessException;
import org.example.shareddocs.common.utils.JwtUtils;
import org.example.shareddocs.common.utils.PasswordUtils;
import org.example.shareddocs.dto.request.LoginRequest;
import org.example.shareddocs.dto.request.RegisterRequest;
import org.example.shareddocs.dto.request.UserUpdateRequest;
import org.example.shareddocs.dto.response.LoginResponse;
import org.example.shareddocs.dto.response.OnlineUserResponse;
import org.example.shareddocs.dto.response.UserResponse;
import org.example.shareddocs.entity.User;

import org.example.shareddocs.mapper.UserMapper;

import org.example.shareddocs.service.UserService;
import org.example.shareddocs.websocket.WebSocketSessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;
    private final WebSocketSessionManager sessionManager;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPassword(passwordUtils.encryptPassword(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        save(user);
        
        // 清除密码字段后返回
        user.setPassword(null);
        return user;
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordUtils.verifyPassword(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 生成Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        
        // 构建响应
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration / 1000) // 转换为秒
                .build();
    }
    
    @Override
    public UserResponse getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper);
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException("刷新令牌不能为空");
        }
        
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
        
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        
        // 查询用户
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 生成新的访问令牌和刷新令牌
        String newAccessToken = jwtUtils.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);
        
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration / 1000)
                .build();
    }
    
    @Override
    public void logout(HttpServletRequest request) {
        // 从请求中获取用户ID
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            // 移除该用户的所有WebSocket会话
            sessionManager.removeUserSessions(userId);
            log.info("用户 {} 已登出", userId);
        }
    }
    
    @Override
    @Transactional
    public User updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 更新昵称
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        
        // 更新头像
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        // 修改密码（如果提供了新密码）
        if (StringUtils.hasText(request.getNewPassword())) {
            // 验证原密码
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException("修改密码需要提供原密码");
            }
            if (!passwordUtils.verifyPassword(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            // 更新为新密码
            user.setPassword(passwordUtils.encryptPassword(request.getNewPassword()));
            log.info("用户修改密码: userId={}", userId);
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        
        // 清除密码后返回
        user.setPassword(null);
        return user;
    }
    
    @Override
    public List<OnlineUserResponse> getOnlineUsers(Long documentId) {
        // 从WebSocket会话管理器获取在线用户ID
        Set<Long> onlineUserIds = sessionManager.getDocumentOnlineUsers(documentId);
        
        if (onlineUserIds == null || onlineUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 查询用户信息
        List<User> users = listByIds(onlineUserIds);
        
        return users.stream()
                .map(user -> OnlineUserResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .status("online")
                        .cursorPosition(null) // TODO: 从WebSocket会话中获取光标位置
                        .joinedAt(LocalDateTime.now().toString()) // TODO: 记录加入时间
                        .build())
                .collect(Collectors.toList());
    }
}
