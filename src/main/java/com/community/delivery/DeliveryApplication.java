package com.community.delivery;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "社区配送机器人进楼门禁避障签收服务 API",
                version = "1.0.0",
                description = """
                        外卖/药品/商超订单的社区机器人配送编排：门禁授权、电梯协同、避障、签收与异常处置。
                        所有设备动作（门禁/路线/电梯/避障/签收/退回/客服介入）都追加到**同一配送事件流**。

                        ## 角色
                        - `property` 物业端：审批门禁、设置夜间限制与施工通告、查看机器人对居民通行的影响
                        - `operator` 运营端：按小区/楼栋/订单类型评估投放效率
                        - `support` 客服端：追踪异常责任、登记投诉与处理结果
                        - `robot` 机器人端：上报到达/动作/电量/签收等事件

                        使用右上角 **Authorize**，输入测试账号（见 README），如 `property/property123`。
                        """,
                contact = @Contact(name = "community-delivery")),
        security = @SecurityRequirement(name = "basicAuth"))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class DeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }
}
