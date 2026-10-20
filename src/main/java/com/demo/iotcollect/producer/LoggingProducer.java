package com.demo.iotcollect.producer;

import com.demo.iotcollect.model.DataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 首版数据出口：仅打印日志，用于验证采集链路。
 * 后续可替换为 KafkaProducer（预留 KafkaTemplate 入口 + app.kafka.enabled 开关）。
 */
public class LoggingProducer implements DataProducer {

    private static final Logger log = LoggerFactory.getLogger(LoggingProducer.class);

    @Override
    public void publish(DataRecord record) {
        log.info(">> sample | device={}, point={}, value={} {}, time={}",
                record.device(), record.name(), record.value(), record.unit(), record.timestamp());
    }
}