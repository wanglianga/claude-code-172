package com.community.delivery.event.dto;

import java.io.Serializable;

/**
 * 投递到 Kafka topic {@code delivery-events} 的统一事件消息。
 * 与 DeliveryEvent 实体字段对应，消费端（分析/通知投影）不直接依赖数据库实体。
 */
public class DeliveryEventMessage implements Serializable {

    private Long eventId;
    private int seq;
    private String orderNo;
    private Long orderId;
    private Long communityId;
    private String building;
    private Long robotId;
    private String eventType;
    private String actor;
    private String location;
    private String detail;
    private long durationMs;
    private boolean blocksResidentPassage;
    private Integer batteryPercent;
    private String orderStatusSnapshot;
    private String responsibleParty;
    private boolean incident;
    private long occurredAtEpochMilli;

    public DeliveryEventMessage() {
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public boolean isBlocksResidentPassage() { return blocksResidentPassage; }
    public void setBlocksResidentPassage(boolean blocksResidentPassage) { this.blocksResidentPassage = blocksResidentPassage; }
    public Integer getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }
    public String getOrderStatusSnapshot() { return orderStatusSnapshot; }
    public void setOrderStatusSnapshot(String orderStatusSnapshot) { this.orderStatusSnapshot = orderStatusSnapshot; }
    public String getResponsibleParty() { return responsibleParty; }
    public void setResponsibleParty(String responsibleParty) { this.responsibleParty = responsibleParty; }
    public boolean isIncident() { return incident; }
    public void setIncident(boolean incident) { this.incident = incident; }
    public long getOccurredAtEpochMilli() { return occurredAtEpochMilli; }
    public void setOccurredAtEpochMilli(long occurredAtEpochMilli) { this.occurredAtEpochMilli = occurredAtEpochMilli; }
}
