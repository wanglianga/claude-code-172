package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/** Kafka 事件发布器：按订单号分区保证同一配送事件流有序 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, DeliveryEventMessage> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, DeliveryEventMessage> kafkaTemplate,
                               @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public boolean publish(DeliveryEventMessage message) {
        try {
            CompletableFuture<SendResult<String, DeliveryEventMessage>> future =
                    kafkaTemplate.send(topic, message.getOrderNo(), message);
            SendResult<String, DeliveryEventMessage> result = future.join();
            log.debug("事件已发送到Kafka topic={} partition={} offset={} eventType={} orderNo={}",
                    topic, result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(), message.getEventType(), message.getOrderNo());
            return true;
        } catch (Exception ex) {
            log.warn("Kafka发送失败，事件将由补偿任务重发 orderNo={} eventType={} err={}",
                    message.getOrderNo(), message.getEventType(), ex.getMessage());
            return false;
        }
    }
}
