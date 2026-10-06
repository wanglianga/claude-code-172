package com.community.delivery.domain.enums;

/** 机器人状态 */
public enum RobotStatus {
    IDLE("空闲"),
    IN_DELIVERY("配送中"),
    BLOCKED("受阻"),
    RETURNING("退回中"),
    CHARGING("充电中"),
    OUT_OF_SERVICE("停运");

    private final String label;

    RobotStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
