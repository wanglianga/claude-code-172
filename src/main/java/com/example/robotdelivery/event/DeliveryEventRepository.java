package com.example.robotdelivery.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, String> {

    List<DeliveryEvent> findByDeliveryIdOrderByOccurredAtAsc(Long deliveryId);

    List<DeliveryEvent> findByCommunityIdAndEventType(Long communityId, String eventType);

    long countByCommunityIdAndEventType(Long communityId, String eventType);

    long countByCommunityIdAndEventTypeAndReason(Long communityId, String eventType, String reason);

    List<DeliveryEvent> findByCommunityIdOrderByOccurredAtDesc(Long communityId, Pageable pageable);

    List<DeliveryEvent> findByCommunityIdAndEventTypeOrderByOccurredAtDesc(Long communityId, String eventType, Pageable pageable);

    List<DeliveryEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
