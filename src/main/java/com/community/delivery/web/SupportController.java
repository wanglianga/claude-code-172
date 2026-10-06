package com.community.delivery.web;

import com.community.delivery.domain.entity.SupportTicket;
import com.community.delivery.service.SupportService;
import com.community.delivery.web.dto.ComplaintRequest;
import com.community.delivery.web.dto.ResolveTicketRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 客服端：受理投诉、依据统一事件流追踪并判定异常责任、安排送错楼栋二次配送 */
@RestController
@RequestMapping("/api/support")
@Tag(name = "客服端", description = "投诉受理、异常责任追踪、处理结果登记")
@SecurityRequirement(name = "basicAuth")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/tickets")
    @Operation(summary = "登记投诉（送错楼栋/超时/货品/占道）")
    public ResponseEntity<SupportTicket> file(@Valid @RequestBody ComplaintRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.fileComplaint(req));
    }

    @GetMapping("/tickets")
    @Operation(summary = "工单列表，可按状态或订单号过滤")
    public List<SupportTicket> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String orderNo) {
        return supportService.list(status, orderNo);
    }

    @GetMapping("/tickets/{ticketNo}")
    @Operation(summary = "工单详情")
    public SupportTicket get(@PathVariable String ticketNo) {
        return supportService.get(ticketNo);
    }

    @PostMapping("/tickets/{ticketNo}/resolve")
    @Operation(summary = "处理工单：判定责任方并登记结论；送错楼栋可传 correctBuilding/correctFloor 触发二次配送")
    public SupportTicket resolve(@PathVariable String ticketNo,
                                 @Valid @RequestBody ResolveTicketRequest req,
                                 Authentication authentication) {
        return supportService.resolve(ticketNo, req, authentication.getName());
    }
}
