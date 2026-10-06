package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.GateAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GateAuthorizationRepository extends JpaRepository<GateAuthorization, Long> {
    Optional<GateAuthorization> findByRequestNo(String requestNo);
    Optional<GateAuthorization> findTopByOrderNoOrderByIdDesc(String orderNo);
}
