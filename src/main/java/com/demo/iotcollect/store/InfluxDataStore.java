package com.demo.iotcollect.store;

import com.demo.iotcollect.config.InfluxProperties;
import com.demo.iotcollect.model.DataRecord;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * InfluxDB2 时序存储。
 * influx.enabled=false 或未配置端到端时，写入/查询自动降级为空操作。
 */
@Service
public class InfluxDataStore {

    private static final Logger log = LoggerFactory.getLogger(InfluxDataStore.class);
    private static final String MEASUREMENT = "telemetry";

    private final InfluxProperties props;
    private final InfluxDBClient client;
    private WriteApiBlocking writeApi;

    public InfluxDataStore(InfluxProperties props, ObjectProvider<InfluxDBClient> clientProvider) {
        this.props = props;
        this.client = props.isEnabled() ? clientProvider.getIfAvailable() : null;
        if (props.isEnabled() && this.client == null) {
            log.warn("influx.enabled=true 但未装配 InfluxDBClient，入库降级为跳过");
        }
    }

    private boolean usable() {
        return client != null;
    }

    private WriteApiBlocking writeApi() {
        if (writeApi == null) {
            writeApi = client.getWriteApiBlocking();
        }
        return writeApi;
    }

    /** 写入一条采样记录 */
    public void write(DataRecord r) {
        if (!usable()) {
            return;
        }
        Point p = Point.measurement(MEASUREMENT)
                .addTag("device", r.device())
                .addTag("point", r.name())
                .addField("value", r.value())
                .time(r.timestamp(), WritePrecision.MS);
        try {
            writeApi().writePoint(p);
        } catch (Exception e) {
            log.warn("时序写入失败 {}: {}", r.metric(), e.getMessage());
        }
    }

    /** 查询某点位历史序列 */
    public List<TimeSeriesPoint> query(String device, String point, Instant from, Instant to) {
        if (!usable()) {
            return List.of();
        }
        String flux = "from(bucket: \"" + esc(props.getBucket()) + "\")"
                + " |> range(start: " + rfc(from) + ", stop: " + rfc(to) + ")"
                + " |> filter(fn: (r) => r._measurement == \"" + MEASUREMENT + "\""
                + " and r.device == \"" + esc(device) + "\""
                + " and r.point == \"" + esc(point) + "\")"
                + " |> sort(columns: [\"_time\"])";

        List<TimeSeriesPoint> out = new ArrayList<>();
        try {
            QueryApi query = client.getQueryApi();
            List<FluxTable> tables = query.query(flux);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Object time = record.getTime();
                    Object value = record.getValue();
                    if (time != null && value instanceof Number n) {
                        out.add(new TimeSeriesPoint(toInstant(time), n.doubleValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("时序查询失败: {} [{}]", e.getMessage(), e.getClass().getSimpleName());
            return List.of();
        }
        return out;
    }

    /** 兼容 influxdb-client 可能返回的多种时间类型 */
    private static Instant toInstant(Object t) {
        if (t instanceof Instant i) {
            return i;
        }
        if (t instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (t instanceof java.time.ZonedDateTime zdt) {
            return zdt.toInstant();
        }
        if (t instanceof java.time.temporal.TemporalAccessor ta) {
            return Instant.from(ta);
        }
        throw new IllegalStateException("无法解析时间为 Instant: " + t);
    }

    private static String rfc(Instant t) {
        // Flux range 的时间字面量是不带引号的 RFC3339：如 2026-10-19T07:04:00Z
        return t.toString();
    }

    private static String esc(String s) {
        return s.replace("\"", "\\\"");
    }
}