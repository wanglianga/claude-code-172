package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地事件总线：无 Kafka 时（local profile）把事件直接投递给所有下游 Sink，
 * 使"落库 -> 发布 -> 分析/通知消费"的链路在本地与生产环境行为一致。
 */
@Component
public class LocalEventBus {

    private static final Logger log = LoggerFactory.getLogger(LocalEventBus.class);

    private final List<DeliveryEventSink> sinks;

    public LocalEventBus(List<DeliveryEventSink> sinks) {
        this.sinks = sinks;
    }

    public void dispatch(DeliveryEventMessage message) {
        for (DeliveryEventSink sink : sinks) {
            try {
                sink.onEvent(message);
            } catch (Exception ex) {
                log.error("本地事件投递失败 sink={} eventId={}", sink.name(), message.getEventId(), ex);
            }
        }
    }
}
