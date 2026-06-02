package org.example.shareddocs.common.enums;

/**
 * 上传状态枚举
 */
public enum UploadStatus {
    
    UPLOADING("uploading", "上传中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败");
    
    private final String code;
    private final String description;
    
    UploadStatus(String code, String description) {
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
