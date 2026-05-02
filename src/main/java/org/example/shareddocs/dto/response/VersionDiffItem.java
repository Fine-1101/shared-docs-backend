package org.example.shareddocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本差异项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionDiffItem {
    
    /**
     * 差异类型：unchanged、added、removed、modified
     */
    private String type;
    
    /**
     * 差异起始位置
     */
    private Integer position;
    
    /**
     * 差异长度
     */
    private Integer length;
    
    /**
     * 版本A的内容
     */
    private String contentA;
    
    /**
     * 版本B的内容
     */
    private String contentB;
}
