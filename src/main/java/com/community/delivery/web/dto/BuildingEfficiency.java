package com.community.delivery.web.dto;

/** 运营端：按楼栋的投放效率 */
public record BuildingEfficiency(
        Long communityId,
        String building,
        long totalOrders,
        long pickedUp,
        long incidents,
        double fulfillmentRate,
        double incidentRate) {
}
