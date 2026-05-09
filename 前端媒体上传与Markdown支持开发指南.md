# 前端开发指南：媒体上传与 Markdown 支持

## 📋 目录

1. [概述](#概述)
2. [媒体文件上传功能](#媒体文件上传功能)
3. [Markdown 编辑器集成](#markdown-编辑器集成)
4. [API 接口文档](#api-接口文档)
5. [示例代码](#示例代码)
6. [注意事项](#注意事项)

---

## 概述

本文档指导前端开发人员如何实现：
- ✅ **媒体文件上传**：支持简单上传和分块上传（大文件）
- ✅ **Markdown 编辑**：集成 Markdown 编辑器，支持实时预览
- ✅ **文档导出**：支持导出为 Markdown 和 HTML 格式

### 技术栈推荐

| 功能 | 推荐库 | 说明 |
|------|--------|------|
| Markdown 编辑器 | TipTap / Editor.js / React-Markdown | 富文本编辑 |
| Markdown 解析 | marked / markdown-it | Markdown → HTML |
| HTTP 客户端 | Axios | API 请求 |
| 文件上传 | 原生 FormData | 文件处理 |
| 进度条 | NProgress / 自定义 | 上传进度显示 |

---

## 媒体文件上传功能

### 功能特性

1. **简单上传**：适用于小文件（< 5MB）
2. **分块上传**：适用于大文件（视频、高清图片等）
   - 支持断点续传
   - 显示上传进度
   - 支持取消上传
3. **媒体管理**：查看、删除已上传的文件

### 上传流程

#### 方案一：简单上传（推荐小文件）

```
用户选择文件 → 调用上传接口 → 服务器保存 → 返回媒体ID和URL
```

**适用场景**：
- 图片（JPEG, PNG, GIF, WebP）
- 小于 5MB 的文件

#### 方案二：分块上传（推荐大文件）

```
1. 初始化上传 → 获取 uploadId
2. 分割文件 → 按 5MB 分块
3. 循环上传分块 → 显示进度
4. 完成上传 → 合并分块
5. 返回媒体信息
```

**适用场景**：
- 视频文件
- 大于 5MB 的大文件
- 需要断点续传的场景

---

## Markdown 编辑器集成

### 方案一：TipTap（推荐）

**优势**：
- 基于 ProseMirror，功能强大
- 支持协同编辑（配合 Yjs）
- 可扩展性强
- 良好的 TypeScript 支持

**安装**：
```bash
npm install @tiptap/core @tiptap/vue-3 @tiptap/starter-kit
npm install @tiptap/extension-image @tiptap/extension-link
```

**基本配置**：
```typescript
import { useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'

const editor = useEditor({
  content: '', // Markdown 内容
  extensions: [
    StarterKit,
    Image.configure({
      inline: true,
      allowBase64: false,
    }),
    Link.configure({
      openOnClick: false,
    }),
  ],
})
```

### 方案二：Editor.js

**优势**：
- 块级编辑器，输出结构化数据
- 简洁的 API
- 插件生态丰富

**安装**：
```bash
npm install @editorjs/editorjs @editorjs/header @editorjs/image
```

### 方案三：React-Markdown + 文本域

**优势**：
- 轻量级
- 简单易用
- 适合纯 Markdown 编辑

**安装**：
```bash
npm install react-markdown remark-gfm
```

---

## API 接口文档

### 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token（Header: `Authorization: Bearer <token>`）
- **Content-Type**: `application/json` 或 `multipart/form-data`

---

### 1. 简单上传

**接口**：`POST /media/documents/{docId}/upload`

**请求**：
```typescript
interface SimpleUploadRequest {
  docId: number;
  file: File;
}
```

**响应**：
```typescript
interface UploadResponse {
  mediaId: number;
  filename: string;
  storagePath: string;
  url: string;          // 访问URL: /api/media/{mediaId}
  fileSize: number;
  mimeType: string;
  uploadStatus: string; // "COMPLETED"
}
```

**示例代码**：
```typescript
async function simpleUpload(docId: number, file: File): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await axios.post(
    `/api/media/documents/${docId}/upload`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${getToken()}`
      }
    }
  );
  
  return response.data.data;
}
```

---

### 2. 分块上传

#### 步骤 1：初始化上传

**接口**：`POST /media/upload/init`

**请求**：
```typescript
interface UploadInitRequest {
  documentId: number;
  filename: string;
  fileSize: number;
  mimeType: string;
  mediaType: 'image' | 'video' | 'audio' | 'file';
  chunkSize?: number;     // 默认 5MB (5242880)
  fileHash: string;       // 文件 SHA-256 哈希值（必填）
}
```

**响应**：
```typescript
interface UploadInitResponse {
  uploadId: string;       // 上传会话ID
  mediaId: number;
  chunkSize: number;
  totalChunks: number;
  uploadedChunks: number[]; // 已上传的分块索引
}
```

**示例代码**：
```typescript
async function initChunkUpload(request: UploadInitRequest): Promise<UploadInitResponse> {
  const response = await axios.post(
    '/api/media/upload/init',
    request,
    {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    }
  );
  
  return response.data.data;
}
```

#### 步骤 2：上传分块

**接口**：`POST /media/upload/chunk`

**请求**（FormData）：
```typescript
interface ChunkUploadRequest {
  uploadId: string;
  chunkIndex: number;     // 分块索引（从0开始）
  chunkHash: string;      // 分块的 MD5 哈希值（必填）
  file: Blob;             // 分块文件数据（字段名必须是 file）
}
```

**响应**：
```typescript
interface ChunkUploadResult {
  uploadId: string;
  chunkIndex: number;
  success: boolean;
  message: string;
}
```

**示例代码**：
```typescript
async function uploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob,
  chunkHash: string
): Promise<ChunkUploadResult> {
  const formData = new FormData();
  formData.append('uploadId', uploadId);
  formData.append('chunkIndex', chunkIndex.toString());
  formData.append('chunkHash', chunkHash);  // 必填：分块哈希
  formData.append('file', chunk);           // 注意：字段名是 file，不是 chunk
  
  const response = await axios.post(
    '/api/media/upload/chunk',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        // 单个分块的上传进度
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total!
        );
        console.log(`分块 ${chunkIndex} 上传进度: ${percentCompleted}%`);
      }
    }
  );
  
  return response.data.data;
}
```

#### 步骤 3：完成上传

**接口**：`POST /media/upload/complete`

**请求**：
```typescript
interface UploadCompleteRequest {
  uploadId: string;
  fileHash?: string;  // 可选：完整文件的哈希值用于校验
}
```

**注意**：后端完成上传时会自动从 Redis 会话中获取文件名、文件大小等信息，不需要前端再次传递。

**响应**：
```typescript
interface MediaAssetResponse {
  id: number;
  documentId: number;
  filename: string;
  fileSize: number;
  mimeType: string;
  mediaType: string;
  url: string;
  thumbnailUrl?: string;  // 缩略图URL（图片）
  placeholderText: string; // Markdown 占位符
  width?: number;
  height?: number;
  uploaderId: number;
  uploaderName: string;
  createdAt: string;
}
```

**示例代码**：
```typescript
async function completeUpload(
  request: UploadCompleteRequest
): Promise<MediaAssetResponse> {
  const response = await axios.post(
    '/api/media/upload/complete',
    request,
    {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    }
  );
  
  return response.data.data;
}
```

#### 完整分块上传流程

```typescript
async function chunkedUpload(
  docId: number,
  file: File,
  onProgress?: (percent: number) => void
): Promise<MediaAssetResponse> {
  const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  
  // 计算文件哈希（可以使用 spark-md5 库）
  const fileHash = await calculateFileHash(file);  // SHA-256
  
  // 1. 初始化上传
  const initResponse = await initChunkUpload({
    documentId: docId,
    filename: file.name,
    fileSize: file.size,
    mimeType: file.type,
    mediaType: getMediaType(file.type),
    chunkSize: CHUNK_SIZE,
    fileHash: fileHash  // 必填：文件哈希
  });
  
  const { uploadId } = initResponse;
  
  try {
    // 2. 循环上传分块
    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE;
      const end = Math.min(start + CHUNK_SIZE, file.size);
      const chunk = file.slice(start, end);
      
      // 计算分块哈希
      const chunkHash = await calculateChunkHash(chunk);  // MD5
      
      await uploadChunk(uploadId, i, chunk, chunkHash);
      
      // 更新总体进度
      if (onProgress) {
        const percent = Math.round(((i + 1) / totalChunks) * 100);
        onProgress(percent);
      }
    }
    
    // 3. 完成上传（只需要 uploadId）
    return await completeUpload({
      uploadId
      // fileHash 可选，如果需要校验可以传
    });
    
  } catch (error) {
    // 上传失败，可以取消上传
    await cancelUpload(uploadId);
    throw error;
  }
}

// 辅助函数：计算文件哈希（示例使用 Web Crypto API）
async function calculateFileHash(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// 辅助函数：计算分块哈希（MD5，可以使用 spark-md5 库）
async function calculateChunkHash(chunk: Blob): Promise<string> {
  // 这里简化处理，实际项目中建议使用 spark-md5
  const buffer = await chunk.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('MD5', buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// 辅助函数：判断媒体类型
function getMediaType(mimeType: string): 'image' | 'video' | 'audio' | 'file' {
  if (mimeType.startsWith('image/')) return 'image';
  if (mimeType.startsWith('video/')) return 'video';
  if (mimeType.startsWith('audio/')) return 'audio';
  return 'file';
}
```

---

### 3. 其他媒体接口

#### 获取文档媒体列表

**接口**：`GET /media/documents/{docId}/media`

**查询参数**：
- `mediaType`: 过滤类型（image/video/audio）
- `page`: 页码（默认 1）
- `pageSize`: 每页数量（默认 20）

**响应**：
```typescript
interface PageResult<T> {
  total: number;
  page: number;
  pageSize: number;
  list: T[];
}
```

#### 删除媒体文件

**接口**：`DELETE /media/{mediaId}`

#### 取消上传

**接口**：`POST /media/upload/cancel`

**请求**：
```typescript
{
  "uploadId": "xxx"
}
```

#### 获取上传状态

**接口**：`GET /media/upload/status/{uploadId}`

---

### 4. 文档导出

**接口**：`POST /documents/{docId}/export`

**请求**：
```typescript
interface DocumentExportRequest {
  format: 'md' | 'html';  // 导出格式
  includeMedia?: boolean;  // 是否包含媒体（暂未实现）
}
```

**响应**：
- `format=md`: 返回 `text/plain`，文件名 `.md`
- `format=html`: 返回 `text/html`，文件名 `.html`

**示例代码**：
```typescript
async function exportDocument(
  docId: number,
  format: 'md' | 'html'
): Promise<void> {
  const response = await axios.post(
    `/api/documents/${docId}/export`,
    { format },
    {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      },
      responseType: 'blob'
    }
  );
  
  // 触发下载
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `document.${format}`);
  document.body.appendChild(link);
  link.click();
  link.remove();
}
```

---

## 示例代码

### Vue 3 + TipTap 完整示例

```vue
<template>
  <div class="document-editor">
    <!-- 工具栏 -->
    <div class="toolbar">
      <button @click="insertImage">插入图片</button>
      <button @click="exportMarkdown">导出 Markdown</button>
      <button @click="exportHtml">导出 HTML</button>
    </div>
    
    <!-- 编辑器 -->
    <editor-content :editor="editor" class="editor-content" />
    
    <!-- 上传进度 -->
    <div v-if="uploading" class="upload-progress">
      <progress :value="uploadProgress" max="100"></progress>
      <span>{{ uploadProgress }}%</span>
      <button @click="cancelUploadHandler">取消</button>
    </div>
    
    <!-- 媒体列表 -->
    <div class="media-list">
      <h3>已上传的媒体</h3>
      <div v-for="media in mediaList" :key="media.id" class="media-item">
        <img v-if="media.mediaType === 'image'" :src="media.url" />
        <video v-if="media.mediaType === 'video'" :src="media.url" controls></video>
        <button @click="deleteMedia(media.id)">删除</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useEditor, EditorContent } from '@tiptap/vue-3';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import axios from 'axios';

const props = defineProps<{
  docId: number;
}>();

const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Image.configure({
      inline: true,
    }),
  ],
});

const uploading = ref(false);
const uploadProgress = ref(0);
const mediaList = ref<any[]>([]);
let currentUploadId: string | null = null;

// 插入图片
const insertImage = async () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*,video/*';
  
  input.onchange = async (e: Event) => {
    const target = e.target as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) return;
    
    try {
      uploading.value = true;
      uploadProgress.value = 0;
      
      let media;
      if (file.size < 5 * 1024 * 1024) {
        // 小文件：简单上传
        media = await simpleUpload(props.docId, file);
      } else {
        // 大文件：分块上传
        media = await chunkedUpload(props.docId, file, (progress) => {
          uploadProgress.value = progress;
        });
      }
      
      // 插入到编辑器
      if (media.mediaType === 'image') {
        editor.value?.chain().focus().setImage({ src: media.url }).run();
      } else if (media.mediaType === 'video') {
        // TipTap 默认不支持 video，可以自定义扩展或使用占位符
        const placeholder = `![视频: ${media.filename}](${media.url})`;
        editor.value?.chain().focus().insertContent(placeholder).run();
      }
      
      // 刷新媒体列表
      await loadMediaList();
      
    } catch (error) {
      console.error('上传失败:', error);
      alert('上传失败，请重试');
    } finally {
      uploading.value = false;
    }
  };
  
  input.click();
};

// 简单上传
const simpleUpload = async (docId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await axios.post(
    `/api/media/documents/${docId}/upload`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    }
  );
  
  return response.data.data;
};

// 分块上传
const chunkedUpload = async (
  docId: number,
  file: File,
  onProgress?: (percent: number) => void
) => {
  const CHUNK_SIZE = 5 * 1024 * 1024;
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  
  // 计算文件哈希（简化示例，实际项目使用 spark-md5）
  const fileHash = await calculateFileHash(file);
  
  // 初始化
  const initResponse = await axios.post(
    '/api/media/upload/init',
    {
      documentId: docId,
      filename: file.name,
      fileSize: file.size,
      mimeType: file.type,
      mediaType: file.type.startsWith('image/') ? 'image' : 'video',
      chunkSize: CHUNK_SIZE,
      fileHash: fileHash  // 必填
    },
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    }
  );
  
  const { uploadId } = initResponse.data.data;
  currentUploadId = uploadId;
  
  try {
    // 上传分块
    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE;
      const end = Math.min(start + CHUNK_SIZE, file.size);
      const chunk = file.slice(start, end);
      
      // 计算分块哈希
      const chunkHash = await calculateChunkHash(chunk);
      
      const formData = new FormData();
      formData.append('uploadId', uploadId);
      formData.append('chunkIndex', i.toString());
      formData.append('chunkHash', chunkHash);  // 必填
      formData.append('file', chunk);           // 注意：字段名是 file
      
      await axios.post('/api/media/upload/chunk', formData);
      
      if (onProgress) {
        onProgress(Math.round(((i + 1) / totalChunks) * 100));
      }
    }
    
    // 完成上传（只需要 uploadId）
    const completeResponse = await axios.post(
      '/api/media/upload/complete',
      {
        uploadId
        // fileHash 可选
      },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    return completeResponse.data.data;
    
  } catch (error) {
    // 取消上传
    if (currentUploadId) {
      await axios.post(
        '/api/media/upload/cancel',
        { uploadId: currentUploadId },
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
    }
    throw error;
  }
};

// 辅助函数：计算文件哈希
const calculateFileHash = async (file: File): Promise<string> => {
  const buffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};

// 辅助函数：计算分块哈希
const calculateChunkHash = async (chunk: Blob): Promise<string> => {
  const buffer = await chunk.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('MD5', buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};

// 取消上传
const cancelUploadHandler = async () => {
  if (currentUploadId) {
    await axios.post(
      '/api/media/upload/cancel',
      { uploadId: currentUploadId },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    uploading.value = false;
    uploadProgress.value = 0;
  }
};

// 加载媒体列表
const loadMediaList = async () => {
  const response = await axios.get(
    `/api/media/documents/${props.docId}/media`,
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    }
  );
  mediaList.value = response.data.data.list;
};

// 删除媒体
const deleteMedia = async (mediaId: number) => {
  if (!confirm('确定要删除这个文件吗？')) return;
  
  await axios.delete(
    `/api/media/${mediaId}`,
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    }
  );
  
  await loadMediaList();
};

// 导出 Markdown
const exportMarkdown = async () => {
  await exportDocument(props.docId, 'md');
};

// 导出 HTML
const exportHtml = async () => {
  await exportDocument(props.docId, 'html');
};

const exportDocument = async (docId: number, format: 'md' | 'html') => {
  const response = await axios.post(
    `/api/documents/${docId}/export`,
    { format },
    {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      responseType: 'blob'
    }
  );
  
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `document.${format}`);
  document.body.appendChild(link);
  link.click();
  link.remove();
};

onMounted(() => {
  loadMediaList();
});

onBeforeUnmount(() => {
  editor.value?.destroy();
});
</script>

<style scoped>
.document-editor {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.toolbar {
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
}

.editor-content {
  border: 1px solid #ddd;
  padding: 20px;
  min-height: 400px;
}

.upload-progress {
  margin: 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.media-list {
  margin-top: 20px;
}

.media-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.media-item img {
  max-width: 100px;
  max-height: 100px;
}

.media-item video {
  max-width: 200px;
}
</style>
```

---

## 注意事项

### 1. 文件大小限制

- 单文件最大：100MB（可配置）
- 建议超过 5MB 使用分块上传
- 视频文件建议使用分块上传

### 2. 错误处理

```typescript
// 全局错误拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Token 过期，跳转登录
      router.push('/login');
    } else if (error.response?.status === 413) {
      alert('文件太大，请使用分块上传');
    } else {
      console.error('请求失败:', error);
    }
    return Promise.reject(error);
  }
);
```

### 3. 断点续传

分块上传天然支持断点续传：
1. 上传中断后，记录已上传的分块索引
2. 重新上传时，跳过已成功的分块
3. 后端会返回 `uploadedChunks` 列表

**实现示例**：
```typescript
// 初始化时检查已上传的分块
const initResponse = await initChunkUpload({...});
const uploadedChunks = initResponse.uploadedChunks || [];

// 只上传未成功的分块
for (let i = 0; i < totalChunks; i++) {
  if (uploadedChunks.includes(i)) {
    console.log(`分块 ${i} 已上传，跳过`);
    continue;
  }
  // 上传分块...
}
```

### 4. 文件哈希计算

**重要**：后端要求提供文件和分块的哈希值用于校验。

**推荐库**：
- **spark-md5**：快速 MD5 计算（适合大文件）
- **Web Crypto API**：浏览器原生 API（无需额外依赖）

**使用 spark-md5 示例**：
```bash
npm install spark-md5
```

```typescript
import SparkMD5 from 'spark-md5';

// 计算文件哈希
function calculateFileHash(file: File): Promise<string> {
  return new Promise((resolve) => {
    const spark = new SparkMD5.ArrayBuffer();
    const reader = new FileReader();
    
    reader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer);
      resolve(spark.end());
    };
    
    reader.readAsArrayBuffer(file);
  });
}

// 计算分块哈希
function calculateChunkHash(chunk: Blob): Promise<string> {
  return new Promise((resolve) => {
    const spark = new SparkMD5.ArrayBuffer();
    const reader = new FileReader();
    
    reader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer);
      resolve(spark.end());
    };
    
    reader.readAsArrayBuffer(chunk);
  });
}
```

### 5. Markdown 语法

后端支持标准 CommonMark 语法：
- 标题、粗体、斜体
- 链接、图片
- 代码块、引用
- 列表、表格

导出的 HTML 包含完整样式，可直接在浏览器中打开。

### 6. 性能优化

- 图片上传前可以压缩
- 视频上传前可以生成缩略图
- 使用虚拟滚动展示大量媒体文件
- 懒加载媒体文件

### 7. 安全性

- 所有接口都需要 JWT Token
- 文件类型白名单验证
- 文件大小限制
- XSS 防护（Markdown 渲染时注意）

---

## 快速开始 checklist

- [ ] 安装依赖（Axios、TipTap/Editor.js）
- [ ] 配置 API Base URL
- [ ] 实现 JWT Token 管理
- [ ] 实现简单上传功能
- [ ] 实现分块上传功能
- [ ] 添加上传进度显示
- [ ] 集成 Markdown 编辑器
- [ ] 实现媒体列表展示
- [ ] 实现文档导出功能
- [ ] 测试各种场景（小文件、大文件、中断续传）

---

## 相关资源

- [后端 API 文档](../接口文档.md)
- [Markdown 支持说明](../Markdown支持说明.md)
- [TipTap 官方文档](https://tiptap.dev/)
- [Editor.js 官方文档](https://editorjs.io/)
- [Axios 官方文档](https://axios-http.com/)

---

**有问题？** 联系后端开发团队或查看后端日志排查问题。
