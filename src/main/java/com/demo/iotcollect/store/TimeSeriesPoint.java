package com.demo.iotcollect.store;

import java.time.Instant;

/**
 * 单条历史时序点，供前端曲线使用。
 */
public record TimeSeriesPoint(Instant time, double value) {
}