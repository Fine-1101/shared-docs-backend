-- 为用户表添加password字段
ALTER TABLE `users` ADD COLUMN `password` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密码（SHA-256加密）' AFTER `username`;

-- 为已有用户设置默认密码（可选）
-- UPDATE `users` SET `password` = '默认密码的SHA256哈希值' WHERE `password` = '';
