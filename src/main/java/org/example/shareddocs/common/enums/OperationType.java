package org.example.shareddocs.common.enums;

/**
 * 操作类型枚举
 */
public enum OperationType {
    
    INSERT("insert", "插入"),
    DELETE("delete", "删除"),
    REPLACE("replace", "替换"),
    CURSOR_MOVE("cursor_move", "光标移动");
    
    private final String code;
    private final String description;
    
    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown operation type: " + code);
    }
}
