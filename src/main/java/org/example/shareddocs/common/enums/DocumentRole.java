package org.example.shareddocs.common.enums;

/**
 * 文档角色枚举
 */
public enum DocumentRole {
    
    OWNER("owner", "所有者"),
    EDITOR("editor", "编辑者"),
    VIEWER("viewer", "查看者");
    
    private final String code;
    private final String description;
    
    DocumentRole(String code, String description) {
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
