package com.community.delivery.web.dto;

import com.community.delivery.domain.enums.ResponsibleParty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 客责判定/处理结果 */
public record ResolveTicketRequest(
        @NotNull ResponsibleParty responsibility,
        @NotBlank String resolution,
        /** 送错楼栋投诉时，填写正确楼栋以安排二次配送，可空 */
        String correctBuilding,
        Integer correctFloor) {
}
