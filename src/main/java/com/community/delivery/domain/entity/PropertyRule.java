package com.community.delivery.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;

/**
 * 小区物业规则：门禁是否允许机器人、夜间运行限制等。
 * 物业要求限制夜间运行即更新本规则。
 */
@Entity
@Table(name = "property_rule")
public class PropertyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long communityId;

    /** 机器人是否可进入该小区 */
    @Column(nullable = false)
    private boolean robotAllowed = true;

    /** 是否限制夜间运行 */
    @Column(nullable = false)
    private boolean nightRestricted = false;

    /** 夜间禁行开始时间（含），如 22:00 */
    @Column
    private LocalTime nightStart;

    /** 夜间禁行结束时间（不含），如 06:00 */
    @Column
    private LocalTime nightEnd;

    /** 门禁审批是否需要物业人工确认（true: 每次申请都要物业批准；false: 自动授权） */
    @Column(nullable = false)
    private boolean gateManualApproval = true;

    @Column(length = 200)
    private String remark;

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
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public boolean isRobotAllowed() { return robotAllowed; }
    public void setRobotAllowed(boolean robotAllowed) { this.robotAllowed = robotAllowed; }
    public boolean isNightRestricted() { return nightRestricted; }
    public void setNightRestricted(boolean nightRestricted) { this.nightRestricted = nightRestricted; }
    public LocalTime getNightStart() { return nightStart; }
    public void setNightStart(LocalTime nightStart) { this.nightStart = nightStart; }
    public LocalTime getNightEnd() { return nightEnd; }
    public void setNightEnd(LocalTime nightEnd) { this.nightEnd = nightEnd; }
    public boolean isGateManualApproval() { return gateManualApproval; }
    public void setGateManualApproval(boolean gateManualApproval) { this.gateManualApproval = gateManualApproval; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
