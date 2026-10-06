package com.community.delivery.service;

import com.community.delivery.domain.entity.DeliveryEvent;
import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.NotificationRecord;
import com.community.delivery.domain.repository.DeliveryEventRepository;
import com.community.delivery.domain.repository.DeliveryOrderRepository;
import com.community.delivery.domain.repository.NotificationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 订单与统一配送事件时间线查询（客服追踪、运营核查共用） */
@Service
public class OrderQueryService {

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryEventRepository eventRepository;
    private final NotificationRecordRepository notificationRepository;

    public OrderQueryService(DeliveryOrderRepository orderRepository,
                             DeliveryEventRepository eventRepository,
                             NotificationRecordRepository notificationRepository) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public DeliveryOrder getOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("订单不存在: " + orderNo));
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> list(Long communityId, String building, String status) {
        if (communityId != null && building != null) {
            return orderRepository.findByCommunityIdAndTargetBuilding(communityId, building);
        }
        if (communityId != null) {
            return orderRepository.findByCommunityId(communityId);
        }
        if (status != null) {
            return orderRepository.findByStatus(
                    com.community.delivery.domain.enums.OrderStatus.valueOf(status));
        }
        return orderRepository.findAll();
    }

    /** 同一配送订单的完整事件流（设备动作 + 业务结果） */
    @Transactional(readOnly = true)
    public List<DeliveryEvent> timeline(String orderNo) {
        getOrder(orderNo); // 确保订单存在
        return eventRepository.findByOrderNoOrderBySeqAsc(orderNo);
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> notifications(String orderNo) {
        return notificationRepository.findByOrderNoOrderBySentAtAsc(orderNo);
    }
}
