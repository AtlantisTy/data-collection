package com.demo.iotcollect.web;

import com.demo.iotcollect.model.DataRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时推送 Hub：采集到新数据时向所有订阅前端广播。
 */
@Component
public class LiveHub extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper;

    public LiveHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** 广播一条采样记录 */
    public void broadcast(DataRecord record) {
        try {
            send(mapper.writeValueAsString(record));
        } catch (Exception e) {
            // 序列化失败不必中断采集
        }
    }

    /** 广播设备心跳状态事件，如 {type:HEARTBEAT, device, status, timestamp} */
    public void pushHeartbeat(String device, String status, java.time.Instant ts) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "type", "HEARTBEAT",
                    "device", device,
                    "status", status,
                    "timestamp", ts.toString()));
            send(json);
        } catch (Exception e) {
            // 序列化失败不必中断
        }
    }

    private void send(String json) {
        if (sessions.isEmpty() || json == null) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}