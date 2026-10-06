package com.community.delivery.service;

import com.community.delivery.domain.entity.DeliveryEvent;
import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.Robot;
import com.community.delivery.domain.enums.EventActor;
import com.community.delivery.domain.enums.EventType;
import com.community.delivery.domain.enums.ResponsibleParty;
import com.community.delivery.domain.repository.DeliveryEventRepository;
import com.community.delivery.event.EventPublisher;
import com.community.delivery.event.dto.DeliveryEventMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 统一配送事件记录器。任何设备动作/业务结果都必须经此追加到同一订单事件流：
 * - 以 {@code (orderId, seq)} 单调递增保证顺序，数据库唯一约束兜底；
 * - 事件与订单状态更新在同一事务落库（"设备动作和业务结果放在同一事件里"）；
 * - 事务提交后（afterCommit）才投递 Kafka/本地总线，避免消费者读到未提交数据；
 *   发送失败保留 published=false，交由 {@link EventPublishRetryJob} 补偿重发。
 */
@Service
public class EventRecorder {

    private final DeliveryEventRepository eventRepository;
    private final EventPublisher eventPublisher;

    public EventRecorder(DeliveryEventRepository eventRepository, EventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    /** 在当前业务事务内追加一条事件，并在事务提交后发布。 */
    @Transactional(propagation = Propagation.REQUIRED)
    public DeliveryEvent record(DeliveryOrder order, Robot robot, EventType type, EventActor actor,
                                String location, String detail, long durationMs,
                                boolean blocksPassage, ResponsibleParty responsibleParty) {
        long count = eventRepository.countByOrderId(order.getId());
        DeliveryEvent event = new DeliveryEvent();
        event.setSeq((int) count + 1);
        event.setOrderId(order.getId());
        event.setOrderNo(order.getOrderNo());
        event.setCommunityId(order.getCommunityId());
        event.setBuilding(order.getTargetBuilding());
        event.setRobotId(robot != null ? robot.getId() : order.getRobotId());
        event.setEventType(type);
        event.setActor(actor);
        event.setLocation(location);
        event.setDetail(detail);
        event.setDurationMs(Math.max(0, durationMs));
        event.setBlocksResidentPassage(blocksPassage);
        event.setBatteryPercent(robot != null ? Integer.valueOf(robot.getBatteryPercent())
                : order.getAssignedBatteryPercent());
        event.setOrderStatusSnapshot(order.getStatus().name());
        event.setResponsibleParty(responsibleParty != null ? responsibleParty : ResponsibleParty.NONE);
        DeliveryEvent saved = eventRepository.save(event);
        eventRepository.flush();

        if (type.isIncident()) {
            order.setIncidentOccurred(true);
        }
        // 责任首次落定后写回订单（PENDING -> 具体方），客服仍可后续复核修正
        if (responsibleParty != null && responsibleParty != ResponsibleParty.NONE
                && responsibleParty != ResponsibleParty.PENDING
                && order.getResponsibleParty() == ResponsibleParty.PENDING) {
            order.setResponsibleParty(responsibleParty);
        }

        DeliveryEventMessage message = toMessage(saved);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAndMark(saved.getId(), message);
                }
            });
        } else {
            publishAndMark(saved.getId(), message);
        }
        return saved;
    }

    /** 无机器人上下文的便捷重载 */
    @Transactional(propagation = Propagation.REQUIRED)
    public DeliveryEvent record(DeliveryOrder order, EventType type, EventActor actor,
                                String location, String detail, long durationMs,
                                boolean blocksPassage, ResponsibleParty responsibleParty) {
        return record(order, null, type, actor, location, detail, durationMs, blocksPassage, responsibleParty);
    }

    /** 重新发布单条事件（补偿任务用），独立事务更新 published 标记 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean republish(DeliveryEvent event) {
        boolean ok = eventPublisher.publish(toMessage(event));
        if (ok) {
            event.setPublished(true);
            eventRepository.save(event);
        }
        return ok;
    }

    private void publishAndMark(Long eventId, DeliveryEventMessage message) {
        boolean ok;
        try {
            ok = eventPublisher.publish(message);
        } catch (Exception ex) {
            ok = false;
        }
        if (ok) {
            DeliveryEvent event = eventRepository.findById(eventId).orElse(null);
            if (event != null && !event.isPublished()) {
                event.setPublished(true);
                eventRepository.save(event);
            }
        }
    }

    private DeliveryEventMessage toMessage(DeliveryEvent e) {
        DeliveryEventMessage m = new DeliveryEventMessage();
        m.setEventId(e.getId());
        m.setSeq(e.getSeq());
        m.setOrderNo(e.getOrderNo());
        m.setOrderId(e.getOrderId());
        m.setCommunityId(e.getCommunityId());
        m.setBuilding(e.getBuilding());
        m.setRobotId(e.getRobotId());
        m.setEventType(e.getEventType().name());
        m.setActor(e.getActor().name());
        m.setLocation(e.getLocation());
        m.setDetail(e.getDetail());
        m.setDurationMs(e.getDurationMs());
        m.setBlocksResidentPassage(e.isBlocksResidentPassage());
        m.setBatteryPercent(e.getBatteryPercent());
        m.setOrderStatusSnapshot(e.getOrderStatusSnapshot());
        m.setResponsibleParty(e.getResponsibleParty() != null ? e.getResponsibleParty().name() : null);
        m.setIncident(e.getEventType().isIncident());
        m.setOccurredAtEpochMilli(e.getOccurredAt().toEpochMilli());
        return m;
    }
}
