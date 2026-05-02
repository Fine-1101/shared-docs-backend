package org.example.shareddocs.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 文档缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String DOCUMENT_KEY_PREFIX = "doc:";
    private static final long DOCUMENT_CACHE_TTL = 3600; // 1小时
    
    /**
     * 缓存文档内容
     */
    public void cacheDocumentContent(Long documentId, String content) {
        String key = DOCUMENT_KEY_PREFIX + documentId;
        redisTemplate.opsForValue().set(key, content, DOCUMENT_CACHE_TTL, TimeUnit.SECONDS);
        log.debug("缓存文档内容: documentId={}", documentId);
    }
    
    /**
     * 获取缓存的文档内容
     */
    public String getCachedDocumentContent(Long documentId) {
        String key = DOCUMENT_KEY_PREFIX + documentId;
        Object content = redisTemplate.opsForValue().get(key);
        return content != null ? content.toString() : null;
    }
    
    /**
     * 清除文档缓存
     */
    public void invalidateDocumentCache(Long documentId) {
        String key = DOCUMENT_KEY_PREFIX + documentId;
        redisTemplate.delete(key);
        log.debug("清除文档缓存: documentId={}", documentId);
    }
}
