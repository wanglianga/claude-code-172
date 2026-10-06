package com.community.delivery.service;

import com.community.delivery.domain.entity.DeliveryEvent;
import com.community.delivery.domain.repository.DeliveryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Kafka 发布补偿：周期性重发 published=false 的事件，保证消息最终送达 */
@Component
public class EventPublishRetryJob {

    private static final Logger log = LoggerFactory.getLogger(EventPublishRetryJob.class);

    private final DeliveryEventRepository eventRepository;
    private final EventRecorder eventRecorder;

    public EventPublishRetryJob(DeliveryEventRepository eventRepository, EventRecorder eventRecorder) {
        this.eventRepository = eventRepository;
        this.eventRecorder = eventRecorder;
    }

    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public void retryUnpublished() {
        List<DeliveryEvent> pending = eventRepository.findByPublishedFalseOrderByOccurredAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        int success = 0;
        for (DeliveryEvent event : pending) {
            if (eventRecorder.republish(event)) {
                success++;
            } else {
                break; // 通道仍不可用，等下一轮
            }
        }
        log.info("事件发布补偿完成 {}/{}", success, pending.size());
    }
}
