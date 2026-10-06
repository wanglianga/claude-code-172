package com.example.robotdelivery.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByStatus(AccessStatus status);

    List<AccessRequest> findByDeliveryId(Long deliveryId);

    List<AccessRequest> findByDelivery_Community_IdAndStatus(Long communityId, AccessStatus status);
}
