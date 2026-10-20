package com.demo.iotcollect.config;

import com.demo.iotcollect.web.LiveHub;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册：实时数据推送通道 /ws/live。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveHub liveHub;

    public WebSocketConfig(LiveHub liveHub) {
        this.liveHub = liveHub;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveHub, "/ws/live").setAllowedOriginPatterns("*");
    }
}