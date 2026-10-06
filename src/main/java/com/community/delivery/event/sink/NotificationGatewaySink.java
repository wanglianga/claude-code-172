package com.community.delivery.event.sink;

import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.enums.NotificationChannel;
import com.community.delivery.domain.repository.DeliveryOrderRepository;
import com.community.delivery.event.DeliveryEventSink;
import com.community.delivery.event.dto.DeliveryEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 通知网关 Sink：消费事件流中的通知类事件，模拟向短信/小程序网关下发。
 * 通知业务记录（NotificationRecord）由编排链路在业务事务内持久化，
 * 此 Sink 只代表解耦的外部网关投递动作（真实环境在此调用短信/小程序 API 并可回执更新状态）。
 */
@Component
public class NotificationGatewaySink implements DeliveryEventSink {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewaySink.class);

    private final DeliveryOrderRepository orderRepository;

    public NotificationGatewaySink(DeliveryOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String name() {
        return "notification-gateway";
    }

    @Override
    public void onEvent(DeliveryEventMessage message) {
        switch (message.getEventType()) {
            case "ARRIVED_NOTIFIED", "PICKUP_REMINDER", "COLD_CHAIN_TIMEOUT" -> {
                Optional<DeliveryOrder> order = orderRepository.findByOrderNo(message.getOrderNo());
                NotificationChannel channel = order
                        .map(o -> o.getMiniAppUserId() != null ? NotificationChannel.MINI_APP : NotificationChannel.SMS)
                        .orElse(NotificationChannel.SMS);
                // 模拟网关调用
                log.info("[通知网关] {} -> {} orderNo={} seq={} detail={}",
                        channel.getLabel(),
                        order.map(DeliveryOrder::getUserPhone).orElse("-"),
                        message.getOrderNo(), message.getSeq(), message.getDetail());
            }
            default -> {
                // 非通知类事件，通知网关不处理
            }
        }
    }
}
