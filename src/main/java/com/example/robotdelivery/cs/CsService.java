package com.example.robotdelivery.cs;

import com.example.robotdelivery.complaint.Complaint;
import com.example.robotdelivery.complaint.ComplaintRepository;
import com.example.robotdelivery.complaint.ComplaintStatus;
import com.example.robotdelivery.config.ApiException;
import com.example.robotdelivery.event.DeliveryEventService;
import com.example.robotdelivery.event.EventActor;
import com.example.robotdelivery.event.EventType;
import com.example.robotdelivery.order.DeliveryOrder;
import com.example.robotdelivery.order.DeliveryOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CsService {

    private final CsCaseRepository csCaseRepository;
    private final ComplaintRepository complaintRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryEventService eventService;

    public CsService(CsCaseRepository csCaseRepository,
                     ComplaintRepository complaintRepository,
                     DeliveryOrderRepository orderRepository,
                     DeliveryEventService eventService) {
        this.csCaseRepository = csCaseRepository;
        this.complaintRepository = complaintRepository;
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }

    /**
     * 客服介入：异常发生时自动（或人工）开立工单，并写入统一配送事件。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public CsCase openCase(Long deliveryId, CaseType caseType, Responsibility responsibility,
                           String note, Long complaintId) {
        CsCase csCase = new CsCase();
        csCase.setCaseNo("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        csCase.setDeliveryId(deliveryId);
        csCase.setComplaintId(complaintId);
        csCase.setCaseType(caseType);
        csCase.setSource("AUTO");
        csCase.setStatus(CaseStatus.OPEN);
        csCase.setResponsibility(responsibility);
        csCase.setNote(note);
        csCaseRepository.save(csCase);

        Long communityId = orderRepository.findById(deliveryId)
                .map(o -> o.getCommunity().getId())
                .orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("caseNo", csCase.getCaseNo());
        payload.put("caseType", caseType.name());
        payload.put("responsibility", responsibility.name());
        payload.put("note", note);
        eventService.emit(deliveryId, communityId, EventType.CS_INTERVENTION, EventActor.CS, payload);
        return csCase;
    }

    /**
     * 客服结案：认定责任并写入统一配送事件。
     */
    @Transactional
    public CsCase resolve(Long caseId, Responsibility responsibility, String note, String handler) {
        CsCase csCase = csCaseRepository.findById(caseId)
                .orElseThrow(() -> ApiException.notFound("客服工单不存在: " + caseId));
        if (csCase.getStatus() == CaseStatus.RESOLVED) {
            throw ApiException.conflict("工单已结案: " + csCase.getCaseNo());
        }
        csCase.setStatus(CaseStatus.RESOLVED);
        csCase.setResponsibility(responsibility);
        csCase.setHandler(handler);
        csCase.setResolvedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            csCase.setNote((csCase.getNote() == null ? "" : csCase.getNote() + " | ") + note);
        }
        csCaseRepository.save(csCase);

        if (csCase.getComplaintId() != null) {
            complaintRepository.findById(csCase.getComplaintId()).ifPresent(c -> {
                c.setStatus(ComplaintStatus.RESOLVED);
                c.setResolvedAt(LocalDateTime.now());
                complaintRepository.save(c);
            });
        }

        Long communityId = csCase.getDeliveryId() == null ? null
                : orderRepository.findById(csCase.getDeliveryId())
                .map(DeliveryOrder::getCommunity).map(com.example.robotdelivery.community.Community::getId)
                .orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("caseNo", csCase.getCaseNo());
        payload.put("caseType", csCase.getCaseType().name());
        payload.put("responsibility", responsibility.name());
        payload.put("handler", handler);
        payload.put("note", note);
        eventService.emit(csCase.getDeliveryId(), communityId, EventType.CS_CASE_CLOSED, EventActor.CS, payload);
        return csCase;
    }
}
