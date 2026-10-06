package com.example.robotdelivery.elevator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ElevatorRequestRepository extends JpaRepository<ElevatorRequest, Long> {

    List<ElevatorRequest> findByStatus(ElevatorStatus status);

    List<ElevatorRequest> findByDeliveryIdOrderByIdDesc(Long deliveryId);

    List<ElevatorRequest> findByStatusAndRequestedAtBefore(ElevatorStatus status, LocalDateTime requestedBefore);
}
