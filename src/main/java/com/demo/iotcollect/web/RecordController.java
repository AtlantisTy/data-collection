package com.demo.iotcollect.web;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DevicePoint;
import com.demo.iotcollect.store.InfluxDataStore;
import com.demo.iotcollect.store.TimeSeriesPoint;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 历史数据查询接口。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecordController {

    private final ModbusProperties props;
    private final InfluxDataStore store;

    public RecordController(ModbusProperties props, InfluxDataStore store) {
        this.props = props;
        this.store = store;
    }

    /** 点位知识点位表（前端下拉用） */
    @GetMapping("/points")
    public List<DevicePoint> points() {
        return props.getPoints();
    }

    /** 查询某点位历史序列：/api/records?device=..&point=..&from=ISO&to=ISO */
    @GetMapping("/records")
    public List<TimeSeriesPoint> records(
            @RequestParam String device,
            @RequestParam String point,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant start = from != null ? from : Instant.now().minusSeconds(3600);
        Instant end = to != null ? to : Instant.now();
        return store.query(device, point, start, end);
    }
}