package com.community.delivery.web.dto;

import jakarta.validation.constraints.NotNull;

/** 电梯满载上报 */
public record ElevatorFullReport(@NotNull Long waitSeconds) {
}
