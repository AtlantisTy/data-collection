package com.demo.iotcollect.filter;

import com.demo.iotcollect.model.DataRecord;

/**
 * 数据过滤策略接口（预留扩展点）。
 * 当前用 {@link NoOpFilter}（透传），后续可加死区过滤 DeadbandFilter：
 * 仅当与上次上报值之差超过阈值(点位 deadband)时放行，抑制重复数据、降低入库与 Kafka 压力。
 */
public interface FilterStrategy {

    /**
     * 决定一条采样是否放行（true=上报/入库，false=丢弃）。
     * 实现可持有跨分支的上报前值状态。
     */
    boolean pass(DataRecord record);
}