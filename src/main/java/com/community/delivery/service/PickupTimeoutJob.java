package com.community.delivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 定时扫描用户超时不取与冷藏订单超时 */
@Component
public class PickupTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(PickupTimeoutJob.class);

    private final DeliveryOrchestrator orchestrator;

    public PickupTimeoutJob(DeliveryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /** 每 60 秒扫描一次等待取件订单 */
    @Scheduled(fixedDelayString = "PT60S")
    public void scan() {
        List<String> handled = orchestrator.scanPickupTimeouts();
        if (!handled.isEmpty()) {
            log.info("超时扫描处置订单: {}", handled);
        }
    }
}
