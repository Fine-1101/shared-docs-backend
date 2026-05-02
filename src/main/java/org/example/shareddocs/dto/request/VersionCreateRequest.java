package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 版本创建请求
 */
@Data
public class VersionCreateRequest {
    
    /**
     * 版本描述
     */
    @Size(max = 500, message = "版本描述长度不能超过500个字符")
    private String description;
}
