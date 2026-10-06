package com.example.robotdelivery.robot;

import com.example.robotdelivery.auth.RequireRole;
import com.example.robotdelivery.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "机器人", description = "配送机器人与载物仓管理、状态/电量上报")
@RestController
@RequestMapping("/api/robots")
public class RobotController {

    private final RobotService robotService;

    public RobotController(RobotService robotService) {
        this.robotService = robotService;
    }

    public record RegisterRequest(@NotBlank String code, @NotNull Long communityId, Integer battery) {
    }

    @Operation(summary = "注册机器人（自动配备 A 冷藏仓 / B 普通仓）")
    @RequireRole({Role.OPS, Role.ADMIN})
    @PostMapping
    public Robot register(@Valid @RequestBody RegisterRequest request) {
        return robotService.register(request.code(), request.communityId(), request.battery());
    }

    @Operation(summary = "机器人列表")
    @GetMapping
    public List<Robot> list(@RequestParam(required = false) Long communityId) {
        return robotService.list(communityId);
    }

    @Operation(summary = "机器人载物仓列表")
    @GetMapping("/{id}/compartments")
    public List<CargoCompartment> compartments(@PathVariable Long id) {
        return robotService.compartments(id);
    }

    public record StatusRequest(RobotStatus status, Integer battery) {
    }

    @Operation(summary = "机器人状态/电量上报", description = "配送途中电量低于阈值会触发 LOW_BATTERY 事件与客服介入")
    @RequireRole({Role.OPS, Role.ADMIN})
    @PostMapping("/{id}/status")
    public Robot updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return robotService.updateStatus(id, request.status(), request.battery());
    }
}
