package com.demo.iotcollect.filter;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DataRecord;
import com.demo.iotcollect.model.DevicePoint;
import com.demo.iotcollect.model.Quality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死区过滤：仅当数值变化超过点位设定的 deadband 才放行，抑制死区内冗余采样，降低入库与推送压力。
 *
 * <p>强制上报（不予死区抑制）：
 * <ol>
 *   <li>质量 BAD —— 坏值必须上报给上层。</li>
 *   <li>deadband 未配置（null）或小于等于 0 —— 视为不过滤。</li>
 *   <li>上次为 BAD 后恢复 GOOD —— 状态变化即刻上报；以及该点位首次上报。</li>
 * </ol>
 * 其余情况按 |新值 - 上次上报值| >= deadband 决定放行。
 */
@Component
@Primary
public class DeadbandFilter implements FilterStrategy {

    private static final Logger log = LoggerFactory.getLogger(DeadbandFilter.class);

    private final ModbusProperties props;
    /** metric(device_name) -> 上次放行的工程值 */
    private final Map<String, Double> lastGoodValue = new ConcurrentHashMap<>();
    /** metric -> 上次记录的最近质量（用于 BAD→GOOD 恢复检测） */
    private final Map<String, Quality> lastQuality = new ConcurrentHashMap<>();

    public DeadbandFilter(ModbusProperties props) {
        this.props = props;
    }

    @Override
    public boolean pass(DataRecord record) {
        String metric = record.metric();
        Quality quality = record.quality();

        // 1. 坏值必须上报，不参与死区抑制
        if (quality == Quality.BAD) {
            lastQuality.put(metric, Quality.BAD);
            return true;
        }

        Double deadband = deadbandOf(record);
        // 2. 未配置或非正值视为不过滤
        if (deadband == null || deadband <= 0) {
            lastGoodValue.put(metric, record.value());
            lastQuality.put(metric, Quality.GOOD);
            return true;
        }

        // 3. 首采或 BAD→GOOD 恢复：强制上报
        Double last = lastGoodValue.get(metric);
        if (last == null || lastQuality.get(metric) != Quality.GOOD) {
            lastGoodValue.put(metric, record.value());
            lastQuality.put(metric, Quality.GOOD);
            return true;
        }

        double delta = Math.abs(record.value() - last);
        if (delta >= deadband) {
            lastGoodValue.put(metric, record.value());
            lastQuality.put(metric, Quality.GOOD);
            return true;
        }

        log.debug("死区抑制 {}({})：变化 {} 小于 deadband {}", record.device(), record.name(), delta, deadband);
        return false;
    }

    /** 按 metric 查找点位死区阈值 */
    private Double deadbandOf(DataRecord record) {
        String metric = record.device() + "_" + record.name();
        for (DevicePoint p : props.getPoints()) {
            if (metric.equals(p.getDevice() + "_" + p.getName())) {
                return p.getDeadband();
            }
        }
        return null;
    }
}