package com.example.robotdelivery.analytics;

import com.example.robotdelivery.auth.RequireRole;
import com.example.robotdelivery.auth.Role;
import com.example.robotdelivery.order.OrderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "统计分析", description = "物业端通行影响 / 运营端投放效率")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "物业端：机器人是否影响居民通行",
            description = "避障停留次数与时长、施工绕行、电梯满载、夜间限制、投诉等指标与影响等级")
    @RequireRole({Role.PROPERTY, Role.OPS, Role.ADMIN})
    @GetMapping("/property/passage-impact")
    public Map<String, Object> passageImpact(@RequestParam Long communityId) {
        return analyticsService.passageImpact(communityId);
    }

    @Operation(summary = "运营端：按小区/楼栋/订单类型评估机器人投放效率")
    @RequireRole({Role.OPS, Role.ADMIN})
    @GetMapping("/ops/efficiency")
    public List<Map<String, Object>> opsEfficiency(@RequestParam(required = false) Long communityId,
                                                   @RequestParam(required = false) Long buildingId,
                                                   @RequestParam(required = false) OrderType orderType) {
        return analyticsService.opsEfficiency(communityId, buildingId, orderType);
    }
}
