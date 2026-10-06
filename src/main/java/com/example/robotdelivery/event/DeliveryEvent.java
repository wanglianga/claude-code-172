package com.example.robotdelivery.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 统一配送事件：设备动作与业务结果放在同一事件里。
 */
@Getter
@Setter
@Entity
@Table(name = "delivery_event")
public class DeliveryEvent {

    @Id
    @Column(length = 64)
    private String id;

    /** 关联配送单；社区级事件（如物业规则变更）可为空 */
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String actor;

    /** 结构化原因：CONSTRUCTION / ELEVATOR_FULL / WRONG_BUILDING / NIGHT_LIMIT ... */
    @Column(length = 64)
    private String reason;

    /** 避障停留/绕行等耗时秒数，便于物业评估通行影响 */
    @Column(name = "wait_seconds")
    private Long waitSeconds;

    /** 业务上下文 JSON（设备动作 + 业务结果） */
    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
