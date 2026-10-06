package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.OrderStatus;
import com.community.delivery.domain.enums.OrderType;
import com.community.delivery.domain.enums.ResponsibleParty;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 配送订单聚合根。记录题目要求的全部订单要素：
 * 订单类型、收货楼栋、门禁授权（关联 GateAuthorization）、是否冷藏、预计送达、
 * 机器人电量（接单时快照）、载物仓编号、用户联系方式。
 */
@Entity
@Table(name = "delivery_order", indexes = {
        @Index(name = "idx_order_community", columnList = "communityId"),
        @Index(name = "idx_order_status", columnList = "status")
})
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外订单号 */
    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderType orderType;

    @Column(nullable = false)
    private Long communityId;

    /** 收货楼栋，如 3 号楼 */
    @Column(nullable = false, length = 32)
    private String targetBuilding;

    /** 单元 */
    @Column(length = 16)
    private String unit;

    /** 目标楼层（电梯协同用） */
    @Column(nullable = false)
    private int targetFloor;

    /** 房号 */
    @Column(length = 16)
    private String room;

    /** 是否需要冷藏 */
    @Column(nullable = false)
    private boolean coldChain = false;

    /** 载物仓编号（机器人分配时写入，如 R01-BIN-C1） */
    @Column(length = 32)
    private String binCode;

    @Column(length = 64)
    private String userName;

    /** 用户联系方式（手机号，短信/小程序通知用） */
    @Column(nullable = false, length = 20)
    private String userPhone;

    /** 小程序用户标识（可选） */
    @Column(length = 64)
    private String miniAppUserId;

    private Long robotId;

    /** 接单时机器人电量快照（%） */
    private Integer assignedBatteryPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.CREATED;

    /** 预计送达时间 */
    @Column(nullable = false)
    private Instant estimatedArrivalAt;

    /** 到达楼层/门口、开始等待取件的时间（超时计算起点） */
    private Instant arrivedAt;

    /** 实际签收时间 */
    private Instant signedAt;

    /** 退回完成时间 */
    private Instant returnedAt;

    /** 是否发生过影响通行/时效的异常 */
    @Column(nullable = false)
    private boolean incidentOccurred = false;

    /** 是否发生冷藏超时 */
    @Column(nullable = false)
    private boolean coldTimeoutOccurred = false;

    /** 异常主要责任方（客服可复核修正） */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ResponsibleParty responsibleParty = ResponsibleParty.PENDING;

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
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getTargetBuilding() { return targetBuilding; }
    public void setTargetBuilding(String targetBuilding) { this.targetBuilding = targetBuilding; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getTargetFloor() { return targetFloor; }
    public void setTargetFloor(int targetFloor) { this.targetFloor = targetFloor; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public boolean isColdChain() { return coldChain; }
    public void setColdChain(boolean coldChain) { this.coldChain = coldChain; }
    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getMiniAppUserId() { return miniAppUserId; }
    public void setMiniAppUserId(String miniAppUserId) { this.miniAppUserId = miniAppUserId; }
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }
    public Integer getAssignedBatteryPercent() { return assignedBatteryPercent; }
    public void setAssignedBatteryPercent(Integer assignedBatteryPercent) { this.assignedBatteryPercent = assignedBatteryPercent; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Instant getEstimatedArrivalAt() { return estimatedArrivalAt; }
    public void setEstimatedArrivalAt(Instant estimatedArrivalAt) { this.estimatedArrivalAt = estimatedArrivalAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public boolean isIncidentOccurred() { return incidentOccurred; }
    public void setIncidentOccurred(boolean incidentOccurred) { this.incidentOccurred = incidentOccurred; }
    public boolean isColdTimeoutOccurred() { return coldTimeoutOccurred; }
    public void setColdTimeoutOccurred(boolean coldTimeoutOccurred) { this.coldTimeoutOccurred = coldTimeoutOccurred; }
    public ResponsibleParty getResponsibleParty() { return responsibleParty; }
    public void setResponsibleParty(ResponsibleParty responsibleParty) { this.responsibleParty = responsibleParty; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
