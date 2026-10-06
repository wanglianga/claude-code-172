package com.community.delivery.web;

import com.community.delivery.domain.entity.DeliveryEvent;
import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.NotificationRecord;
import com.community.delivery.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 订单与统一配送事件时间线查询（物业/运营/客服登录后均可核查） */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单与事件流", description = "订单查询、同一配送事件流时间线、通知记录")
@SecurityRequirement(name = "basicAuth")
public class OrderController {

    private final OrderQueryService queryService;

    public OrderController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "订单列表，可按小区/楼栋/状态过滤")
    public List<DeliveryOrder> list(@RequestParam(required = false) Long communityId,
                                    @RequestParam(required = false) String building,
                                    @RequestParam(required = false) String status) {
        return queryService.list(communityId, building, status);
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "订单详情")
    public DeliveryOrder get(@PathVariable String orderNo) {
        return queryService.getOrder(orderNo);
    }

    @GetMapping("/{orderNo}/events")
    @Operation(summary = "同一订单的完整配送事件流（门禁/路线/电梯/避障/签收/退回/客服介入）")
    public List<DeliveryEvent> events(@PathVariable String orderNo) {
        return queryService.timeline(orderNo);
    }

    @GetMapping("/{orderNo}/notifications")
    @Operation(summary = "该订单的短信/小程序通知记录")
    public List<NotificationRecord> notifications(@PathVariable String orderNo) {
        return queryService.notifications(orderNo);
    }
}
