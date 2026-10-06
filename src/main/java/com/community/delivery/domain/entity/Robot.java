package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.RobotStatus;
import jakarta.persistence.*;

import java.time.Instant;

/** 配送机器人 */
@Entity
@Table(name = "robot")
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** 当前所在小区；空闲待命时也可能为 null */
    @Column
    private Long currentCommunityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RobotStatus status = RobotStatus.IDLE;

    /** 电量百分比 0-100 */
    @Column(nullable = false)
    private int batteryPercent = 100;

    /** 冷藏载物仓数量上限 */
    @Column(nullable = false)
    private int coldBinCapacity = 2;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getCurrentCommunityId() { return currentCommunityId; }
    public void setCurrentCommunityId(Long currentCommunityId) { this.currentCommunityId = currentCommunityId; }
    public RobotStatus getStatus() { return status; }
    public void setStatus(RobotStatus status) { this.status = status; }
    public int getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(int batteryPercent) { this.batteryPercent = Math.max(0, Math.min(100, batteryPercent)); }
    public int getColdBinCapacity() { return coldBinCapacity; }
    public void setColdBinCapacity(int coldBinCapacity) { this.coldBinCapacity = coldBinCapacity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
