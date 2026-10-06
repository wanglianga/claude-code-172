package com.community.delivery.domain.enums;

/**
 * 统一配送事件类型。设备动作与业务结果共用一条事件流：
 * 门禁开通 / 路线调整 / 电梯请求 / 避障停留 / 签收 / 退回 / 客服介入 全部是同一订单事件流上的事件。
 */
public enum EventType {
    // 订单与分配
    ORDER_CREATED("订单创建", false),
    ROBOT_ASSIGNED("机器人分配", false),

    // 门禁
    GATE_REQUESTED("门禁申请", false),
    GATE_GRANTED("门禁开通", true),
    GATE_REJECTED("门禁拒绝", true),
    GATE_PASSED("通过门禁", false),
    ENTER_BUILDING("进入目标楼栋", false),
    NIGHT_RESTRICTION("夜间运行限制", true),

    // 路线与避障
    ROUTE_ADJUSTED("路线调整/绕行", true),
    OBSTACLE_DETECTED("检测到障碍物", true),
    OBSTACLE_WAIT("避障停留", true),
    OBSTACLE_CLEARED("障碍物解除继续行进", false),
    CONSTRUCTION_ENCOUNTERED("遇楼道施工", true),

    // 电梯
    ELEVATOR_REQUESTED("电梯请求", false),
    ELEVATOR_FULL("电梯满载等待", true),
    ELEVATOR_BOARDED("进入电梯", false),
    ELEVATOR_ARRIVED_FLOOR("电梯到达目标楼层", false),

    // 电量与人工接力
    LOW_BATTERY("电量不足", true),
    STAFF_HANDOVER("人工接力", true),

    // 到达与通知
    ARRIVED_NOTIFIED("到达通知用户", false),
    PICKUP_REMINDER("超时取件提醒", true),
    PICKUP_TIMEOUT("用户超时不取", true),
    COLD_CHAIN_TIMEOUT("冷藏订单超时", true),

    // 载物仓与签收
    BIN_OPEN_FAILED("载物仓打不开", true),
    BIN_OPENED("载物仓打开", false),
    SIGNED("用户签收", false),

    // 退回与客服
    RETURN_STARTED("开始退回", false),
    RETURNED("退回完成", false),
    COMPLAINT_FILED("用户投诉", true),
    CUSTOMER_SERVICE_INTERVENED("客服介入", true);

    private final String label;
    /** 是否为异常/对居民通行或时效有影响的事件 */
    private final boolean incident;

    EventType(String label, boolean incident) {
        this.label = label;
        this.incident = incident;
    }

    public String getLabel() {
        return label;
    }

    public boolean isIncident() {
        return incident;
    }
}
