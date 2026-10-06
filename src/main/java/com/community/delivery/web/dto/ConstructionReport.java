package com.community.delivery.web.dto;

import jakarta.validation.constraints.NotNull;

/** 遇楼道施工上报 */
public record ConstructionReport(
        @NotNull Boolean detourAvailable,
        String note) {
}
