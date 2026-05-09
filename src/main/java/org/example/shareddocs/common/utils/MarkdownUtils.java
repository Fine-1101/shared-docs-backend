package org.example.shareddocs.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Markdown 工具类
 * 提供 Markdown 相关的辅助功能
 */
@Slf4j
public class MarkdownUtils {
    
    /**
     * 从 Markdown 文本中提取纯文本（去除所有 Markdown 标记）
     * 
     * @param markdown Markdown 文本
     * @return 纯文本
     */
    public static String extractPlainText(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        String text = markdown;
        
        // 移除代码块
        text = text.replaceAll("```[\\s\\S]*?```", "");
        text = text.replaceAll("`([^`]+)`", "$1");
        
        // 移除图片标记 ![alt](url)
        text = text.replaceAll("!\\[([^\\]]*)\\]\\([^)]+\\)", "$1");
        
        // 移除链接标记 [text](url)
        text = text.replaceAll("\\[([^\\]]*)\\]\\([^)]+\\)", "$1");
        
        // 移除标题标记 #
        text = text.replaceAll("^#{1,6}\\s+", "");
        
        // 移除粗体和斜体标记
        text = text.replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1");
        text = text.replaceAll("_{1,3}([^_]+)_{1,3}", "$1");
        
        // 移除删除线标记
        text = text.replaceAll("~~([^~]+)~~", "$1");
        
        // 移除引用标记 >
        text = text.replaceAll("^>\\s+", "");
        
        // 移除列表标记
        text = text.replaceAll("^[\\s]*[-*+]\\s+", "");
        text = text.replaceAll("^[\\s]*\\d+\\.\\s+", "");
        
        // 移除水平线
        text = text.replaceAll("^[-*_]{3,}$", "");
        
        // 移除多余的空行
        text = text.replaceAll("\n{3,}", "\n\n");
        
        return text.trim();
    }
    
    /**
     * 计算 Markdown 文本的字数（去除标记后的纯文本字数）
     * 
     * @param markdown Markdown 文本
     * @return 字数
     */
    public static int countWords(String markdown) {
        String plainText = extractPlainText(markdown);
        // 中文字符和英文单词都计算
        return plainText.replaceAll("\\s+", "").length();
    }
    
    /**
     * 截断 Markdown 文本到指定长度（保持语法完整性）
     * 
     * @param markdown Markdown 文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String truncate(String markdown, int maxLength) {
        if (markdown == null || markdown.length() <= maxLength) {
            return markdown;
        }
        
        // 简单截断，可能需要更复杂的逻辑来保持 Markdown 语法完整
        String truncated = markdown.substring(0, maxLength);
        
        // 确保不在单词中间截断
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0) {
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated + "...";
    }
}
