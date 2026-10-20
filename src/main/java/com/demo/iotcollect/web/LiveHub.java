package com.demo.iotcollect.web;

import com.demo.iotcollect.model.DataRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void broadcast(DataRecord record) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(record);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try {
                        s.sendMessage(msg);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            // 序列化失败不必中断采集
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