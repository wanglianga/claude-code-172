package com.community.delivery.web.dto;

/** 物业规则更新请求（限制夜间运行等） */
public record PropertyRuleRequest(
        boolean robotAllowed,
        boolean nightRestricted,
        /** HH:mm */
        String nightStart,
        /** HH:mm */
        String nightEnd,
        boolean gateManualApproval,
        String remark) {
}
