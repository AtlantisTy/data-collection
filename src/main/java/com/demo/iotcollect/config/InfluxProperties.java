package com.demo.iotcollect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * InfluxDB2 连接配置，绑定 application.yml 下 influx 前缀。
 */
@ConfigurationProperties(prefix = "influx")
public class InfluxProperties {

    /** 是否启用入库（false 时跳过写入/查询返回空） */
    private boolean enabled = false;
    private String url = "http://127.0.0.1:8086";
    private String token = "";
    private String org = "demo";
    private String bucket = "iot";
    /** 保留策略时长，供 Flux range 默认窗口用 */
    private int retentionHours = 24;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getOrg() { return org; }
    public void setOrg(String org) { this.org = org; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public int getRetentionHours() { return retentionHours; }
    public void setRetentionHours(int retentionHours) { this.retentionHours = retentionHours; }
}