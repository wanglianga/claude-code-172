package com.community.delivery.service;

import com.community.delivery.domain.enums.OrderType;
import com.community.delivery.domain.repository.DeliveryOrderRepository;
import com.community.delivery.web.dto.BuildingEfficiency;
import com.community.delivery.web.dto.CommunityEfficiency;
import com.community.delivery.web.dto.OrderTypeEfficiency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 运营端服务：按小区、楼栋、订单类型评估机器人投放效率。
 * 指标：单量、签收量（履约率）、退回量、异常率、冷藏超时、准时率。
 */
@Service
public class AnalyticsService {

    private final DeliveryOrderRepository orderRepository;
    private final Clock clock;

    public AnalyticsService(DeliveryOrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CommunityEfficiency> byCommunity(Long communityId, Instant since) {
        Instant from = since != null ? since : Instant.now(clock).minusSeconds(30L * 24 * 3600);
        return orderRepository.aggregateByCommunity(communityId, from).stream()
                .map(row -> {
                    long total = ((Number) row[1]).longValue();
                    long pickedUp = ((Number) row[2]).longValue();
                    long returned = ((Number) row[3]).longValue();
                    long incidents = ((Number) row[4]).longValue();
                    long coldTimeouts = ((Number) row[5]).longValue();
                    long onTime = ((Number) row[6]).longValue();
                    return new CommunityEfficiency(
                            ((Number) row[0]).longValue(), total, pickedUp, returned, incidents,
                            coldTimeouts, onTime,
                            rate(pickedUp, total), rate(onTime, pickedUp), rate(incidents, total));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BuildingEfficiency> byBuilding(Long communityId, Instant since) {
        Instant from = since != null ? since : Instant.now(clock).minusSeconds(30L * 24 * 3600);
        return orderRepository.aggregateByBuilding(communityId, from).stream()
                .map(row -> {
                    long total = ((Number) row[2]).longValue();
                    long pickedUp = ((Number) row[3]).longValue();
                    long incidents = ((Number) row[4]).longValue();
                    return new BuildingEfficiency(
                            ((Number) row[0]).longValue(), (String) row[1], total, pickedUp, incidents,
                            rate(pickedUp, total), rate(incidents, total));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderTypeEfficiency> byOrderType(Long communityId, Instant since) {
        Instant from = since != null ? since : Instant.now(clock).minusSeconds(30L * 24 * 3600);
        return orderRepository.aggregateByOrderType(communityId, from).stream()
                .map(row -> {
                    OrderType type = (OrderType) row[0];
                    long total = ((Number) row[1]).longValue();
                    long pickedUp = ((Number) row[2]).longValue();
                    long incidents = ((Number) row[3]).longValue();
                    return new OrderTypeEfficiency(type, type.getLabel(), total, pickedUp, incidents,
                            rate(pickedUp, total), rate(incidents, total));
                })
                .toList();
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round(numerator * 10000.0 / denominator) / 100.0;
    }
}
