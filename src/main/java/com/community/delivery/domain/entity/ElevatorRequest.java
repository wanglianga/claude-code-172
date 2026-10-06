package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.ElevatorRequestStatus;
import jakarta.persistence.*;

import java.time.Instant;

/** 电梯协同请求：进入楼栋后与电梯系统联动选层 */
@Entity
@Table(name = "elevator_request")
public class ElevatorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 32)
    private String building;

    private Long robotId;

    /** 乘梯起点楼层（通常 1） */
    @Column(nullable = false)
    private int fromFloor = 1;

    @Column(nullable = false)
    private int targetFloor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ElevatorRequestStatus status = ElevatorRequestStatus.REQUESTED;

    /** 满载等待次数（电梯满载场景会多次重试） */
    @Column(nullable = false)
    private int fullRetries = 0;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    private Instant boardedAt;
    private Instant arrivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }
    public int getFromFloor() { return fromFloor; }
    public void setFromFloor(int fromFloor) { this.fromFloor = fromFloor; }
    public int getTargetFloor() { return targetFloor; }
    public void setTargetFloor(int targetFloor) { this.targetFloor = targetFloor; }
    public ElevatorRequestStatus getStatus() { return status; }
    public void setStatus(ElevatorRequestStatus status) { this.status = status; }
    public int getFullRetries() { return fullRetries; }
    public void setFullRetries(int fullRetries) { this.fullRetries = fullRetries; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getBoardedAt() { return boardedAt; }
    public void setBoardedAt(Instant boardedAt) { this.boardedAt = boardedAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }
}
