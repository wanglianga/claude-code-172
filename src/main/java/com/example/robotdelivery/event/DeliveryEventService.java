package com.example.robotdelivery.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 业务侧统一入口：每一次门禁开通、路线调整、电梯请求、避障停留、签收、退回、
 * 客服介入都通过 emit() 写入同一配送事件流。
 */
@Service
public class DeliveryEventService {

    private final DeliveryEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public DeliveryEventService(DeliveryEventPublisher publisher, ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    public DeliveryEventMessage emit(Long deliveryId, Long communityId, EventType type,
                                     EventActor actor, Map<String, Object> payload) {
        return emit(deliveryId, communityId, type, actor, null, null, payload);
    }

    public DeliveryEventMessage emit(Long deliveryId, Long communityId, EventType type, EventActor actor,
                                     String reason, Long waitSeconds, Map<String, Object> payload) {
        String payloadJson;
        try {
            payloadJson = payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = String.valueOf(payload);
        }
        DeliveryEventMessage message = new DeliveryEventMessage(
                UUID.randomUUID().toString(),
                deliveryId,
                communityId,
                type.name(),
                actor.name(),
                reason,
                waitSeconds,
                payloadJson,
                LocalDateTime.now().toString());
        publisher.send(message);
        return message;
    }
}
