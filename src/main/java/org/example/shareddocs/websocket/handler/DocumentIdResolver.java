package org.example.shareddocs.websocket.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.mapper.DocumentMapper;
import org.springframework.stereotype.Component;

/**
 * 文档ID解析工具
 * 支持Long和UUID格式的文档ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIdResolver {
    
    private final DocumentMapper documentMapper;
    
    /**
     * 解析文档ID（支持Long和UUID字符串）
     * 
     * @param docId 文档ID字符串（可以是数字或UUID）
     * @return Long类型的文档ID，如果无效则返回null
     */
    public Long resolve(String docId) {
        if (docId == null || docId.trim().isEmpty()) {
            log.warn("文档ID为空");
            return null;
        }
        
        try {
            // 尝试解析为Long
            return Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 如果是UUID格式，查询数据库获取Long ID
            log.debug("文档ID是UUID格式，尝试查询: {}", docId);
            return resolveUuid(docId);
        }
    }
    
    /**
     * 通过UUID解析文档ID
     */
    private Long resolveUuid(String uuid) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getDocUuid, uuid)
               .eq(Document::getIsDeleted, 0);
        Document document = documentMapper.selectOne(wrapper);
        
        if (document != null) {
            log.debug("通过UUID找到文档ID: uuid={}, id={}", uuid, document.getId());
            return document.getId();
        } else {
            log.warn("未找到UUID对应的文档: {}", uuid);
            return null;
        }
    }
}
