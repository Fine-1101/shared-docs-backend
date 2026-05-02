package org.example.shareddocs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.request.LoginRequest;
import org.example.shareddocs.dto.request.RegisterRequest;
import org.example.shareddocs.dto.request.UserUpdateRequest;
import org.example.shareddocs.dto.response.LoginResponse;
import org.example.shareddocs.dto.response.OnlineUserResponse;
import org.example.shareddocs.dto.response.UserResponse;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success("注册成功", user);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<UserResponse> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        UserResponse userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        LoginResponse response = userService.refreshToken(refreshToken);
        return Result.success("刷新成功", response);
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        userService.logout(request);
        return Result.successVoid("登出成功");
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(HttpServletRequest request,
                                      @Valid @RequestBody UserUpdateRequest updateRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        User user = userService.updateUserInfo(userId, updateRequest);
        return Result.success("更新成功", user);
    }
    
    /**
     * 获取文档在线用户
     */
    @GetMapping("/documents/{docId}/users")
    public Result<Map<String, Object>> getOnlineUsers(@PathVariable Long docId) {
        List<OnlineUserResponse> users = userService.getOnlineUsers(docId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", users.size());
        data.put("users", users);
        
        return Result.success(data);
    }
}
