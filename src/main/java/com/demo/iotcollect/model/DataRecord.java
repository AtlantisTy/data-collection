package com.demo.iotcollect.model;

import java.time.Instant;

/**
 * 单次采样记录。质量 BAD 时 value 为 NaN。
 */
public record DataRecord(
        String device,
        String name,
        double value,
        String unit,
        Instant timestamp,
        Quality quality
) {

    /** 指标唯一标识，用于 Influx measurement / 前端序列名 */
    public String metric() {
        return device + "_" + name;
    }
}