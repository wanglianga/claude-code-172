package com.community.delivery.web.dto;

/** 签收/退回请求 */
public record ActionRequest(String signerName, String reason, String note) {
}
