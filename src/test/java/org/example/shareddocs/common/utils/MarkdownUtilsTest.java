package org.example.shareddocs.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Markdown 工具类测试
 */
class MarkdownUtilsTest {
    
    @Test
    void testExtractPlainText() {
        String markdown = "# 标题\n\n**粗体**和*斜体*\n\n- 列表项\n\n[链接](url)";
        String plainText = MarkdownUtils.extractPlainText(markdown);
        
        System.out.println("原文: " + markdown);
        System.out.println("纯文本: " + plainText);
        
        assertNotNull(plainText);
        assertFalse(plainText.contains("#"));
        assertFalse(plainText.contains("**"));
        assertFalse(plainText.contains("["));
    }
    
    @Test
    void testCountWords() {
        String markdown = "# 标题\n\n这是测试文本";
        int count = MarkdownUtils.countWords(markdown);
        
        System.out.println("字数: " + count);
        assertTrue(count > 0);
    }
    
    @Test
    void testTruncate() {
        String markdown = "这是一段很长的文本，用于测试截断功能是否正常工作的内容。";
        String truncated = MarkdownUtils.truncate(markdown, 10);
        
        System.out.println("原文长度: " + markdown.length());
        System.out.println("截断后: " + truncated);
        
        assertTrue(truncated.length() <= 13); // 10 + "..."
        assertTrue(truncated.endsWith("..."));
    }
}
