package com.demo.iotcollect.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB2 客户端装配。
 * 仅 influx.enabled=true 时创建；否则不装配，入库层自动降级为 no-op。
 */
@Configuration
@ConditionalOnProperty(name = "influx.enabled", havingValue = "true")
public class InfluxConfig {

    private final InfluxProperties props;

    @Autowired
    public InfluxConfig(InfluxProperties props) {
        this.props = props;
    }

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(props.getUrl(), props.getToken().toCharArray(),
                props.getOrg(), props.getBucket());
    }
}