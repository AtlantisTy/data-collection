package com.demo.iotcollect.collector;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DataRecord;
import com.demo.iotcollect.model.DevicePoint;
import com.demo.iotcollect.filter.FilterStrategy;
import com.demo.iotcollect.heartbeat.HeartbeatMonitor;
import com.demo.iotcollect.model.Quality;
import com.demo.iotcollect.producer.DataProducer;
import com.demo.iotcollect.store.InfluxDataStore;
import com.demo.iotcollect.web.LiveHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 定时采集调度：按轮询周期遍历点位表，读取后经 producer 出口并写入时序库。
 */
@Component
public class CollectScheduler {

    private static final Logger log = LoggerFactory.getLogger(CollectScheduler.class);

    private final ModbusProperties props;
    private final ModbusClientService modbus;
    private final DataProducer producer;
    private final InfluxDataStore store;
    private final LiveHub liveHub;
    private final FilterStrategy filter;
    private final HeartbeatMonitor heartbeat;

    public CollectScheduler(ModbusProperties props, ModbusClientService modbus,
                            DataProducer producer, InfluxDataStore store, LiveHub liveHub,
                            FilterStrategy filter, HeartbeatMonitor heartbeat) {
        this.props = props;
        this.modbus = modbus;
        this.producer = producer;
        this.store = store;
        this.liveHub = liveHub;
        this.filter = filter;
        this.heartbeat = heartbeat;
    }

    @Scheduled(fixedDelayString = "${app.modbus.poll-interval-ms:1000}")
    public void collect() {
        List<DevicePoint> points = props.getPoints();
        if (points == null || points.isEmpty()) {
            return;
        }
        for (DevicePoint point : points) {
            Instant now = Instant.now();
            String device = point.getDevice();
            try {
                double value = modbus.readValue(point);
                heartbeat.recordSuccess(device, now);
                DataRecord record = new DataRecord(device, point.getName(),
                        value, point.getUnit(), now, Quality.GOOD);
                // 死区过滤：GOOD 且变化未超阈值时抑制
                if (filter.pass(record)) {
                    emit(record);
                }
            } catch (Exception e) {
                // 失败仍要上报 BAD 记录（质量可见），且绕过死区直接出口；不更新心跳
                DataRecord bad = new DataRecord(device, point.getName(),
                        Double.NaN, point.getUnit(), now, Quality.BAD);
                try {
                    emit(bad);
                } catch (Exception ex) {
                    log.warn("上报 BAD 记录失败 {}: {}", point, ex.getMessage());
                }
                // 单个点/从站失败不影响其它点
                log.warn("采集失败点位 {}: {}", point, e.getMessage());
            }
        }
    }

    /** 统一出口：发布（预留 Kafka）、入库、实时推送 */
    private void emit(DataRecord record) {
        producer.publish(record);  // 出口：预留 Kafka 缓冲位
        store.write(record);       // 入库：InfluxDB2
        liveHub.broadcast(record); // 实时推送：前端曲线
    }
}