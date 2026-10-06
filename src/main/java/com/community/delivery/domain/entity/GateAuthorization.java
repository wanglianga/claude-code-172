package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.GateAuthStatus;
import jakarta.persistence.*;

import java.time.Instant;

/** 门禁授权：机器人到小区门口时申请，物业批准或拒绝 */
@Entity
@Table(name = "gate_authorization")
public class GateAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 48)
    private String requestNo;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long communityId;

    private Long robotId;

    /** 申请的门禁点，如"南门人行门" */
    @Column(length = 64)
    private String gatePoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateAuthStatus status = GateAuthStatus.PENDING;

    @Column(length = 200)
    private String rejectReason;

    /** 授权有效期截止（一次性通行码） */
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    private Instant decidedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }
    public String getGatePoint() { return gatePoint; }
    public void setGatePoint(String gatePoint) { this.gatePoint = gatePoint; }
    public GateAuthStatus getStatus() { return status; }
    public void setStatus(GateAuthStatus status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
