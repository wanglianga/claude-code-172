package com.example.robotdelivery.complaint;

import com.example.robotdelivery.auth.CurrentUserHolder;
import com.example.robotdelivery.auth.RequireRole;
import com.example.robotdelivery.auth.Role;
import com.example.robotdelivery.cs.CaseType;
import com.example.robotdelivery.cs.CsService;
import com.example.robotdelivery.cs.Responsibility;
import com.example.robotdelivery.config.ApiException;
import com.example.robotdelivery.event.DeliveryEventService;
import com.example.robotdelivery.event.EventActor;
import com.example.robotdelivery.event.EventType;
import com.example.robotdelivery.order.DeliveryOrder;
import com.example.robotdelivery.order.DeliveryOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户投诉", description = "用户投诉（送错楼栋等），自动触发客服介入")
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryEventService eventService;
    private final CsService csService;

    public ComplaintController(ComplaintRepository complaintRepository,
                               DeliveryOrderRepository orderRepository,
                               DeliveryEventService eventService,
                               CsService csService) {
        this.complaintRepository = complaintRepository;
        this.orderRepository = orderRepository;
        this.eventService = eventService;
        this.csService = csService;
    }

    public record FileComplaintRequest(@NotNull Long deliveryId,
                                       @NotNull ComplaintType complaintType,
                                       String content) {
    }

    @Operation(summary = "用户发起投诉", description = "如送错楼栋（WRONG_BUILDING），自动创建客服工单")
    @PostMapping
    @Transactional
    public Complaint file(@Valid @RequestBody FileComplaintRequest request) {
        DeliveryOrder order = orderRepository.findById(request.deliveryId())
                .orElseThrow(() -> ApiException.notFound("配送订单不存在: " + request.deliveryId()));

        Complaint complaint = new Complaint();
        complaint.setDelivery(order);
        complaint.setComplaintType(request.complaintType());
        complaint.setContent(request.content());
        complaint.setStatus(ComplaintStatus.OPEN);
        complaintRepository.save(complaint);

        Map<String, Object> payload = new HashMap<>();
        payload.put("complaintId", complaint.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("complaintType", request.complaintType().name());
        payload.put("content", request.content());
        payload.put("filedBy", CurrentUserHolder.get() == null ? "user" : CurrentUserHolder.get().username());
        eventService.emit(order.getId(), order.getCommunity().getId(),
                EventType.COMPLAINT_FILED, EventActor.USER, request.complaintType().name(), null, payload);

        csService.openCase(order.getId(), CaseType.COMPLAINT, Responsibility.NONE,
                "用户投诉[" + request.complaintType() + "]: " + request.content(), complaint.getId());
        return complaint;
    }

    @Operation(summary = "投诉列表")
    @RequireRole({Role.CS, Role.OPS, Role.ADMIN})
    @GetMapping
    public List<Complaint> list(@RequestParam(required = false) ComplaintStatus status) {
        return status == null ? complaintRepository.findAll() : complaintRepository.findByStatus(status);
    }
}
