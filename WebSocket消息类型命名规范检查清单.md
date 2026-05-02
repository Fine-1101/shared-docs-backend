# WebSocket 消息类型命名规范检查清单

## 📋 开发前必读

### 核心规则
**所有 WebSocket 消息类型必须使用小写 + 下划线格式（snake_case）**

---

## ✅ 后端检查清单

### MessageType 枚举
- [ ] 每个枚举项的 `code` 字段使用小写下划线（如 `"chat_message"`）
- [ ] `@JsonValue` 返回的是 `code` 字段（小写）
- [ ] `@JsonCreator` 方法兼容大写和小写两种输入
- [ ] 枚举类注释中明确说明命名规范

### Handler 实现
- [ ] `getSupportedMessageType()` 返回小写下划线格式
- [ ] 日志输出中包含消息类型，便于调试
- [ ] 广播消息时使用正确的 MessageType 枚举

### 示例代码
```java
public enum MessageType {
    CHAT_MESSAGE("chat_message", "聊天消息"),  // ✅ code 是小写
    CHAT_BROADCAST("chat_broadcast", "聊天广播");
    
    @JsonValue
    public String getCode() {
        return code;  // ✅ 返回小写
    }
}

@Component
public class ChatMessageHandler implements MessageHandler {
    @Override
    public String getSupportedMessageType() {
        return "chat_message";  // ✅ 小写下划线
    }
}
```

---

## ✅ 前端检查清单

### WebSocket 客户端
- [ ] `send()` 方法自动将 `type` 转换为小写
- [ ] `on()` 方法注册时使用小写键名
- [ ] `handleMessage()` 使用小写匹配处理器
- [ ] 类注释中明确说明命名规范

### 消息发送
- [ ] 所有 `wsClient.send()` 调用中的 `type` 是小写下划线
- [ ] 没有使用大写格式（如 `'CHAT_MESSAGE'`）
- [ ] 没有使用驼峰格式（如 `'chatMessage'`）

### 消息接收
- [ ] 所有 `wsClient.on()` 调用中的类型是小写下划线
- [ ] 监听的消息类型与后端发送的一致
- [ ] 控制台有未注册处理器的警告日志

### 示例代码
```typescript
// ✅ 正确
wsClient.send({
  type: 'chat_message',  // 小写下划线
  documentId: '7',
  data: { content: 'Hello' }
});

wsClient.on('chat_broadcast', (data) => {
  console.log('收到消息:', data);
});

// ❌ 错误
wsClient.send({
  type: 'CHAT_MESSAGE',  // 大写 - 错误！
  ...
});

wsClient.on('CHAT_BROADCAST', handler);  // 大写 - 错误！
```

---

## ✅ 文档检查清单

### API 文档
- [ ] 所有消息类型示例使用小写下划线
- [ ] 有明确的命名规范说明章节
- [ ] 提供了常见错误对照表
- [ ] TypeScript 接口定义中的注释使用小写

### 前端开发计划书
- [ ] WebSocket 消息类型列表使用小写
- [ ] 代码示例中的 type 字段是小写
- [ ] 有"重要：消息类型命名规范"提示框
- [ ] 提供了完整的 WebSocketClient 实现示例

---

## 🧪 测试检查清单

### 功能测试
- [ ] 两个浏览器窗口可以实时看到对方的聊天消息
- [ ] 两个浏览器窗口可以实时看到对方的编辑操作
- [ ] 控制台没有"未注册的消息处理器"警告
- [ ] 后端日志显示消息已正确广播

### 调试步骤
1. 打开两个浏览器窗口，登录不同用户
2. 加入同一个文档
3. 在一个窗口发送聊天消息
4. 检查另一个窗口是否立即收到
5. 查看浏览器控制台的网络标签（WS）
6. 查看后端日志的广播信息

---

## ❌ 常见错误及修复

### 错误 1: 前端监听大写格式
```typescript
// ❌ 错误
wsClient.on('CHAT_BROADCAST', handler);

// ✅ 修复
wsClient.on('chat_broadcast', handler);
```

### 错误 2: 前端发送大写格式
```typescript
// ❌ 错误
wsClient.send({ type: 'OPERATION', ... });

// ✅ 修复
wsClient.send({ type: 'operation', ... });
```

### 错误 3: 文档中使用大写格式
```markdown
<!-- ❌ 错误 -->
- CHAT_BROADCAST - 聊天广播

<!-- ✅ 修复 -->
- chat_broadcast - 聊天广播
```

### 错误 4: 后端 code 字段使用大写
```java
// ❌ 错误
CHAT_MESSAGE("CHAT_MESSAGE", "聊天消息")

// ✅ 修复
CHAT_MESSAGE("chat_message", "聊天消息")
```

---

## 📚 参考文件

### 后端
- `src/main/java/org/example/shareddocs/common/enums/MessageType.java`
- `src/main/java/org/example/shareddocs/websocket/handler/ChatMessageHandler.java`
- `src/main/java/org/example/shareddocs/websocket/handler/OperationHandler.java`

### 前端
- `src/main/resources/templates/websocket-client-template.ts`
- `前端开发项目计划书.md` 第 112-139 行、593-685 行

### 数据库
- `src/main/resources/db/create_chat_messages_table.sql`

---

## 🎯 快速验证命令

### 检查后端消息类型
```bash
grep -r "getCode()" src/main/java/org/example/shareddocs/common/enums/MessageType.java
```

### 检查前端消息发送
```bash
grep -r "type:" src/**/*.ts | grep -E "(CHAT_|OPERATION_|USER_)"
```

### 检查文档规范性
```bash
grep -E "CHAT_BROADCAST|OPERATION_BROADCAST" 前端开发项目计划书.md
```

---

## 💡 最佳实践建议

1. **在 WebSocketClient 中自动转换大小写**，提供容错能力
2. **在文档开头醒目位置标注命名规范**
3. **提供完整的示例代码**，而不是只写接口定义
4. **添加运行时警告**，当检测到不规范的消息类型时输出警告
5. **编写单元测试**，验证消息类型的序列化/反序列化

---

最后更新：2026-05-02
