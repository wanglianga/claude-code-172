package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;

/**
 * 统一事件发布抽象：
 * - Kafka 开启时发布到 {@code delivery-events} topic；
 * - 本地（无 Kafka）时降级为进程内直接投递，保证分析/通知链路行为一致。
 */
public interface EventPublisher {

    /**
     * 发布一条配送事件。
     * @return true 表示已成功进入消息通道（Kafka 已发送或本地已投递）；false 表示发送失败待补偿
     */
    boolean publish(DeliveryEventMessage message);
}
