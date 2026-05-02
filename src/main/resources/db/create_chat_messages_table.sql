-- 聊天消息表
DROP TABLE IF EXISTS chat_messages;

CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型：text, image, file',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_document_id (document_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 插入测试数据（可选）
INSERT INTO chat_messages (document_id, user_id, message_type, content, created_at) VALUES
(7, 7, 'text', 'Hello World', NOW()),
(7, 8, 'text', 'Hi there!', NOW());
