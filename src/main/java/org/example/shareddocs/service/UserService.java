package org.example.shareddocs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.example.shareddocs.dto.request.LoginRequest;
import org.example.shareddocs.dto.request.RegisterRequest;
import org.example.shareddocs.dto.request.UserUpdateRequest;
import org.example.shareddocs.dto.response.LoginResponse;
import org.example.shareddocs.dto.response.OnlineUserResponse;
import org.example.shareddocs.dto.response.UserResponse;
import org.example.shareddocs.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户注册
     */
    User register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 刷新令牌
     */
    LoginResponse refreshToken(String refreshToken);
    
    /**
     * 用户登出
     */
    void logout(HttpServletRequest request);
    
    /**
     * 更新用户信息
     */
    User updateUserInfo(Long userId, UserUpdateRequest request);
    
    /**
     * 根据ID获取用户信息
     */
    UserResponse getUserInfo(Long userId);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 获取文档在线用户列表
     */
    List<OnlineUserResponse> getOnlineUsers(Long documentId);
    

}
