package com.community.delivery.domain.enums;

public enum TicketType {
    WRONG_BUILDING("送错楼栋"),
    DELIVERY_DELAY("配送超时"),
    CARGO_ISSUE("货品问题"),
    ROBOT_BLOCKING("机器人占道影响通行"),
    OTHER("其他投诉");

    private final String label;

    TicketType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
