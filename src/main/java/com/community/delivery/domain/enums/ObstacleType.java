package com.community.delivery.domain.enums;

/** 道路障碍类型：老人/儿童挡路是需要兼顾居民通行体验的典型场景 */
public enum ObstacleType {
    ELDERLY_CHILD("老人/儿童挡路"),
    RESIDENT_CROWD("居民聚集"),
    CONSTRUCTION("楼道施工"),
    PARKED_BIKE("违停非机动车"),
    OTHER("其他障碍");

    private final String label;

    ObstacleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
