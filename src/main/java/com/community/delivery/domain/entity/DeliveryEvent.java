package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.EventActor;
import com.community.delivery.domain.enums.EventType;
import com.community.delivery.domain.enums.ResponsibleParty;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 统一配送事件（append-only）。
 * 每一次门禁开通、路线调整、电梯请求、避障停留、签收、退回和客服介入
 * 都是同一 {@code orderId} 事件流上的一条记录，设备动作与业务结果同表：
 * {@code eventType} 表示动作，{@code result/detail/durationMs/responsibleParty} 表示业务结果。
 * 该表既是审计时间线、客服责任追踪来源，也是运营/物业统计的事实表，并同步到 Kafka。
 */
@Entity
@Table(name = "delivery_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_order_seq", columnNames = {"orderId", "seq"}),
        indexes = {
        @Index(name = "idx_event_order", columnList = "orderId,seq"),
        @Index(name = "idx_event_community", columnList = "communityId"),
        @Index(name = "idx_event_type", columnList = "eventType"),
        @Index(name = "idx_event_time", columnList = "occurredAt")
})
public class DeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 同一配送订单内单调递增的序号，保证事件流顺序 */
    @Column(nullable = false)
    private int seq;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 32)
    private String building;

    private Long robotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventActor actor;

    /** 事件发生位置：GATE 小区门口 / COMMUNITY 园区 / BUILDING 楼栋 / ELEVATOR 电梯 / DOOR 户门口 */
    @Column(length = 16)
    private String location;

    /** 业务结果/说明，如 "物业拒绝：夜间禁止入内" */
    @Column(length = 500)
    private String detail;

    /** 该动作造成的等待/绕行耗时（毫秒），0 表示无等待 */
    @Column(nullable = false)
    private long durationMs = 0;

    /** 该事件是否对居民通行造成占用/阻碍 */
    @Column(nullable = false)
    private boolean blocksResidentPassage = false;

    /** 事件时刻机器人电量 */
    private Integer batteryPercent;

    /** 事件时刻订单状态快照 */
    @Column(length = 32)
    private String orderStatusSnapshot;

    /** 异常事件的责任方（非异常为 NONE/PENDING） */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ResponsibleParty responsibleParty = ResponsibleParty.NONE;

    /** 是否已同步到 Kafka（本地总线模式恒为 true） */
    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public EventActor getActor() { return actor; }
    public void setActor(EventActor actor) { this.actor = actor; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public boolean isBlocksResidentPassage() { return blocksResidentPassage; }
    public void setBlocksResidentPassage(boolean blocksResidentPassage) { this.blocksResidentPassage = blocksResidentPassage; }
    public Integer getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }
    public String getOrderStatusSnapshot() { return orderStatusSnapshot; }
    public void setOrderStatusSnapshot(String orderStatusSnapshot) { this.orderStatusSnapshot = orderStatusSnapshot; }
    public ResponsibleParty getResponsibleParty() { return responsibleParty; }
    public void setResponsibleParty(ResponsibleParty responsibleParty) { this.responsibleParty = responsibleParty; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
