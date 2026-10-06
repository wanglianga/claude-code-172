package com.community.delivery.web.dto;

import com.community.delivery.domain.enums.OrderType;

/** 运营端：按订单类型的履约效率 */
public record OrderTypeEfficiency(
        OrderType orderType,
        String label,
        long totalOrders,
        long pickedUp,
        long incidents,
        double fulfillmentRate,
        double incidentRate) {
}
