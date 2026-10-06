package com.community.delivery.domain.enums;

/** 订单类型：外卖、药品、商超 */
public enum OrderType {
    FOOD("外卖"),
    MEDICINE("药品"),
    GROCERY("商超");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
