package com.community.delivery.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 物业门禁审批请求 */
public record GateDecisionRequest(
        @NotBlank String decision,
        String reason) {

    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(decision) || "GRANT".equalsIgnoreCase(decision);
    }
}
