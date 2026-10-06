package com.example.robotdelivery.cs;

/**
 * 异常责任归属，客服端据此追踪异常责任。
 */
public enum Responsibility {
    NONE,            // 待定
    ROBOT_VENDOR,    // 机器人/设备方
    PROPERTY,        // 物业
    ELEVATOR_SYSTEM, // 电梯系统
    USER,            // 用户
    PLATFORM,        // 平台运营
    THIRD_PARTY      // 第三方（如施工方）
}
