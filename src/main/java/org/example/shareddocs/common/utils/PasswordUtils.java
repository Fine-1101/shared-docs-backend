package org.example.shareddocs.common.utils;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具类
 */
@Component
public class PasswordUtils {
    
    /**
     * 加密密码（使用SHA-256）
     * 注意：生产环境建议使用BCrypt
     */
    public String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, String encryptedPassword) {
        String encryptedInput = encryptPassword(rawPassword);
        return encryptedInput.equals(encryptedPassword);
    }
}
