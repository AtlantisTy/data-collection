package com.demo.iotcollect.producer;

import com.demo.iotcollect.model.DataRecord;

/**
 * 数据出口抽象：采集链路末端。
 * 首版用 {@link LoggingProducer} 打到日志；后续可新增 KafkaProducer 实现缓冲削峰。
 */
public interface DataProducer {

    /** 推送一条采样记录到下游 */
    void publish(DataRecord record);
}