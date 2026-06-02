package org.example.shareddocs.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 版本对比请求
 */
@Data
public class VersionCompareRequest {
    
    /**
     * 版本号A
     */
    @NotNull(message = "版本号A不能为空")
    private Integer versionA;
    
    /**
     * 版本号B
     */
    @NotNull(message = "版本号B不能为空")
    private Integer versionB;
}
