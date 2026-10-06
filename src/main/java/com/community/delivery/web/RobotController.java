package com.community.delivery.web;

import com.community.delivery.domain.entity.DeliveryOrder;
import com.community.delivery.domain.entity.ElevatorRequest;
import com.community.delivery.domain.entity.GateAuthorization;
import com.community.delivery.domain.enums.EventActor;
import com.community.delivery.service.DeliveryOrchestrator;
import com.community.delivery.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 机器人端 API：下单（业务系统接入）与机器人配送全程动作上报。
 * 每次动作都会在统一配送事件流中追加事件。
 */
@RestController
@RequestMapping("/api/robot")
@Tag(name = "机器人/配送端", description = "下单、门禁、避障、电梯、电量、签收、退回等配送动作")
@SecurityRequirement(name = "basicAuth")
public class RobotController {

    private final DeliveryOrchestrator orchestrator;

    public RobotController(DeliveryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/orders")
    @Operation(summary = "创建配送订单（外卖/药品/商超）")
    public ResponseEntity<DeliveryOrder> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createOrder(req));
    }

    @PostMapping("/orders/{orderNo}/assign")
    @Operation(summary = "分配机器人（不传 robotCode 时自动选择电量最充足的空闲机器人）")
    public DeliveryOrder assign(@PathVariable String orderNo,
                                @RequestParam(required = false) String robotCode) {
        return orchestrator.assignRobot(orderNo, robotCode);
    }

    @PostMapping("/orders/{orderNo}/gate-request")
    @Operation(summary = "到达小区门口申请门禁（夜间限制/物业禁入在此判定）")
    public GateAuthorization requestGate(@PathVariable String orderNo,
                                         @RequestBody(required = false) GatePointRequest body) {
        return orchestrator.requestGate(orderNo, body != null ? body.gatePoint() : null);
    }

    @PostMapping("/orders/{orderNo}/pass-gate")
    @Operation(summary = "门禁开通后通过门禁进入小区")
    public DeliveryOrder passGate(@PathVariable String orderNo) {
        return orchestrator.passGate(orderNo);
    }

    @PostMapping("/orders/{orderNo}/enter-building")
    @Operation(summary = "进入目标楼栋")
    public DeliveryOrder enterBuilding(@PathVariable String orderNo) {
        return orchestrator.enterBuilding(orderNo);
    }

    @PostMapping("/orders/{orderNo}/obstacle")
    @Operation(summary = "上报挡路障碍（老人/儿童、居民聚集、违停车辆），原地避让并可绕行")
    public DeliveryOrder obstacle(@PathVariable String orderNo,
                                  @Valid @RequestBody ObstacleReport report) {
        return orchestrator.reportObstacle(orderNo, report);
    }

    @PostMapping("/orders/{orderNo}/obstacle-clear")
    @Operation(summary = "障碍解除（老人儿童安全通过），恢复行进")
    public DeliveryOrder obstacleClear(@PathVariable String orderNo) {
        return orchestrator.clearObstacle(orderNo);
    }

    @PostMapping("/orders/{orderNo}/construction")
    @Operation(summary = "遇楼道施工：有绕行则路线调整，无绕行转人工接力")
    public DeliveryOrder construction(@PathVariable String orderNo,
                                      @Valid @RequestBody ConstructionReport report) {
        return orchestrator.encounterConstruction(orderNo, report.detourAvailable(), report.note());
    }

    @PostMapping("/orders/{orderNo}/elevator-request")
    @Operation(summary = "进入楼栋后与电梯系统协同呼叫电梯")
    public ElevatorRequest elevatorRequest(@PathVariable String orderNo) {
        return orchestrator.requestElevator(orderNo);
    }

    @PostMapping("/orders/{orderNo}/elevator-full")
    @Operation(summary = "上报电梯满载：礼让居民等待下一班")
    public ElevatorRequest elevatorFull(@PathVariable String orderNo,
                                        @Valid @RequestBody ElevatorFullReport report) {
        return orchestrator.reportElevatorFull(orderNo, report.waitSeconds());
    }

    @PostMapping("/orders/{orderNo}/elevator-board")
    @Operation(summary = "电梯到达后乘梯")
    public DeliveryOrder elevatorBoard(@PathVariable String orderNo) {
        return orchestrator.boardElevator(orderNo);
    }

    @PostMapping("/orders/{orderNo}/arrive-floor")
    @Operation(summary = "到达目标楼层出梯，机器人到户门口并通知用户取件（短信/小程序）")
    public DeliveryOrder arriveFloor(@PathVariable String orderNo) {
        return orchestrator.arriveFloor(orderNo);
    }

    @PostMapping("/orders/{orderNo}/arrive-direct")
    @Operation(summary = "低层无电梯：直接到达户门口并通知取件")
    public DeliveryOrder arriveDirect(@PathVariable String orderNo) {
        return orchestrator.arriveAtDoorDirect(orderNo);
    }

    @PostMapping("/orders/{orderNo}/battery")
    @Operation(summary = "上报电量；低于阈值自动就近停靠并呼叫人工接力")
    public DeliveryOrder battery(@PathVariable String orderNo,
                                 @Valid @RequestBody BatteryReport report) {
        return orchestrator.reportBattery(orderNo, report);
    }

    @PostMapping("/orders/{orderNo}/bin-failure")
    @Operation(summary = "上报载物仓打不开：报修并转人工现场开箱")
    public DeliveryOrder binFailure(@PathVariable String orderNo,
                                    @RequestBody(required = false) ActionRequest body) {
        return orchestrator.reportBinFailure(orderNo, body != null ? body.note() : null);
    }

    @PostMapping("/orders/{orderNo}/bin-opened")
    @Operation(summary = "载物仓成功打开")
    public DeliveryOrder binOpened(@PathVariable String orderNo) {
        return orchestrator.binOpened(orderNo);
    }

    @PostMapping("/orders/{orderNo}/sign")
    @Operation(summary = "用户签收")
    public DeliveryOrder sign(@PathVariable String orderNo,
                              @RequestBody(required = false) ActionRequest body) {
        return orchestrator.sign(orderNo, EventActor.USER, body != null ? body.signerName() : null);
    }

    @PostMapping("/orders/{orderNo}/staff-takeover")
    @Operation(summary = "调度/客服安排人工接力（接力后由工作人员送达并代签收）")
    public DeliveryOrder staffTakeover(@PathVariable String orderNo,
                                       @RequestBody(required = false) ActionRequest body) {
        return orchestrator.staffTakeover(orderNo, body != null ? body.note() : null);
    }

    @PostMapping("/orders/{orderNo}/staff-sign")
    @Operation(summary = "人工接力送达后代用户签收")
    public DeliveryOrder staffSign(@PathVariable String orderNo,
                                   @RequestBody(required = false) ActionRequest body) {
        return orchestrator.sign(orderNo, EventActor.STAFF, body != null ? body.signerName() : null);
    }

    @PostMapping("/orders/{orderNo}/return")
    @Operation(summary = "开始退回（如门禁被拒后、用户超时）")
    public DeliveryOrder startReturn(@PathVariable String orderNo,
                                     @RequestBody(required = false) ActionRequest body) {
        return orchestrator.startReturn(orderNo, body != null ? body.reason() : "人工发起退回");
    }

    @PostMapping("/orders/{orderNo}/returned")
    @Operation(summary = "机器人返回站点，确认退回完成")
    public DeliveryOrder returned(@PathVariable String orderNo) {
        return orchestrator.confirmReturned(orderNo);
    }

    @PostMapping("/timeouts/scan")
    @Operation(summary = "手动触发一次超时扫描（用户超时不取/冷藏超时），生产环境由定时任务每分钟执行")
    public java.util.List<String> scanTimeouts() {
        return orchestrator.scanPickupTimeouts();
    }
}
