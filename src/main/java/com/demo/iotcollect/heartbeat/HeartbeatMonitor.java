package com.demo.iotcollect.heartbeat;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DevicePoint;
import com.demo.iotcollect.web.LiveHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备心跳监测：按设备（device）维度汇总。
 *
 * <p>以"最后一次成功读到数据的时间"作为心跳依据（见 {@link #recordSuccess}）。
 * 定时任务周期检查：某设备最后一次成功读数距今超过 {@code heartbeatTimeoutMs} 时判定离线并推送；
 * 恢复成功读取时切换回在线并推送。ON/OFFLINE 切换仅经 WebSocket 推送，不入库。
 */
@Component
public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);

    private final ModbusProperties props;
    private final LiveHub liveHub;

    /** device -> 最后一次成功读数据的时间 */
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();
    /** 当前处于在线状态的设备 */
    private final Set<String> onlineDevices = ConcurrentHashMap.newKeySet();

    public HeartbeatMonitor(ModbusProperties props, LiveHub liveHub) {
        this.props = props;
        this.liveHub = liveHub;
    }

    /** 设备某点位成功读到数据时调用 */
    public void recordSuccess(String device, Instant ts) {
        boolean firstOnline = !onlineDevices.contains(device);
        lastSeen.put(device, ts);
        if (firstOnline) {
            onlineDevices.add(device);
            liveHub.pushHeartbeat(device, "ONLINE", ts);
            log.info("设备 {} 上线", device);
        }
    }

    /** 周期检查离线：仅对曾在线（或已上报成功）的设备发生 ONLINE→OFFLINE 切换时才推送 */
    @Scheduled(fixedDelayString = "${app.modbus.heartbeat-timeout-ms:5000}")
    public void check() {
        Instant now = Instant.now();
        long timeoutMs = props.getHeartbeatTimeoutMs();
        for (DevicePoint p : props.getPoints()) {
            String device = p.getDevice();
            Instant last = lastSeen.get(device);
            if (last == null || !onlineDevices.contains(device)) {
                continue;
            }
            if (Duration.between(last, now).toMillis() >= timeoutMs) {
                onlineDevices.remove(device);
                liveHub.pushHeartbeat(device, "OFFLINE", now);
                log.warn("设备 {} 离线（{} 内无成功读数）", device, Duration.between(last, now));
            }
        }
    }
}