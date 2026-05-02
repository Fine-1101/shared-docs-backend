package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天消息数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageData {
    
    /**
     * 客户端生成的消息ID
     */
    private String messageId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * @的用户ID列表
     */
    private List<Long> mentionUsers;
}
