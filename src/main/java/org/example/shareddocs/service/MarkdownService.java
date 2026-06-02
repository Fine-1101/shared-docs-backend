package org.example.shareddocs.service;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

/**
 * Markdown 服务
 * 提供 Markdown 文本与 HTML 之间的转换功能
 */
@Slf4j
@Service
public class MarkdownService {
    
    private final Parser parser;
    private final HtmlRenderer renderer;
    
    public MarkdownService() {
        // 创建 Markdown 解析器
        this.parser = Parser.builder().build();
        // 创建 HTML 渲染器
        this.renderer = HtmlRenderer.builder().build();
        log.info("MarkdownService 初始化完成");
    }
    
    /**
     * 将 Markdown 文本转换为 HTML
     * 
     * @param markdown Markdown 格式的文本
     * @return HTML 格式的文本
     */
    public String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            log.debug("Markdown 内容为空，返回空字符串");
            return "";
        }
        
        try {
            // 解析 Markdown
            Node document = parser.parse(markdown);
            // 渲染为 HTML
            String html = renderer.render(document);
            log.debug("Markdown 转换为 HTML 成功，原文长度: {}, HTML长度: {}", 
                     markdown.length(), html.length());
            return html;
        } catch (Exception e) {
            log.error("Markdown 转换为 HTML 失败", e);
            // 如果转换失败，返回原始文本（避免数据丢失）
            return markdown;
        }
    }
    
    /**
     * 验证文本是否为 Markdown 格式（简单判断）
     * 
     * @param text 待验证的文本
     * @return 如果包含 Markdown 语法特征则返回 true
     */
    public boolean isMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 简单的 Markdown 特征检测
        return text.contains("# ") ||      // 标题
               text.contains("**") ||      // 粗体
               text.contains("*") ||       // 斜体或列表
               text.contains("[") ||       // 链接
               text.contains("```") ||     // 代码块
               text.contains("> ") ||      // 引用
               text.contains("- ") ||      // 无序列表
               text.matches(".*\\d+\\.\\s.*"); // 有序列表
    }
}
