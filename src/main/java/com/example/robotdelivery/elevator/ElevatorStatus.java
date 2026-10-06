package com.example.robotdelivery.elevator;

public enum ElevatorStatus {
    PENDING,    // 等待电梯系统派梯
    ASSIGNED,   // 已派梯
    FULL,       // 电梯满载
    TIMEOUT,    // 请求超时
    COMPLETED   // 乘梯完成
}
