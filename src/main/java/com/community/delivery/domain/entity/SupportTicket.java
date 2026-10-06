package com.community.delivery.domain.entity;

import com.community.delivery.domain.enums.ResponsibleParty;
import com.community.delivery.domain.enums.TicketStatus;
import com.community.delivery.domain.enums.TicketType;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 客服工单：用户投诉（如送错楼栋）与异常介入。
 * 客服端据此追踪异常责任，责任可由客服依据统一事件流复核。
 */
@Entity
@Table(name = "support_ticket", indexes = {
        @Index(name = "idx_ticket_order", columnList = "orderNo"),
        @Index(name = "idx_ticket_status", columnList = "status")
})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String ticketNo;

    /** 关联订单；无订单的投诉（如占道）可空 */
    @Column(length = 32)
    private String orderNo;

    @Column
    private Long communityId;

    @Column
    private String building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketType type;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 20)
    private String complainantPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResponsibleParty responsibility = ResponsibleParty.PENDING;

    @Column(length = 500)
    private String resolution;

    @Column(length = 32)
    private String handledBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant resolvedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getComplainantPhone() { return complainantPhone; }
    public void setComplainantPhone(String complainantPhone) { this.complainantPhone = complainantPhone; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public ResponsibleParty getResponsibility() { return responsibility; }
    public void setResponsibility(ResponsibleParty responsibility) { this.responsibility = responsibility; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
