package com.community.delivery.web.dto;

/** 运营端：按小区的投放效率 */
public record CommunityEfficiency(
        Long communityId,
        long totalOrders,
        long pickedUp,
        long returned,
        long incidents,
        long coldTimeouts,
        long onTime,
        double fulfillmentRate,
        double onTimeRate,
        double incidentRate) {
}
