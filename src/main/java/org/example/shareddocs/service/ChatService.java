package org.example.shareddocs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.dto.response.ChatMessageResponse;
import org.example.shareddocs.entity.ChatMessage;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService extends IService<ChatMessage> {
    
    /**
     * 发送聊天消息
     */
    ChatMessage sendMessage(Long documentId, Long userId, String content);
    
    /**
     * 获取文档聊天记录（支持分页）
     */
    PageResult<ChatMessageResponse> getChatHistory(Long documentId, Integer page, Integer pageSize, Long beforeId);
}
