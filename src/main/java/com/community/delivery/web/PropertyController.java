package com.community.delivery.web;

import com.community.delivery.domain.entity.ConstructionNotice;
import com.community.delivery.domain.entity.PropertyRule;
import com.community.delivery.service.PropertyService;
import com.community.delivery.web.dto.ConstructionNoticeRequest;
import com.community.delivery.web.dto.GateDecisionRequest;
import com.community.delivery.web.dto.PassageImpact;
import com.community.delivery.web.dto.PropertyRuleRequest;
import com.community.delivery.domain.entity.GateAuthorization;
import com.community.delivery.service.DeliveryOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** 物业端：门禁审批、物业规则/夜间限制、施工通告、居民通行影响看板 */
@RestController
@RequestMapping("/api/property")
@Tag(name = "物业端", description = "门禁审批、夜间限制、施工通告、机器人对居民通行的影响")
@SecurityRequirement(name = "basicAuth")
public class PropertyController {

    private final PropertyService propertyService;
    private final DeliveryOrchestrator orchestrator;

    public PropertyController(PropertyService propertyService, DeliveryOrchestrator orchestrator) {
        this.propertyService = propertyService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/communities/{communityId}/rule")
    @Operation(summary = "查看小区物业规则")
    public PropertyRule getRule(@PathVariable Long communityId) {
        return propertyService.getRule(communityId);
    }

    @PutMapping("/communities/{communityId}/rule")
    @Operation(summary = "更新物业规则（含限制夜间运行、是否需要人工审批门禁）")
    public PropertyRule updateRule(@PathVariable Long communityId,
                                   @Valid @RequestBody PropertyRuleRequest req) {
        return propertyService.updateRule(communityId, req.robotAllowed(), req.nightRestricted(),
                req.nightStart(), req.nightEnd(), req.gateManualApproval(), req.remark());
    }

    @PostMapping("/gate-requests/{requestNo}/decision")
    @Operation(summary = "审批门禁申请：decision=APPROVED 开通，REJECTED 拒绝（机器人随后退回）")
    public GateAuthorization decide(@PathVariable String requestNo,
                                    @Valid @RequestBody GateDecisionRequest req) {
        return orchestrator.decideGate(requestNo, req.approved(), req.reason());
    }

    @PostMapping("/constructions")
    @Operation(summary = "发布楼道施工通告")
    public ConstructionNotice publishConstruction(@Valid @RequestBody ConstructionNoticeRequest req) {
        return propertyService.publishConstruction(req.communityId(), req.building(),
                req.description(), req.startTime(), req.endTime());
    }

    @PostMapping("/constructions/{noticeId}/revoke")
    @Operation(summary = "撤销施工通告")
    public ConstructionNotice revoke(@PathVariable Long noticeId) {
        return propertyService.revokeConstruction(noticeId);
    }

    @GetMapping("/constructions")
    @Operation(summary = "查看生效中的施工通告")
    public List<ConstructionNotice> constructions(@RequestParam(required = false) Long communityId) {
        return propertyService.listConstructions(communityId);
    }

    @GetMapping("/communities/{communityId}/passage-impact")
    @Operation(summary = "通行影响看板：占道事件数/占用时长/按楼栋分布（机器人是否影响居民通行）")
    public PassageImpact passageImpact(@PathVariable Long communityId,
                                       @RequestParam(required = false) Instant since) {
        return propertyService.passageImpact(communityId, since);
    }
}
