package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** Kafka 消费者：把 delivery-events 投递给所有下游 Sink */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final List<DeliveryEventSink> sinks;

    public KafkaEventConsumer(List<DeliveryEventSink> sinks) {
        this.sinks = sinks;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(DeliveryEventMessage message) {
        log.debug("消费配送事件 orderNo={} seq={} type={}", message.getOrderNo(), message.getSeq(), message.getEventType());
        for (DeliveryEventSink sink : sinks) {
            try {
                sink.onEvent(message);
            } catch (Exception ex) {
                log.error("事件处理失败 sink={} eventId={}", sink.name(), message.getEventId(), ex);
            }
        }
    }
}
