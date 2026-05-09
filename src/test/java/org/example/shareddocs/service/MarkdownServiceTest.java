package org.example.shareddocs.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Markdown 服务测试类
 */
@Slf4j
@SpringBootTest
class MarkdownServiceTest {
    
    @Autowired
    private MarkdownService markdownService;
    
    @BeforeEach
    void setUp() {
        log.info("开始 Markdown 服务测试");
    }
    
    @Test
    void testToHtml_BasicMarkdown() {
        // 测试基础 Markdown 转换
        String markdown = "# 标题\n\n这是一段**粗体**文本。";
        String html = markdownService.toHtml(markdown);
        
        log.info("Markdown: {}", markdown);
        log.info("HTML: {}", html);
        
        assertNotNull(html);
        assertTrue(html.contains("<h1>"));
        assertTrue(html.contains("<strong>"));
    }
    
    @Test
    void testToHtml_CodeBlock() {
        // 测试代码块
        String markdown = "```java\npublic class Test {}\n```";
        String html = markdownService.toHtml(markdown);
        
        log.info("代码块 HTML: {}", html);
        
        assertNotNull(html);
        assertTrue(html.contains("<code>"));
    }
    
    @Test
    void testToHtml_Lists() {
        // 测试列表
        String markdown = "- 项目一\n- 项目二\n- 项目三";
        String html = markdownService.toHtml(markdown);
        
        log.info("列表 HTML: {}", html);
        
        assertNotNull(html);
        assertTrue(html.contains("<ul>") || html.contains("<li>"));
    }
    
    @Test
    void testToHtml_EmptyContent() {
        // 测试空内容
        String html = markdownService.toHtml("");
        assertEquals("", html);
        
        html = markdownService.toHtml(null);
        assertEquals("", html);
    }
    
    @Test
    void testIsMarkdown_ValidMarkdown() {
        // 测试 Markdown 检测
        assertTrue(markdownService.isMarkdown("# 标题"));
        assertTrue(markdownService.isMarkdown("**粗体**"));
        assertTrue(markdownService.isMarkdown("[链接](url)"));
        assertTrue(markdownService.isMarkdown("```代码```"));
    }
    
    @Test
    void testIsMarkdown_PlainText() {
        // 测试纯文本（不是 Markdown）
        assertFalse(markdownService.isMarkdown("这是普通文本"));
        assertFalse(markdownService.isMarkdown("Hello World"));
    }
}
