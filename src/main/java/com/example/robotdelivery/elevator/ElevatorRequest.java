package com.example.robotdelivery.elevator;

import com.example.robotdelivery.order.DeliveryOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 与电梯系统协同的乘梯请求。
 */
@Getter
@Setter
@Entity
@Table(name = "elevator_request")
public class ElevatorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryOrder delivery;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "from_floor", nullable = false)
    private Integer fromFloor;

    @Column(name = "to_floor", nullable = false)
    private Integer toFloor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElevatorStatus status;

    /** 电梯轿厢编号（电梯系统分配） */
    @Column(name = "car_id", length = 32)
    private String carId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
