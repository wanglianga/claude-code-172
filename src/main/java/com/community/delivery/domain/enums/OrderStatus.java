package com.community.delivery.domain.enums;

/**
 * 订单履约状态机：
 * CREATED -> GATE_PENDING -> IN_COMMUNITY -> IN_BUILDING -> ELEVATOR -> ARRIVED_WAITING_PICKUP -> PICKED_UP
 * 任意阶段异常可转 EXCEPTION_HANDOVER（人工接力）或 RETURNING -> RETURNED
 */
public enum OrderStatus {
    CREATED("已创建"),
    GATE_PENDING("门禁申请中"),
    IN_COMMUNITY("小区内配送"),
    IN_BUILDING("楼栋内行进"),
    ELEVATOR_WAITING("等待电梯"),
    ARRIVED_WAITING_PICKUP("已到达等待取件"),
    PICKED_UP("已签收"),
    EXCEPTION_HANDOVER("异常人工接力"),
    RETURNING("退回中"),
    RETURNED("已退回"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
