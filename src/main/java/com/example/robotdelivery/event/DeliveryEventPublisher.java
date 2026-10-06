package com.example.robotdelivery.event;

import com.example.robotdelivery.config.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 配送事件生产者：所有设备动作与业务结果先写入 Kafka 事件总线，
 * key 为配送单 ID，保证同一配送单的事件有序。
 */
@Slf4j
@Component
public class DeliveryEventPublisher {

    public static final String TOPIC = "delivery-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(DeliveryEventMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String key = message.deliveryId() != null
                    ? message.deliveryId().toString()
                    : "community-" + message.communityId();
            kafkaTemplate.send(TOPIC, key, json).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("配送事件写入事件总线失败: {}", message, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "配送事件写入事件总线失败: " + e.getMessage());
        }
    }
}
