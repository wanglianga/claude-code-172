package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.ElevatorRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElevatorRequestRepository extends JpaRepository<ElevatorRequest, Long> {
    Optional<ElevatorRequest> findByOrderNo(String orderNo);
    Optional<ElevatorRequest> findTopByOrderNoOrderByIdDesc(String orderNo);
}
