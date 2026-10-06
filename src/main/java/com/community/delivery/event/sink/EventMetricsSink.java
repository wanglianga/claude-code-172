package com.community.delivery.event.sink;

import com.community.delivery.event.DeliveryEventSink;
import com.community.delivery.event.dto.DeliveryEventMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件指标投影 Sink：消费统一事件流，维护各事件类型的实时计数，
 * 作为运营/物业实时看板的轻量投影（与数据库聚合互补，也用于验证消息链路通畅）。
 */
@Component
public class EventMetricsSink implements DeliveryEventSink {

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final AtomicLong totalConsumed = new AtomicLong();

    @Override
    public String name() {
        return "event-metrics";
    }

    @Override
    public void onEvent(DeliveryEventMessage message) {
        counters.computeIfAbsent(message.getEventType(), k -> new AtomicLong()).incrementAndGet();
        totalConsumed.incrementAndGet();
    }

    public long getTotalConsumed() {
        return totalConsumed.get();
    }

    public Map<String, Long> snapshotByType() {
        ConcurrentHashMap<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
}
