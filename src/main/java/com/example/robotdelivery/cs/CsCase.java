package com.example.robotdelivery.cs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客服工单：异常进入客服介入流程，追踪责任归属。
 */
@Getter
@Setter
@Entity
@Table(name = "cs_case")
public class CsCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no", nullable = false, unique = true, length = 40)
    private String caseNo;

    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "complaint_id")
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;

    /** AUTO=系统自动创建，MANUAL=人工创建 */
    @Column(nullable = false, length = 10)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Responsibility responsibility;

    @Column(columnDefinition = "text")
    private String note;

    @Column(length = 64)
    private String handler;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
