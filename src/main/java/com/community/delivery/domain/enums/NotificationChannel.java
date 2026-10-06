package com.community.delivery.domain.enums;

public enum NotificationChannel {
    SMS("短信"), MINI_APP("小程序");

    private final String label;

    NotificationChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
