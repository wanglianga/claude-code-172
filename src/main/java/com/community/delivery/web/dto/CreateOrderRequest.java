package com.community.delivery.web.dto;

import com.community.delivery.domain.enums.OrderType;
import jakarta.validation.constraints.*;

import java.time.Instant;

/** 下单请求 */
public record CreateOrderRequest(
        @NotNull OrderType orderType,
        @NotNull Long communityId,
        @NotBlank String targetBuilding,
        String unit,
        @Min(1) int targetFloor,
        String room,
        boolean coldChain,
        @NotBlank String userName,
        @NotBlank @Pattern(regexp = "1[3-9]\\d{9}", message = "手机号格式不正确") String userPhone,
        /** 小程序用户标识；非空时优先小程序通知 */
        String miniAppUserId,
        /** 预计送达时间；不传则默认 30 分钟后 */
        Instant estimatedArrivalAt) {
}
