package com.demo.iotcollect.config;

import com.demo.iotcollect.model.DevicePoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Modbus 采集配置，绑定 application.yml 下的 app.modbus 前缀。
 */
@ConfigurationProperties(prefix = "app.modbus")
public class ModbusProperties {

    /** 从站 host（本 demo 为 Modbus Slave TcpServer 地址） */
    private String host = "127.0.0.1";
    /** 从站端口 */
    private int port = 1502;
    /** 轮询周期（毫秒） */
    private int pollIntervalMs = 1000;
    /** 单次请求超时（毫秒） */
    private int timeoutMs = 3000;
    /** 点位表 */
    private List<DevicePoint> points = new ArrayList<>();

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(int pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public List<DevicePoint> getPoints() { return points; }
    public void setPoints(List<DevicePoint> points) { this.points = points; }
}