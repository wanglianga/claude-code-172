package com.community.delivery.web.dto;

import java.util.List;

/** 物业端：机器人对居民通行影响看板 */
public record PassageImpact(
        Long communityId,
        long blockingEventCount,
        long totalBlockingMs,
        long gateRejectedCount,
        long nightRestrictionCount,
        long obstacleWaitCount,
        long elevatorFullCount,
        long constructionCount,
        List<BuildingPassage> byBuilding,
        List<EventTypeCount> eventBreakdown) {

    public record BuildingPassage(String building, long blockingEvents, long blockingMs) {
    }

    public record EventTypeCount(String eventType, String label, long count, long totalWaitMs) {
    }
}
