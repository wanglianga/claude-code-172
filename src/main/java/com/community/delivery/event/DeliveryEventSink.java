package com.community.delivery.event;

import com.community.delivery.event.dto.DeliveryEventMessage;

/** 下游事件处理 Sink（分析投影、通知投影等实现此接口） */
public interface DeliveryEventSink {

    String name();

    void onEvent(DeliveryEventMessage message);
}
