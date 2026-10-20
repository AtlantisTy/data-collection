package com.demo.iotcollect.config;

import com.demo.iotcollect.producer.DataProducer;
import com.demo.iotcollect.producer.LoggingProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据出口装配：首版用日志实现；后续接入 Kafka 时替换这里的实现并加 app.kafka.enabled 开关。
 */
@Configuration
public class ProducerConfig {

    @Bean
    public DataProducer dataProducer() {
        return new LoggingProducer();
    }
}