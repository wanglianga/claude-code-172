package com.example.robotdelivery.event;

/**
 * Kafka 上传输的配送事件消息。
 */
public record DeliveryEventMessage(
        String id,
        Long deliveryId,
        Long communityId,
        String eventType,
        String actor,
        String reason,
        Long waitSeconds,
        String payload,
        String occurredAt) {
}
