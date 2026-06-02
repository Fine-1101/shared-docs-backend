package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文档导出请求
 */
@Data
public class DocumentExportRequest {
    
    /**
     * 导出格式：md、html、pdf、docx
     */
    @NotBlank(message = "导出格式不能为空")
    private String format;
    
    /**
     * 是否包含媒体文件（打包为zip）
     */
    private Boolean includeMedia = false;
}
