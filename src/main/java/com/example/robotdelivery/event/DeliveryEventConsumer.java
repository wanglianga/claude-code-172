package com.example.robotdelivery.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 配送事件消费者：把事件总线上的统一配送事件落库，形成可查询的事件时间线。
 */
@Slf4j
@Component
public class DeliveryEventConsumer {

    private final DeliveryEventRepository repository;
    private final ObjectMapper objectMapper;

    public DeliveryEventConsumer(DeliveryEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = DeliveryEventPublisher.TOPIC,
            groupId = "${spring.kafka.consumer.group-id:robot-delivery}")
    public void onMessage(String json) throws Exception {
        DeliveryEventMessage message = objectMapper.readValue(json, DeliveryEventMessage.class);
        DeliveryEvent event = new DeliveryEvent();
        event.setId(message.id());
        event.setDeliveryId(message.deliveryId());
        event.setCommunityId(message.communityId());
        event.setEventType(message.eventType());
        event.setActor(message.actor());
        event.setReason(message.reason());
        event.setWaitSeconds(message.waitSeconds());
        event.setPayload(message.payload());
        event.setOccurredAt(LocalDateTime.parse(message.occurredAt()));
        repository.save(event);
        log.info("统一配送事件落库: deliveryId={} type={} actor={}",
                message.deliveryId(), message.eventType(), message.actor());
    }
}
