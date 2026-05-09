# Markdown 支持使用说明

## 功能概述

后端已完整支持 Markdown 文法，提供以下功能：

1. ✅ **Markdown 解析**：将 Markdown 文本转换为 HTML
2. ✅ **文档导出**：支持导出为 Markdown (.md) 和 HTML (.html) 格式
3. ✅ **Markdown 工具**：提供纯文本提取、字数统计等辅助功能
4. ✅ **版本对比**：支持 Markdown 文档的版本差异对比

## 技术栈

- **Markdown 解析库**：[commonmark-java](https://github.com/commonmark/commonmark-java) v0.21.0
- **符合标准**：CommonMark 规范（Markdown 的标准规范）

## API 接口

### 1. 导出文档

**接口地址**：`POST /api/documents/{docId}/export`

**请求参数**：
```json
{
  "format": "md",  // 或 "html"
  "includeMedia": false  // 是否包含媒体文件（暂未实现）
}
```

**响应**：
- `format=md`：返回 Markdown 原文（text/plain）
- `format=html`：返回渲染后的 HTML 文档（text/html）

**使用示例**：

```bash
# 导出为 Markdown
curl -X POST http://localhost:8080/api/documents/1/export \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"format": "md"}' \
  --output document.md

# 导出为 HTML
curl -X POST http://localhost:8080/api/documents/1/export \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"format": "html"}' \
  --output document.html
```

## 支持的 Markdown 语法

### 基础语法

| 语法 | 示例 | 说明 |
|------|------|------|
| 标题 | `# H1` `## H2` `### H3` | 支持 1-6 级标题 |
| 粗体 | `**文本**` 或 `__文本__` | 加粗显示 |
| 斜体 | `*文本*` 或 `_文本_` | 斜体显示 |
| 删除线 | `~~文本~~` | 删除线效果 |
| 链接 | `[文本](URL)` | 超链接 |
| 图片 | `![alt](URL)` | 插入图片 |
| 代码 | `` `代码` `` | 行内代码 |
| 代码块 | ` ```语言\n代码\n``` ` | 多行代码块 |
| 引用 | `> 引用文本` | 引用块 |
| 无序列表 | `- 项目` 或 `* 项目` | 列表项 |
| 有序列表 | `1. 项目` | 编号列表 |
| 表格 | `\| 列1 \| 列2 \|` | 表格 |
| 水平线 | `---` 或 `***` | 分隔线 |

### 示例文档

```markdown
# 文档标题

这是一段普通文本，支持 **粗体**、*斜体* 和 ~~删除线~~。

## 代码示例

行内代码：`console.log('Hello')`

代码块：
```javascript
function hello() {
    console.log('Hello, Markdown!');
}
```

## 列表

### 无序列表
- 项目一
- 项目二
  - 子项目

### 有序列表
1. 第一步
2. 第二步
3. 第三步

## 表格

| 姓名 | 年龄 | 城市 |
|------|------|------|
| 张三 | 25 | 北京 |
| 李四 | 30 | 上海 |

## 引用

> 这是一段引用文本
> 可以有多行

## 链接

[访问 GitHub](https://github.com)

## 图片

![示例图片](https://example.com/image.png)
```

## 服务类说明

### 1. MarkdownService

**位置**：`org.example.shareddocs.service.MarkdownService`

**主要方法**：
- `String toHtml(String markdown)` - 将 Markdown 转换为 HTML
- `boolean isMarkdown(String text)` - 检测文本是否为 Markdown 格式

**使用示例**：
```java
@Autowired
private MarkdownService markdownService;

// 转换 Markdown 为 HTML
String html = markdownService.toHtml("# Hello\n**World**");
// 输出: <h1>Hello</h1>\n<p><strong>World</strong></p>
```

### 2. DocumentExportService

**位置**：`org.example.shareddocs.service.DocumentExportService`

**主要方法**：
- `ResponseEntity<byte[]> exportDocument(Long documentId, String format)` - 导出文档

**支持的格式**：
- `md` - Markdown 原文
- `html` - 渲染后的 HTML（包含完整样式）

### 3. MarkdownUtils

**位置**：`org.example.shareddocs.common.utils.MarkdownUtils`

**主要方法**：
- `String extractPlainText(String markdown)` - 提取纯文本（去除 Markdown 标记）
- `int countWords(String markdown)` - 计算字数
- `String truncate(String markdown, int maxLength)` - 截断文本

**使用示例**：
```java
String markdown = "# 标题\n**粗体文本**";
String plainText = MarkdownUtils.extractPlainText(markdown);
// 输出: "标题\n粗体文本"

int wordCount = MarkdownUtils.countWords(markdown);
// 输出: 6
```

## 前端集成建议

### 方案一：前端渲染（推荐）

前端使用 Markdown 编辑器（如 TipTap、Editor.js），后端只存储和传输 Markdown 原文：

```typescript
// 前端获取文档内容
const response = await fetch('/api/documents/1/content');
const { content } = await response.json();

// 前端渲染 Markdown
import { marked } from 'marked';
const html = marked.parse(content);
document.getElementById('preview').innerHTML = html;
```

### 方案二：后端渲染

前端请求 HTML 格式的文档：

```typescript
// 导出为 HTML
const response = await fetch('/api/documents/1/export', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ format: 'html' })
});

const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
window.open(url);
```

## 注意事项

1. **存储格式**：文档在数据库中仍以纯文本形式存储 Markdown 原文
2. **实时协同**：WebSocket 协同编辑操作的是 Markdown 原文
3. **版本管理**：版本快照保存的是 Markdown 原文
4. **导出 HTML**：导出的 HTML 包含完整的 CSS 样式，可直接在浏览器中打开
5. **性能优化**：Markdown 转换是轻量级操作，适合实时处理

## 未来扩展

可以进一步实现的功能：

- [ ] PDF 导出（需集成 iText 或 OpenHTMLtoPDF）
- [ ] DOCX 导出（需集成 Apache POI）
- [ ] Markdown 增强语法支持（脚注、数学公式等，需使用 flexmark-java）
- [ ] 实时 Markdown 预览（前端实现）
- [ ] Markdown 语法高亮（前端实现）

## 相关文档

- [CommonMark 规范](https://commonmark.org/)
- [commonmark-java GitHub](https://github.com/commonmark/commonmark-java)
- [Markdown 官方指南](https://www.markdownguide.org/)
