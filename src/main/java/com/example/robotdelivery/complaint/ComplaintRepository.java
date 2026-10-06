package com.example.robotdelivery.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByDeliveryId(Long deliveryId);

    long countByDelivery_Community_Id(Long communityId);
}
