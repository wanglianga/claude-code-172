package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地降级发布器：直接走进程内事件总线 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
public class LocalEventPublisher implements EventPublisher {

    private final LocalEventBus localEventBus;

    public LocalEventPublisher(LocalEventBus localEventBus) {
        this.localEventBus = localEventBus;
    }

    @Override
    public boolean publish(DeliveryEventMessage message) {
        localEventBus.dispatch(message);
        return true;
    }
}
