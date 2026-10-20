package com.demo.iotcollect.collector;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DataRecord;
import com.demo.iotcollect.model.DevicePoint;
import com.demo.iotcollect.filter.FilterStrategy;
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

    public CollectScheduler(ModbusProperties props, ModbusClientService modbus,
                            DataProducer producer, InfluxDataStore store, LiveHub liveHub,
                            FilterStrategy filter) {
        this.props = props;
        this.modbus = modbus;
        this.producer = producer;
        this.store = store;
        this.liveHub = liveHub;
        this.filter = filter;
    }

    @Scheduled(fixedDelayString = "${app.modbus.poll-interval-ms:1000}")
    public void collect() {
        List<DevicePoint> points = props.getPoints();
        if (points == null || points.isEmpty()) {
            return;
        }
        for (DevicePoint point : points) {
            try {
                double value = modbus.readValue(point);
                DataRecord record = new DataRecord(point.getDevice(), point.getName(),
                        value, point.getUnit(), Instant.now());
                // 过滤（首版透传，后续死区过滤在此拦截）
                if (filter.pass(record)) {
                    producer.publish(record);  // 出口：预留 Kafka 缓冲位
                    store.write(record);       // 入库：InfluxDB2
                    liveHub.broadcast(record); // 实时推送：前端曲线
                }
            } catch (Exception e) {
                // 单个点/从站失败不影响其它点
                log.warn("采集跳过点位 {}: {}", point, e.getMessage());
            }
        }
    }
}