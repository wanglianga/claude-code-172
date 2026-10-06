package com.community.delivery.service;

import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.SupportTicket;
import com.community.delivery.domain.enums.*;
import com.community.delivery.domain.repository.DeliveryOrderRepository;
import com.community.delivery.domain.repository.SupportTicketRepository;
import com.community.delivery.web.dto.ComplaintRequest;
import com.community.delivery.web.dto.ResolveTicketRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 客服端服务：受理投诉（送错楼栋等）、依据统一配送事件流判定异常责任、
 * 记录处理结果；送错楼栋可联动编排服务修正楼栋二次配送。
 */
@Service
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryOrchestrator orchestrator;
    private final EventRecorder eventRecorder;
    private final Clock clock;

    public SupportService(SupportTicketRepository ticketRepository,
                          DeliveryOrderRepository orderRepository,
                          DeliveryOrchestrator orchestrator,
                          EventRecorder eventRecorder,
                          Clock clock) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.orchestrator = orchestrator;
        this.eventRecorder = eventRecorder;
        this.clock = clock;
    }

    @Transactional
    public SupportTicket fileComplaint(ComplaintRequest req) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("CS" + Instant.now(clock).toEpochMilli()
                + String.format("%03d", (int) (Math.random() * 1000)));
        ticket.setOrderNo(req.orderNo());
        ticket.setType(req.type());
        ticket.setDescription(req.description());
        ticket.setComplainantPhone(req.complainantPhone());
        ticket.setBuilding(req.building());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setResponsibility(ResponsibleParty.PENDING);

        if (req.orderNo() != null) {
            DeliveryOrder order = orderRepository.findByOrderNo(req.orderNo()).orElse(null);
            if (order != null) {
                ticket.setCommunityId(order.getCommunityId());
                ticket.setBuilding(order.getTargetBuilding());
                // 送错楼栋初判机器人/调度责任，客服可复核
                ticket.setResponsibility(req.type() == TicketType.WRONG_BUILDING
                        ? ResponsibleParty.ROBOT : order.getResponsibleParty());
                eventRecorder.record(order, EventType.COMPLAINT_FILED, EventActor.USER, null,
                        String.format("用户投诉[%s]：%s", req.type().getLabel(), req.description()),
                        0, false,
                        req.type() == TicketType.WRONG_BUILDING ? ResponsibleParty.ROBOT : ResponsibleParty.PENDING);
            }
        }
        return ticketRepository.save(ticket);
    }

    /** 客服处理工单：判定责任、写处理结论；送错楼栋可修正地址触发二次配送 */
    @Transactional
    public SupportTicket resolve(String ticketNo, ResolveTicketRequest req, String handledBy) {
        SupportTicket ticket = ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> new NotFoundException("工单不存在: " + ticketNo));
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BusinessException("TICKET_CLOSED", "工单已处理完成");
        }
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResponsibility(req.responsibility());
        ticket.setResolution(req.resolution());
        ticket.setHandledBy(handledBy);
        ticket.setResolvedAt(Instant.now(clock));
        ticketRepository.save(ticket);

        if (ticket.getOrderNo() != null) {
            DeliveryOrder order = orderRepository.findByOrderNo(ticket.getOrderNo()).orElse(null);
            if (order != null) {
                order.setResponsibleParty(req.responsibility());
                orderRepository.save(order);

                if (ticket.getType() == TicketType.WRONG_BUILDING && req.correctBuilding() != null) {
                    // 送错楼栋：修正楼栋并安排二次配送（内部写客服介入 + 路线调整事件）
                    orchestrator.correctBuilding(order.getOrderNo(), req.correctBuilding(),
                            req.correctFloor(), "工单" + ticketNo + " 客服处理：" + req.resolution());
                }
            }
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> list(String status, String orderNo) {
        if (orderNo != null) {
            return ticketRepository.findByOrderNo(orderNo);
        }
        if (status != null) {
            return ticketRepository.findByStatus(TicketStatus.valueOf(status));
        }
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SupportTicket get(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> new NotFoundException("工单不存在: " + ticketNo));
    }
}
