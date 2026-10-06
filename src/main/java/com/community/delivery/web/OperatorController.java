package com.community.delivery.web;

import com.community.delivery.service.AnalyticsService;
import com.community.delivery.web.dto.BuildingEfficiency;
import com.community.delivery.web.dto.CommunityEfficiency;
import com.community.delivery.web.dto.OrderTypeEfficiency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** 运营端：按小区/楼栋/订单类型评估机器人投放效率 */
@RestController
@RequestMapping("/api/operator")
@Tag(name = "运营端", description = "按小区、楼栋、订单类型评估投放效率")
@SecurityRequirement(name = "basicAuth")
public class OperatorController {

    private final AnalyticsService analyticsService;

    public OperatorController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/efficiency/by-community")
    @Operation(summary = "按小区汇总单量/签收率/退回/异常率/冷藏超时/准时率")
    public List<CommunityEfficiency> byCommunity(@RequestParam(required = false) Long communityId,
                                                 @RequestParam(required = false) Instant since) {
        return analyticsService.byCommunity(communityId, since);
    }

    @GetMapping("/efficiency/by-building")
    @Operation(summary = "按小区+楼栋汇总履约率/异常率")
    public List<BuildingEfficiency> byBuilding(@RequestParam(required = false) Long communityId,
                                               @RequestParam(required = false) Instant since) {
        return analyticsService.byBuilding(communityId, since);
    }

    @GetMapping("/efficiency/by-order-type")
    @Operation(summary = "按订单类型（外卖/药品/商超）汇总履约率/异常率")
    public List<OrderTypeEfficiency> byOrderType(@RequestParam(required = false) Long communityId,
                                                 @RequestParam(required = false) Instant since) {
        return analyticsService.byOrderType(communityId, since);
    }
}
