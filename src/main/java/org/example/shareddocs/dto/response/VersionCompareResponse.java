package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 版本对比结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCompareResponse {
    
    /**
     * 版本A信息
     */
    private VersionInfo versionA;
    
    /**
     * 版本B信息
     */
    private VersionInfo versionB;
    
    /**
     * 差异列表
     */
    private List<VersionDiffItem> diff;
    
    /**
     * 差异摘要
     */
    private DiffSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        private Integer versionNumber;
        private String createdAt;
        private String creatorName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffSummary {
        /**
         * 新增字符数
         */
        private Integer addedCount;
        
        /**
         * 删除字符数
         */
        private Integer removedCount;
        
        /**
         * 修改字符数
         */
        private Integer modifiedCount;
    }
}
