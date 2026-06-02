package org.example.shareddocs.common.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    
    ONLINE("online", "在线"),
    AWAY("away", "离开"),
    OFFLINE("offline", "离线");
    
    private final String code;
    private final String description;
    
    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
