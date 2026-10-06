package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.DeliveryEvent;
import com.community.delivery.domain.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {

    List<DeliveryEvent> findByOrderIdOrderBySeqAsc(Long orderId);

    List<DeliveryEvent> findByOrderNoOrderBySeqAsc(String orderNo);

    long countByOrderId(Long orderId);

    long countByEventType(EventType eventType);

    boolean existsByOrderIdAndEventType(Long orderId, EventType eventType);

    /** 未成功发布到 Kafka 的事件，供发布补偿任务重发 */
    List<DeliveryEvent> findByPublishedFalseOrderByOccurredAtAsc();

    /** 物业：某小区内对居民通行造成占用/阻碍的事件（机器人是否影响居民通行） */
    long countByCommunityIdAndBlocksResidentPassageTrue(Long communityId);

    @Query("""
            select e.eventType as eventType, count(e) as cnt, coalesce(sum(e.durationMs),0) as waitMs
            from DeliveryEvent e
            where e.communityId = ?1 and e.occurredAt >= ?2
            group by e.eventType
            order by cnt desc
            """)
    List<Object[]> aggregateEventImpact(Long communityId, Instant since);

    /** 物业通行影响：按楼栋聚合占道事件数与总占用时长 */
    @Query("""
            select e.building as building,
                   count(e) as blockingEvents,
                   coalesce(sum(e.durationMs),0) as blockingMs
            from DeliveryEvent e
            where e.communityId = ?1 and e.blocksResidentPassage = true and e.occurredAt >= ?2
            group by e.building
            order by blockingEvents desc
            """)
    List<Object[]> aggregatePassageImpactByBuilding(Long communityId, Instant since);
}
