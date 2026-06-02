package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 聊天发送请求
 */
@Data
public class ChatSendRequest {
    
    /**
     * 客户端生成的消息ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;
    
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容长度不能超过500个字符")
    private String content;
    
    /**
     * @的用户ID列表
     */
    private List<Long> mentionUsers;
}
