package com.example.robotdelivery.access;

import com.example.robotdelivery.auth.CurrentUserHolder;
import com.example.robotdelivery.auth.RequireRole;
import com.example.robotdelivery.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "物业门禁", description = "机器人到小区门口申请物业门禁，物业开通/拒绝")
@RestController
@RequestMapping("/api/access-requests")
public class AccessController {

    private final AccessRequestRepository accessRepository;
    private final AccessService accessService;

    public AccessController(AccessRequestRepository accessRepository, AccessService accessService) {
        this.accessRepository = accessRepository;
        this.accessService = accessService;
    }

    @Operation(summary = "门禁申请列表（物业端查看待审批）")
    @RequireRole({Role.PROPERTY, Role.OPS, Role.ADMIN})
    @GetMapping
    public List<AccessRequest> list(@RequestParam(required = false) AccessStatus status,
                                    @RequestParam(required = false) Long communityId) {
        if (communityId != null && status != null) {
            return accessRepository.findByDelivery_Community_IdAndStatus(communityId, status);
        }
        if (status != null) {
            return accessRepository.findByStatus(status);
        }
        return accessRepository.findAll();
    }

    @Operation(summary = "物业开通门禁")
    @RequireRole({Role.PROPERTY, Role.ADMIN})
    @PostMapping("/{id}/approve")
    public AccessRequest approve(@PathVariable Long id) {
        return accessService.approve(id, CurrentUserHolder.get().username());
    }

    public record DenyRequest(@NotBlank String reason) {
    }

    @Operation(summary = "物业拒绝门禁")
    @RequireRole({Role.PROPERTY, Role.ADMIN})
    @PostMapping("/{id}/deny")
    public AccessRequest deny(@PathVariable Long id, @RequestBody DenyRequest request) {
        return accessService.deny(id, request.reason(), CurrentUserHolder.get().username());
    }
}
