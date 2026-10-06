package com.community.delivery.web.dto;

import com.community.delivery.domain.enums.ObstacleType;
import jakarta.validation.constraints.NotNull;

/** 机器人上报障碍物（老人儿童挡路等） */
public record ObstacleReport(
        @NotNull ObstacleType obstacleType,
        /** GATE/COMMUNITY/BUILDING/ELEVATOR/DOOR */
        String location,
        /** 机器人预计等待秒数 */
        @NotNull Long waitSeconds,
        /** 是否存在可行绕行路线 */
        boolean detourAvailable,
        String detail) {
}
