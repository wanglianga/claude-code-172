package com.example.robotdelivery.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin.NewTopics deliveryEventsTopic() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(DeliveryEventPublisher.TOPIC).partitions(3).replicas(1).build());
    }
}
