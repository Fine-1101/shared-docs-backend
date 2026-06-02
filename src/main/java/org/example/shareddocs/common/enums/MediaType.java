package org.example.shareddocs.common.enums;

/**
 * 媒体类型枚举
 */
public enum MediaType {
    
    IMAGE("image", "图片"),
    VIDEO("video", "视频");
    
    private final String code;
    private final String description;
    
    MediaType(String code, String description) {
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
