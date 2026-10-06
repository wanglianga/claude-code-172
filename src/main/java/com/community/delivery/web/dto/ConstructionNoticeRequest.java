package com.community.delivery.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** 物业发布施工通告请求 */
public record ConstructionNoticeRequest(
        @NotNull Long communityId,
        String building,
        @NotNull String description,
        @NotNull Instant startTime,
        @NotNull Instant endTime) {
}
