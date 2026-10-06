package com.community.delivery.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 楼道施工通告：施工期间机器人必须绕行或转人工接力。
 * 由物业端发布，机器人行进到相应楼栋时触发 ROUTE_ADJUSTED / CONSTRUCTION_ENCOUNTERED 事件。
 */
@Entity
@Table(name = "construction_notice")
public class ConstructionNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    /** 受影响楼栋，null 表示小区范围内 */
    @Column
    private String building;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    /** 是否仍生效（过期后自动视为无效，也可由物业提前撤销） */
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
