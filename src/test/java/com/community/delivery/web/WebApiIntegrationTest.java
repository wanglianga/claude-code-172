package com.community.delivery.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST 层集成测试：角色鉴权、端到端下单-门禁-签收链路、Swagger/OpenAPI 可访问。
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebApiIntegrationTest {

    @Autowired MockMvc mvc;

    private static final String ORDER_JSON = """
            {
              "orderType": "FOOD",
              "communityId": 1,
              "targetBuilding": "3号楼",
              "unit": "1单元",
              "targetFloor": 8,
              "room": "801",
              "coldChain": false,
              "userName": "李女士",
              "userPhone": "13900000001"
            }
            """;

    @Test
    void swaggerUi_and_openApi_are_public() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymous_is_unauthorized() throws Exception {
        mvc.perform(get("/api/meta/communities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void role_separation_is_enforced() throws Exception {
        // 机器人账号不能访问运营/物业接口
        mvc.perform(get("/api/operator/efficiency/by-community").with(httpBasic("robot", "robot123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/property/communities/1/rule").with(httpBasic("operator", "operator123")))
                .andExpect(status().isForbidden());
        // 密码错误
        mvc.perform(get("/api/meta/communities").with(httpBasic("robot", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endToEnd_http_flow() throws Exception {
        // 1. 机器人端下单
        String createResp = mvc.perform(post("/api/robot/orders")
                        .with(httpBasic("robot", "robot123"))
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNo").exists())
                .andReturn().getResponse().getContentAsString();
        String orderNo = com.jayway.jsonpath.JsonPath.read(createResp, "$.orderNo");

        // 2. 自动分配机器人
        mvc.perform(post("/api/robot/orders/" + orderNo + "/assign")
                        .with(httpBasic("robot", "robot123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binCode").exists());

        // 3. 申请门禁（阳光花园需人工审批）
        String gateResp = mvc.perform(post("/api/robot/orders/" + orderNo + "/gate-request")
                        .with(httpBasic("robot", "robot123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gatePoint\":\"南门人行门\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String requestNo = com.jayway.jsonpath.JsonPath.read(gateResp, "$.requestNo");

        // 4. 物业审批通过
        mvc.perform(post("/api/property/gate-requests/" + requestNo + "/decision")
                        .with(httpBasic("property", "property123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"reason\":\"核验订单真实\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRANTED"));

        // 5. 进门 -> 电梯 -> 到达 -> 签收
        mvc.perform(post("/api/robot/orders/" + orderNo + "/pass-gate")
                .with(httpBasic("robot", "robot123"))).andExpect(status().isOk());
        mvc.perform(post("/api/robot/orders/" + orderNo + "/enter-building")
                .with(httpBasic("robot", "robot123"))).andExpect(status().isOk());
        mvc.perform(post("/api/robot/orders/" + orderNo + "/elevator-request")
                .with(httpBasic("robot", "robot123"))).andExpect(status().isOk());
        mvc.perform(post("/api/robot/orders/" + orderNo + "/elevator-board")
                .with(httpBasic("robot", "robot123"))).andExpect(status().isOk());
        mvc.perform(post("/api/robot/orders/" + orderNo + "/arrive-floor")
                .with(httpBasic("robot", "robot123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED_WAITING_PICKUP"));
        mvc.perform(post("/api/robot/orders/" + orderNo + "/sign")
                        .with(httpBasic("robot", "robot123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"signerName\":\"李女士\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // 6. 客服端查看统一事件流（任一登录角色均可）
        mvc.perform(get("/api/orders/" + orderNo + "/events")
                        .with(httpBasic("support", "support123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$[0].seq").value(1));

        // 7. 运营端统计
        mvc.perform(get("/api/operator/efficiency/by-community")
                        .with(httpBasic("operator", "operator123")))
                .andExpect(status().isOk());
    }

    @Test
    void validation_error_returns_400() throws Exception {
        mvc.perform(post("/api/robot/orders")
                        .with(httpBasic("robot", "robot123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderType":"FOOD","communityId":1,"targetBuilding":"3号楼",
                                 "targetFloor":8,"userName":"x","userPhone":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
