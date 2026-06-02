package org.example.shareddocs.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 心跳确认数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatAckData {
    
    /**
     * 服务端时间戳
     */
    private Long serverTime;
}
