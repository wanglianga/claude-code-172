package com.community.delivery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Kafka 基础设施配置：自动创建统一配送事件 topic */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic deliveryEventsTopic(@Value("${app.kafka.topic}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }
}
