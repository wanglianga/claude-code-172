package com.community.delivery.web.dto;

import com.community.delivery.domain.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 用户/客服登记投诉（含送错楼栋） */
public record ComplaintRequest(
        /** 关联订单号；无订单的占道投诉可空 */
        String orderNo,
        @NotNull TicketType type,
        @NotBlank String description,
        String complainantPhone,
        String building) {
}
