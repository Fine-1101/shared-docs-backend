package org.example.shareddocs.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.PageResult;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.response.ChatMessageResponse;
import org.example.shareddocs.entity.ChatMessage;
import org.example.shareddocs.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 */
@RestController
@RequestMapping("/documents/{docId}/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    /**
     * 获取聊天记录（支持分页）
     */
    @GetMapping
    public Result<PageResult<ChatMessageResponse>> getChatHistory(@PathVariable Long docId,
                                                                   @RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                                   @RequestParam(required = false) Long beforeId) {
        PageResult<ChatMessageResponse> result = chatService.getChatHistory(docId, page, pageSize, beforeId);
        return Result.success(result);
    }
    
    /**
     * 发送消息（REST API方式，WebSocket是主要方式）
     */
    @PostMapping
    public Result<ChatMessage> sendMessage(HttpServletRequest request,
                                           @PathVariable Long docId,
                                           @RequestBody Map<String, String> requestBody) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        String content = requestBody.get("content");
        ChatMessage message = chatService.sendMessage(docId, userId, content);
        return Result.success(message);
    }
}
