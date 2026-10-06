package com.community.delivery.domain.enums;

public enum TicketStatus {
    OPEN("待处理"), IN_PROGRESS("处理中"), RESOLVED("已处理"), CLOSED("已关闭");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
