/**
 * WebSocket 客户端工具类
 * 
 * ⚠️ 重要：消息类型命名规范
 * - 所有消息类型必须使用小写 + 下划线格式（snake_case）
 * - 例如：'chat_message', 'operation_broadcast', 'user_joined'
 * - 不要使用大写格式（如 'CHAT_MESSAGE'）或驼峰格式（如 'chatMessage'）
 */

class WebSocketClient {
  private ws: WebSocket | null = null;
  private messageHandlers: Map<string, Function> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectTimer: number | null = null;

  /**
   * 注册消息处理器
   * @param type 消息类型（自动转换为小写）
   * @param handler 处理函数
   */
  on(type: string, handler: (data: any) => void) {
    // ✅ 自动转换为小写，确保一致性
    this.messageHandlers.set(type.toLowerCase(), handler);
  }

  /**
   * 取消注册消息处理器
   */
  off(type: string) {
    this.messageHandlers.delete(type.toLowerCase());
  }

  /**
   * 连接 WebSocket
   */
  connect(token: string, docId: string, userId: number) {
    const wsUrl = `ws://localhost:8080/api/ws?token=${token}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('✅ WebSocket 连接成功');
      this.reconnectAttempts = 0;
      
      // 发送加入文档消息
      this.send({
        type: 'join_document',  // ✅ 小写下划线
        documentId: docId,
        userId: userId,
        data: {
          cursorPosition: 0,
          clientInfo: {
            platform: 'web',
            version: '1.0.0'
          }
        }
      });
    };

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        console.log('📨 收到消息:', message.type, message);
        
        // 分发消息到对应的处理器
        this.handleMessage(message);
      } catch (error) {
        console.error('❌ 解析消息失败:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('❌ WebSocket 错误:', error);
    };

    this.ws.onclose = (event) => {
      console.log('WebSocket 关闭:', event.code, event.reason);
      this.attemptReconnect(token, docId, userId);
    };
  }

  /**
   * 发送消息
   * @param message 消息对象
   */
  send(message: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      // ✅ 自动将 type 转换为小写
      if (message.type) {
        message.type = message.type.toLowerCase();
      }
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('⚠️ WebSocket 未连接，readyState:', this.ws?.readyState);
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    
    this.messageHandlers.clear();
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: any) {
    // ✅ 使用小写匹配
    const handler = this.messageHandlers.get(message.type.toLowerCase());
    if (handler) {
      handler(message.data);
    } else {
      console.warn('⚠️ 未注册的消息处理器:', message.type);
      console.warn('已注册的处理器:', Array.from(this.messageHandlers.keys()));
    }
  }

  /**
   * 尝试重连
   */
  private attemptReconnect(token: string, docId: string, userId: number) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      
      console.log(`${delay}ms 后重连... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      this.reconnectTimer = window.setTimeout(() => {
        this.connect(token, docId, userId);
      }, delay);
    } else {
      console.error('❌ WebSocket 重连失败，已达到最大重试次数');
    }
  }

  /**
   * 获取连接状态
   */
  get readyState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED;
  }

  /**
   * 是否已连接
   */
  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }
}

// 导出单例
export default new WebSocketClient();
