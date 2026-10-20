package com.demo.iotcollect.collector;

import com.demo.iotcollect.config.ModbusProperties;
import com.demo.iotcollect.model.DevicePoint;
import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Modbus TCP 主站客户端封装（digitalpetri modbus 2.1.6）。
 * 惰性连接 + 库内自动重连；按点位数据类型将响应 byte[]（大端）解析为工程值。
 */
@Service
public class ModbusClientService {

    private static final Logger log = LoggerFactory.getLogger(ModbusClientService.class);

    private final ModbusProperties props;
    private volatile ModbusTcpClient client;
    private volatile AutoCloseable lease;

    public ModbusClientService(ModbusProperties props) {
        this.props = props;
    }

    /** 读取单点位当前工程值；从站不可达时抛异常由调度层捕获 */
    public double readValue(DevicePoint point) {
        ModbusTcpClient c = getOrConnect();
        try {
            ReadHoldingRegistersRequest req =
                    new ReadHoldingRegistersRequest(point.getAddress(), point.getQuantity());
            ReadHoldingRegistersResponse resp = c.readHoldingRegisters(point.getSlaveId(), req);
            return decode(resp.registers(), point.getDataType()) * point.getScale();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "读取点位 " + point + " 失败: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    private ModbusTcpClient getOrConnect() {
        ModbusTcpClient c = client;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (client == null) {
                try {
                    log.info("连接 Modbus 从站 {}:{} ...", props.getHost(), props.getPort());
                    NettyTcpClientTransport transport = NettyTcpClientTransport.create(cfg ->
                            cfg.setHostname(props.getHost()).setPort(props.getPort()));
                    c = ModbusTcpClient.create(transport);
                    lease = c.open(); // 连接建立失败会抛异常
                    client = c;       // 库内自动重连，client 可长期复用
                    log.info("Modbus 从站连接成功");
                } catch (Exception e) {
                    log.warn("Modbus 从站连接失败，稍后重试: {}", e.getMessage());
                    throw new IllegalStateException("连接 Modbus 从站失败: " + e.getMessage(), e);
                }
            }
            return client;
        }
    }

    private double decode(byte[] bytes, DevicePoint.DataType type) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("响应寄存器为空");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        return switch (type) {
            case FLOAT -> buf.getFloat();
            case INT32 -> buf.getInt();
            case INT16 -> buf.getShort();
            case UINT16 -> Short.toUnsignedInt(buf.getShort());
        };
    }

    @PreDestroy
    public void shutdown() {
        if (lease != null) {
            try {
                lease.close();
            } catch (Exception ignored) {
            }
            lease = null;
            client = null;
            log.info("Modbus 客户端已关闭");
        }
    }
}