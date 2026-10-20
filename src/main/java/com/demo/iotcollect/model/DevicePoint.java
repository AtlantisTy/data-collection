package com.demo.iotcollect.model;

/**
 * 采集点位定义，来自 application.yml 的点位表。
 */
public class DevicePoint {

    /** 设备/测量名，例如 temperature-01 */
    private String device = "default";
    /** 点位名，例如 temperature */
    private String name;
    /** Modbus 从站 unitId */
    private int slaveId = 1;
    /** 寄存器起始地址 */
    private int address;
    /** 寄存器数量（16bit 寄存器个数） */
    private int quantity = 1;
    /** 数据类型 */
    private DataType dataType = DataType.INT16;
    /** 工程换算系数（原始值 * scale） */
    private double scale = 1.0;
    /** 工程单位 */
    private String unit = "";
    /** 死区阈值（绝对值），预留后续过滤 */
    private Double deadband = null;

    // ---- getters / setters ----

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSlaveId() { return slaveId; }
    public void setSlaveId(int slaveId) { this.slaveId = slaveId; }

    public int getAddress() { return address; }
    public void setAddress(int address) { this.address = address; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public DataType getDataType() { return dataType; }
    public void setDataType(DataType dataType) { this.dataType = dataType; }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getDeadband() { return deadband; }
    public void setDeadband(Double deadband) { this.deadband = deadband; }

    /** 数据类型与占用寄存器数量 */
    public enum DataType {
        INT16(1, 1),
        UINT16(1, 1),
        INT32(2, 2),
        FLOAT(2, 4);

        public final int registerCount;
        public final int byteCount;

        DataType(int registerCount, int byteCount) {
            this.registerCount = registerCount;
            this.byteCount = byteCount;
        }
    }

    @Override
    public String toString() {
        return "DevicePoint{" + device + ":" + name +
                ", slaveId=" + slaveId + ", address=" + address +
                ", type=" + dataType + ", scale=" + scale + ", unit='" + unit + "'}";
    }
}