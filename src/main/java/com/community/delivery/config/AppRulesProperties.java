package com.community.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/** 业务规则参数（application.yml 中 app.rules，可被小区级 PropertyRule 覆盖夜间窗口） */
@ConfigurationProperties(prefix = "app.rules")
public class AppRulesProperties {

    /** 用户超时不取阈值（秒） */
    private long pickupTimeoutSeconds = 900;

    /** 冷藏订单等待超时阈值（秒），短于取件超时：冷链风险先升级人工 */
    private long coldChainTimeoutSeconds = 600;

    /** 低电量阈值（%） */
    private int lowBatteryThreshold = 20;

    /** 避障基础停留时长（秒） */
    private long blockingWaitSeconds = 30;

    private LocalTime nightStart = LocalTime.of(22, 0);
    private LocalTime nightEnd = LocalTime.of(6, 0);

    public long getPickupTimeoutSeconds() { return pickupTimeoutSeconds; }
    public void setPickupTimeoutSeconds(long v) { this.pickupTimeoutSeconds = v; }
    public long getColdChainTimeoutSeconds() { return coldChainTimeoutSeconds; }
    public void setColdChainTimeoutSeconds(long v) { this.coldChainTimeoutSeconds = v; }
    public int getLowBatteryThreshold() { return lowBatteryThreshold; }
    public void setLowBatteryThreshold(int v) { this.lowBatteryThreshold = v; }
    public long getBlockingWaitSeconds() { return blockingWaitSeconds; }
    public void setBlockingWaitSeconds(long v) { this.blockingWaitSeconds = v; }
    public LocalTime getNightStart() { return nightStart; }
    public void setNightStart(LocalTime v) { this.nightStart = v; }
    public LocalTime getNightEnd() { return nightEnd; }
    public void setNightEnd(LocalTime v) { this.nightEnd = v; }
}
