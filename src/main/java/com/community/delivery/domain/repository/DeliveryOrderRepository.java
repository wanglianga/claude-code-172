package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.enums.OrderStatus;
import com.community.delivery.domain.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    Optional<DeliveryOrder> findByOrderNo(String orderNo);

    List<DeliveryOrder> findByCommunityId(Long communityId);

    List<DeliveryOrder> findByCommunityIdAndTargetBuilding(Long communityId, String building);

    List<DeliveryOrder> findByStatus(OrderStatus status);

    /** 已到达等待取件且到达时间早于 cutoff 的订单（超时不取扫描） */
    List<DeliveryOrder> findByStatusAndArrivedAtBefore(OrderStatus status, Instant cutoff);

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndOrderType(Long communityId, OrderType orderType);

    /** 运营：按小区汇总单量、签收、退回、异常、冷藏超时、准时率（H2/PostgreSQL 均兼容的写法） */
    @Query("""
            select o.communityId as communityId,
                   count(o) as total,
                   sum(case when o.status = com.community.delivery.domain.enums.OrderStatus.PICKED_UP then 1 else 0 end) as pickedUp,
                   sum(case when o.status = com.community.delivery.domain.enums.OrderStatus.RETURNED then 1 else 0 end) as returned,
                   sum(case when o.incidentOccurred = true then 1 else 0 end) as incidents,
                   sum(case when o.coldTimeoutOccurred = true then 1 else 0 end) as coldTimeouts,
                   sum(case when o.status = com.community.delivery.domain.enums.OrderStatus.PICKED_UP
                             and o.signedAt <= o.estimatedArrivalAt then 1 else 0 end) as onTime
            from DeliveryOrder o
            where (?1 is null or o.communityId = ?1)
              and o.createdAt >= ?2
            group by o.communityId
            """)
    List<Object[]> aggregateByCommunity(Long communityId, Instant since);

    /** 运营：按小区+楼栋汇总 */
    @Query("""
            select o.communityId as communityId, o.targetBuilding as building,
                   count(o) as total,
                   sum(case when o.status = com.community.delivery.domain.enums.OrderStatus.PICKED_UP then 1 else 0 end) as pickedUp,
                   sum(case when o.incidentOccurred = true then 1 else 0 end) as incidents
            from DeliveryOrder o
            where (?1 is null or o.communityId = ?1)
              and o.createdAt >= ?2
            group by o.communityId, o.targetBuilding
            order by o.communityId, o.targetBuilding
            """)
    List<Object[]> aggregateByBuilding(Long communityId, Instant since);

    /** 运营：按订单类型汇总 */
    @Query("""
            select o.orderType as orderType,
                   count(o) as total,
                   sum(case when o.status = com.community.delivery.domain.enums.OrderStatus.PICKED_UP then 1 else 0 end) as pickedUp,
                   sum(case when o.incidentOccurred = true then 1 else 0 end) as incidents
            from DeliveryOrder o
            where (?1 is null or o.communityId = ?1)
              and o.createdAt >= ?2
            group by o.orderType
            """)
    List<Object[]> aggregateByOrderType(Long communityId, Instant since);
}
