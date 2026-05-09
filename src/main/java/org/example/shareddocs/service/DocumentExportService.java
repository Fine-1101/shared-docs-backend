package org.example.shareddocs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.mapper.DocumentMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文档导出服务
 * 支持将文档导出为 Markdown、HTML 等格式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExportService {
    
    private final DocumentMapper documentMapper;
    private final MarkdownService markdownService;
    
    /**
     * 导出文档为指定格式
     * 
     * @param documentId 文档ID
     * @param format 导出格式：md、html
     * @return ResponseEntity 包含导出文件的内容
     */
    public ResponseEntity<byte[]> exportDocument(Long documentId, String format) {
        log.info("开始导出文档，documentId={}, format={}", documentId, format);
        
        // 查询文档
        Document document = documentMapper.selectById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            log.warn("文档不存在或已删除，documentId={}", documentId);
            throw new RuntimeException("文档不存在");
        }
        
        String content = document.getContent() != null ? document.getContent() : "";
        String filename = generateFilename(document.getTitle(), format);
        
        switch (format.toLowerCase()) {
            case "md":
                return exportAsMarkdown(content, filename);
            case "html":
                return exportAsHtml(content, filename);
            default:
                log.warn("不支持的导出格式: {}", format);
                throw new RuntimeException("不支持的导出格式: " + format);
        }
    }
    
    /**
     * 导出为 Markdown 格式
     */
    private ResponseEntity<byte[]> exportAsMarkdown(String content, String filename) {
        log.debug("导出为 Markdown 格式");
        
        HttpHeaders headers = new HttpHeaders();
        // 重要：指定 UTF-8 字符编码
        headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", filename + ".md");
        
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        headers.setContentLength(bytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
    
    /**
     * 导出为 HTML 格式
     */
    private ResponseEntity<byte[]> exportAsHtml(String content, String filename) {
        log.debug("导出为 HTML 格式");
        
        // 将 Markdown 转换为 HTML
        String html = markdownService.toHtml(content);
        
        // 构建完整的 HTML 文档
        String fullHtml = buildHtmlDocument(html, filename);
        
        HttpHeaders headers = new HttpHeaders();
        // 重要：指定 UTF-8 字符编码，避免中文乱码
        headers.setContentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", filename + ".html");
        
        byte[] bytes = fullHtml.getBytes(StandardCharsets.UTF_8);
        headers.setContentLength(bytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
    
    /**
     * 构建完整的 HTML 文档
     */
    private String buildHtmlDocument(String bodyContent, String title) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                            line-height: 1.6;
                            max-width: 900px;
                            margin: 0 auto;
                            padding: 20px;
                            color: #333;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            margin-top: 24px;
                            margin-bottom: 16px;
                            font-weight: 600;
                            line-height: 1.25;
                        }
                        code {
                            background-color: #f6f8fa;
                            padding: 2px 6px;
                            border-radius: 3px;
                            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
                        }
                        pre {
                            background-color: #f6f8fa;
                            padding: 16px;
                            border-radius: 6px;
                            overflow: auto;
                        }
                        pre code {
                            background-color: transparent;
                            padding: 0;
                        }
                        blockquote {
                            border-left: 4px solid #dfe2e5;
                            padding-left: 16px;
                            margin-left: 0;
                            color: #6a737d;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin: 16px 0;
                        }
                        table th, table td {
                            border: 1px solid #dfe2e5;
                            padding: 6px 13px;
                        }
                        table tr:nth-child(2n) {
                            background-color: #f6f8fa;
                        }
                        img {
                            max-width: 100%%;
                            height: auto;
                        }
                        a {
                            color: #0366d6;
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                    </style>
                </head>
                <body>
                    %s
                </body>
                </html>
                """.formatted(escapeHtml(title), bodyContent);
    }
    
    /**
     * 生成文件名
     */
    private String generateFilename(String title, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeTitle = title != null ? title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_") : "document";
        return safeTitle + "_" + timestamp;
    }
    
    /**
     * 转义 HTML 特殊字符
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
