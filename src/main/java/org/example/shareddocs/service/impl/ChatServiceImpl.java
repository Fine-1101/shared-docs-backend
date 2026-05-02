package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.response.ChatMessageResponse;
import org.example.shareddocs.entity.ChatMessage;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.ChatMessageMapper;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {
    
    private final UserMapper userMapper;
    
    @Override
    public ChatMessage sendMessage(Long documentId, Long userId, String content) {
        ChatMessage message = new ChatMessage();
        message.setDocumentId(documentId);
        message.setUserId(userId);
        message.setMessageType("text");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        save(message);
        return message;
    }
    
    @Override
    public PageResult<ChatMessageResponse> getChatHistory(Long documentId, Integer page, Integer pageSize, Long beforeId) {
        // 构建查询条件
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getDocumentId, documentId);
        
        // beforeId参数：获取此ID之前的消息（用于加载更多）
        if (beforeId != null) {
            wrapper.lt(ChatMessage::getId, beforeId);
        }
        
        // 按创建时间降序排列（最新的在前）
        wrapper.orderByDesc(ChatMessage::getCreatedAt);
        
        // 分页查询
        Page<ChatMessage> pageParam = new Page<>(page, pageSize);
        Page<ChatMessage> resultPage = page(pageParam, wrapper);
        
        List<ChatMessage> messages = resultPage.getRecords();
        
        // 转换为响应DTO并填充用户信息
        List<ChatMessageResponse> responseList = messages.stream()
                .map(msg -> {
                    User user = userMapper.selectById(msg.getUserId());
                    return ChatMessageResponse.builder()
                            .id(msg.getId())
                            .documentId(msg.getDocumentId())
                            .userId(msg.getUserId())
                            .username(user != null ? user.getUsername() : "未知用户")
                            .nickname(user != null ? user.getNickname() : "未知用户")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .messageType(msg.getMessageType())
                            .content(msg.getContent())
                            .createdAt(msg.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        // 计算是否有更多消息
        long total = resultPage.getTotal();
        boolean hasMore = (page * pageSize) < total;
        
        return new PageResult<>(total, page, pageSize, responseList, hasMore);
    }
}
