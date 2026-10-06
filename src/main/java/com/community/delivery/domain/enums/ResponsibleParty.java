package com.community.delivery.domain.enums;

/** 异常责任归属，供客服端追踪 */
public enum ResponsibleParty {
    PENDING("待判定"),
    PROPERTY("物业"),
    ROBOT("机器人设备"),
    ELEVATOR("电梯系统"),
    USER("用户"),
    STAFF("人工配送员"),
    EXTERNAL("外部因素"),
    NONE("无责(正常波动)");

    private final String label;

    ResponsibleParty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
