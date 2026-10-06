package com.community.delivery.service;

import com.community.delivery.MutableClock;
import com.community.delivery.TestClockConfig;
import com.community.delivery.domain.entity.*;
import com.community.delivery.domain.enums.*;
import com.community.delivery.domain.repository.*;
import com.community.delivery.event.sink.EventMetricsSink;
import com.community.delivery.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 配送编排核心集成测试（H2 + 本地事件总线）：
 * 正常全流程 + 九类异常 + 夜间限制 + 统一事件流完整性 + 通知留痕 + 三端统计。
 */
@SpringBootTest
@Import(TestClockConfig.class)
class DeliveryOrchestratorIntegrationTest {

    @Autowired DeliveryOrchestrator orchestrator;
    @Autowired PropertyService propertyService;
    @Autowired SupportService supportService;
    @Autowired AnalyticsService analyticsService;
    @Autowired Clock clock;
    @Autowired EventMetricsSink metricsSink;

    @Autowired DeliveryOrderRepository orderRepository;
    @Autowired DeliveryEventRepository eventRepository;
    @Autowired GateAuthorizationRepository gateRepository;
    @Autowired ElevatorRequestRepository elevatorRepository;
    @Autowired NotificationRecordRepository notificationRepository;
    @Autowired SupportTicketRepository ticketRepository;
    @Autowired RobotRepository robotRepository;
    @Autowired CommunityRepository communityRepository;

    private static final Long SUNNY = 1L;
    private static final Long RIVER = 2L;

    @BeforeEach
    void reset() {
        ((MutableClock) clock).setInstant(Instant.parse("2026-10-06T10:00:00Z")); // 上海 18:00 白天
        eventRepository.deleteAll();
        gateRepository.deleteAll();
        elevatorRepository.deleteAll();
        notificationRepository.deleteAll();
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        robotRepository.findByCode("R01").ifPresent(r -> {
            r.setStatus(RobotStatus.IDLE); r.setBatteryPercent(96); robotRepository.save(r);
        });
        robotRepository.findByCode("R02").ifPresent(r -> {
            r.setStatus(RobotStatus.IDLE); r.setBatteryPercent(58); robotRepository.save(r);
        });
        robotRepository.findByCode("R03").ifPresent(r -> {
            r.setStatus(RobotStatus.CHARGING); r.setBatteryPercent(15); robotRepository.save(r);
        });
    }

    private CreateOrderRequest orderReq(long communityId, OrderType type, boolean cold, String phoneTail) {
        return new CreateOrderRequest(type, communityId, "3号楼", "1单元", 8, "801",
                cold, "测试用户", "1380000" + phoneTail, null, null);
    }

    /** 从下单走到户门口等待取件（含门禁审批、电梯协同） */
    private String driveToDoor(long communityId) {
        DeliveryOrder order = orchestrator.createOrder(orderReq(communityId, OrderType.FOOD, false, "001"));
        orchestrator.assignRobot(order.getOrderNo(), null);
        GateAuthorization auth = orchestrator.requestGate(order.getOrderNo(), "南门人行门");
        if (auth.getStatus() == GateAuthStatus.PENDING) {
            orchestrator.decideGate(auth.getRequestNo(), true, "核验通过");
        }
        orchestrator.passGate(order.getOrderNo());
        orchestrator.enterBuilding(order.getOrderNo());
        orchestrator.requestElevator(order.getOrderNo());
        orchestrator.boardElevator(order.getOrderNo());
        orchestrator.arriveFloor(order.getOrderNo());
        return order.getOrderNo();
    }

    private List<DeliveryEvent> timeline(String orderNo) {
        return eventRepository.findByOrderNoOrderBySeqAsc(orderNo);
    }

    private boolean hasEvent(String orderNo, EventType type) {
        return timeline(orderNo).stream().anyMatch(e -> e.getEventType() == type);
    }

    // ------------------------------------------------------------------
    // 1. 正常全流程：门禁开通 -> 进楼 -> 电梯 -> 通知 -> 签收
    // ------------------------------------------------------------------
    @Test
    void happyPath_gateElevatorNotifySign() {
        String no = driveToDoor(SUNNY);
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.ARRIVED_WAITING_PICKUP);

        DeliveryOrder signed = orchestrator.sign(no, EventActor.USER, "张女士");
        assertThat(signed.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        assertThat(signed.getSignedAt()).isNotNull();

        List<DeliveryEvent> events = timeline(no);
        // 事件流序号严格连续，保证是"同一配送事件"
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).getSeq()).isEqualTo(i + 1);
        }
        assertThat(events).extracting(DeliveryEvent::getEventType).containsSubsequence(
                EventType.ORDER_CREATED, EventType.ROBOT_ASSIGNED,
                EventType.GATE_REQUESTED, EventType.GATE_GRANTED, EventType.GATE_PASSED,
                EventType.ENTER_BUILDING, EventType.ELEVATOR_REQUESTED,
                EventType.ELEVATOR_BOARDED, EventType.ELEVATOR_ARRIVED_FLOOR,
                EventType.ARRIVED_NOTIFIED, EventType.SIGNED);
        // 所有动作事件都带订单要素快照；电量从"机器人分配"事件起必须有快照
        assertThat(events).allSatisfy(e -> {
            assertThat(e.getOrderNo()).isEqualTo(no);
            assertThat(e.getCommunityId()).isEqualTo(SUNNY);
            assertThat(e.getBuilding()).isEqualTo("3号楼");
        });
        List<DeliveryEvent> afterAssign = events.subList(1, events.size());
        assertThat(afterAssign).allSatisfy(e -> assertThat(e.getBatteryPercent()).isNotNull());
        assertThat(events.get(0).getEventType()).isEqualTo(EventType.ORDER_CREATED);
        assertThat(events.get(0).getBatteryPercent()).isNull();

        // 到达即发短信通知
        List<NotificationRecord> notifs = notificationRepository.findByOrderNoOrderBySentAtAsc(no);
        assertThat(notifs).anyMatch(n -> n.getNotifyType().equals("ARRIVED"))
                .anyMatch(n -> n.getChannel() == NotificationChannel.SMS);

        // 事件已进入（本地）消息总线并被指标 Sink 消费
        assertThat(metricsSink.getTotalConsumed()).isGreaterThan(0);
    }

    // 2. 物业门禁拒绝 -> 订单退回
    @Test
    void gateRejected_thenReturn() {
        DeliveryOrder order = orchestrator.createOrder(orderReq(SUNNY, OrderType.MEDICINE, false, "002"));
        orchestrator.assignRobot(order.getOrderNo(), "R01");
        GateAuthorization auth = orchestrator.requestGate(order.getOrderNo(), "南门人行门");
        orchestrator.decideGate(auth.getRequestNo(), false, "物业临时管控");

        DeliveryOrder reloaded = orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.RETURNING);
        DeliveryEvent rejected = timeline(order.getOrderNo()).stream()
                .filter(e -> e.getEventType() == EventType.GATE_REJECTED).findFirst().orElseThrow();
        assertThat(rejected.getResponsibleParty()).isEqualTo(ResponsibleParty.PROPERTY);
        assertThat(rejected.getDetail()).contains("物业临时管控");

        orchestrator.confirmReturned(order.getOrderNo());
        assertThat(orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.RETURNED);
    }

    // 3. 老人/儿童挡路：原地避障停留 + 绕行，不影响后续签收
    @Test
    void elderlyChildBlocked_waitAndDetour() {
        String no = driveToGatePassed(SUNNY);
        DeliveryOrder after = orchestrator.reportObstacle(no, new ObstacleReport(
                ObstacleType.ELDERLY_CHILD, "COMMUNITY", 35L, true, "一位老人带小孩缓慢行走"));
        assertThat(after.getStatus()).isEqualTo(OrderStatus.IN_COMMUNITY);

        DeliveryEvent wait = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.OBSTACLE_WAIT).findFirst().orElseThrow();
        assertThat(wait.getDurationMs()).isEqualTo(35_000L);
        assertThat(wait.isBlocksResidentPassage()).isTrue();
        assertThat(wait.getResponsibleParty()).isEqualTo(ResponsibleParty.EXTERNAL);
        assertThat(hasEvent(no, EventType.ROUTE_ADJUSTED)).isTrue();
    }

    private String driveToGatePassed(long communityId) {
        DeliveryOrder order = orchestrator.createOrder(orderReq(communityId, OrderType.GROCERY, false, "003"));
        orchestrator.assignRobot(order.getOrderNo(), "R02");
        GateAuthorization auth = orchestrator.requestGate(order.getOrderNo(), "北门");
        if (auth.getStatus() == GateAuthStatus.PENDING) {
            orchestrator.decideGate(auth.getRequestNo(), true, null);
        }
        orchestrator.passGate(order.getOrderNo());
        return order.getOrderNo();
    }

    // 4. 电梯满载：礼让居民等待，再乘梯送达
    @Test
    void elevatorFull_retryThenDeliver() {
        String no = driveToGatePassed(SUNNY);
        orchestrator.enterBuilding(no);
        orchestrator.requestElevator(no);
        orchestrator.reportElevatorFull(no, 45L);
        ElevatorRequest req = elevatorRepository.findTopByOrderNoOrderByIdDesc(no).orElseThrow();
        assertThat(req.getFullRetries()).isEqualTo(1);

        DeliveryEvent full = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.ELEVATOR_FULL).findFirst().orElseThrow();
        assertThat(full.getDurationMs()).isEqualTo(45_000L);
        assertThat(full.isBlocksResidentPassage()).isTrue();
        assertThat(full.getResponsibleParty()).isEqualTo(ResponsibleParty.ELEVATOR);

        orchestrator.boardElevator(no);
        orchestrator.arriveFloor(no);
        orchestrator.sign(no, EventActor.USER, null);
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    // 5. 楼道施工无绕行 -> 人工接力送达
    @Test
    void constructionNoDetour_handover() {
        String no = driveToGatePassed(SUNNY);
        orchestrator.encounterConstruction(no, false, "3号楼一层封路");
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXCEPTION_HANDOVER);
        assertThat(hasEvent(no, EventType.CONSTRUCTION_ENCOUNTERED)).isTrue();
        DeliveryEvent handover = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.STAFF_HANDOVER).findFirst().orElseThrow();
        assertThat(handover.getDetail()).contains("施工");
        orchestrator.sign(no, EventActor.STAFF, "配送员王师傅");
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    // 6. 机器人电量不足 -> 就近停靠 + 人工接力
    @Test
    void lowBattery_handover() {
        String no = driveToGatePassed(SUNNY);
        orchestrator.reportBattery(no, new BatteryReport(12));
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXCEPTION_HANDOVER);
        DeliveryEvent low = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.LOW_BATTERY).findFirst().orElseThrow();
        assertThat(low.getResponsibleParty()).isEqualTo(ResponsibleParty.ROBOT);
        assertThat(low.getBatteryPercent()).isEqualTo(12);
        orchestrator.sign(no, EventActor.STAFF, "接力员");
        assertThat(hasEvent(no, EventType.SIGNED)).isTrue();
    }

    // 7. 用户超时不取 -> 提醒后退回
    @Test
    void pickupTimeout_return() {
        String no = driveToDoor(SUNNY);
        ((MutableClock) clock).advanceSeconds(460);
        orchestrator.scanPickupTimeouts(); // 半程提醒
        assertThat(hasEvent(no, EventType.PICKUP_REMINDER)).isTrue();

        ((MutableClock) clock).advanceSeconds(460); // 累计 920s > 900s
        List<String> handled = orchestrator.scanPickupTimeouts();
        assertThat(handled).contains(no);
        DeliveryOrder order = orderRepository.findByOrderNo(no).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNING);
        DeliveryEvent timeout = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.PICKUP_TIMEOUT).findFirst().orElseThrow();
        assertThat(timeout.getResponsibleParty()).isEqualTo(ResponsibleParty.USER);
        orchestrator.confirmReturned(no);
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    // 8. 载物仓打不开 -> 人工现场开箱交付
    @Test
    void binOpenFailed_handoverAndSign() {
        String no = driveToDoor(SUNNY);
        orchestrator.reportBinFailure(no, "仓门电机无响应");
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXCEPTION_HANDOVER);
        DeliveryEvent fail = timeline(no).stream()
                .filter(e -> e.getEventType() == EventType.BIN_OPEN_FAILED).findFirst().orElseThrow();
        assertThat(fail.getResponsibleParty()).isEqualTo(ResponsibleParty.ROBOT);
        assertThat(fail.getDetail()).contains("仓门电机无响应");
        orchestrator.sign(no, EventActor.STAFF, "现场运维小李");
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    // 9. 冷藏订单超时 -> 冷链风险升级人工
    @Test
    void coldChainTimeout_priorityHandover() {
        DeliveryOrder order = orchestrator.createOrder(orderReq(SUNNY, OrderType.MEDICINE, true, "009"));
        orchestrator.assignRobot(order.getOrderNo(), null); // R01 有冷藏仓
        GateAuthorization auth = orchestrator.requestGate(order.getOrderNo(), "南门");
        orchestrator.decideGate(auth.getRequestNo(), true, null);
        orchestrator.passGate(order.getOrderNo());
        orchestrator.enterBuilding(order.getOrderNo());
        orchestrator.requestElevator(order.getOrderNo());
        orchestrator.boardElevator(order.getOrderNo());
        orchestrator.arriveFloor(order.getOrderNo());
        assertThat(orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow().getBinCode())
                .contains("COLD");

        ((MutableClock) clock).advanceSeconds(605); // 超过 600s 冷链时限
        orchestrator.scanPickupTimeouts();
        DeliveryOrder reloaded = orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow();
        assertThat(reloaded.isColdTimeoutOccurred()).isTrue();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.EXCEPTION_HANDOVER);
        assertThat(hasEvent(order.getOrderNo(), EventType.COLD_CHAIN_TIMEOUT)).isTrue();
    }

    // 10. 投诉送错楼栋 -> 客服判责 + 修正楼栋二次配送
    @Test
    void wrongBuildingComplaint_correctAndRedeliver() {
        String no = driveToDoor(SUNNY);
        SupportTicket ticket = supportService.fileComplaint(new ComplaintRequest(
                no, TicketType.WRONG_BUILDING, "我的外卖被送到3号楼，实际我住9号楼", "13800000001", null));
        assertThat(ticket.getResponsibility()).isEqualTo(ResponsibleParty.ROBOT);

        supportService.resolve(ticket.getTicketNo(),
                new ResolveTicketRequest(ResponsibleParty.ROBOT, "核实为楼栋识别错误，安排二次配送",
                        "9号楼", 12), "客服001");
        DeliveryOrder order = orderRepository.findByOrderNo(no).orElseThrow();
        assertThat(order.getTargetBuilding()).isEqualTo("9号楼");
        assertThat(order.getTargetFloor()).isEqualTo(12);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_COMMUNITY);
        assertThat(hasEvent(no, EventType.COMPLAINT_FILED)).isTrue();
        assertThat(hasEvent(no, EventType.CUSTOMER_SERVICE_INTERVENED)).isTrue();

        // 按正确楼栋重新走完
        orchestrator.enterBuilding(no);
        orchestrator.requestElevator(no);
        orchestrator.boardElevator(no);
        orchestrator.arriveFloor(no);
        orchestrator.sign(no, EventActor.USER, "本人");
        assertThat(orderRepository.findByOrderNo(no).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    // 11. 物业夜间限制：禁行时段拒绝，白天恢复后可正常配送
    @Test
    void nightRestriction_blockedThenAllowedNextDay() {
        // 河畔家园 21:30-06:30 禁行；拨到上海时间 23:00
        ((MutableClock) clock).setInstant(Instant.parse("2026-10-06T15:00:00Z"));
        DeliveryOrder order = orchestrator.createOrder(orderReq(RIVER, OrderType.FOOD, false, "011"));
        orchestrator.assignRobot(order.getOrderNo(), "R02");
        GateAuthorization denied = orchestrator.requestGate(order.getOrderNo(), "东门");
        assertThat(denied.getStatus()).isEqualTo(GateAuthStatus.REJECTED);
        assertThat(orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXCEPTION_HANDOVER);
        assertThat(hasEvent(order.getOrderNo(), EventType.NIGHT_RESTRICTION)).isTrue();

        // 次日 08:00 重试：自动授权
        ((MutableClock) clock).setInstant(Instant.parse("2026-10-07T00:00:00Z"));
        GateAuthorization granted = orchestrator.requestGate(order.getOrderNo(), "东门");
        assertThat(granted.getStatus()).isEqualTo(GateAuthStatus.GRANTED);
        orchestrator.passGate(order.getOrderNo());
        assertThat(orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.IN_COMMUNITY);
    }

    // 12. 物业端更新规则（限制夜间运行）后即时生效
    @Test
    void propertyUpdatesNightRule_thenGateRejectedAtNight() {
        propertyService.updateRule(SUNNY, true, true, "23:00", "05:00", true, "业主大会决议");
        ((MutableClock) clock).setInstant(Instant.parse("2026-10-06T15:30:00Z")); // 上海 23:30
        DeliveryOrder order = orchestrator.createOrder(orderReq(SUNNY, OrderType.GROCERY, false, "012"));
        orchestrator.assignRobot(order.getOrderNo(), "R01");
        GateAuthorization auth = orchestrator.requestGate(order.getOrderNo(), "南门");
        assertThat(auth.getStatus()).isEqualTo(GateAuthStatus.REJECTED);
        assertThat(hasEvent(order.getOrderNo(), EventType.NIGHT_RESTRICTION)).isTrue();
    }

    // 13. 运营端按小区/楼栋/类型统计投放效率
    @Test
    void analytics_efficiencyAggregations() {
        String picked = driveToDoor(SUNNY);
        orchestrator.sign(picked, EventActor.USER, null);

        String rejected = driveToGatePassed(SUNNY);
        orchestrator.startReturn(rejected, "测试退回");
        orchestrator.confirmReturned(rejected);

        var byCommunity = analyticsService.byCommunity(SUNNY, null);
        var sunny = byCommunity.stream().filter(c -> c.communityId() == SUNNY).findFirst().orElseThrow();
        assertThat(sunny.totalOrders()).isGreaterThanOrEqualTo(2);
        assertThat(sunny.pickedUp()).isGreaterThanOrEqualTo(1);
        assertThat(sunny.returned()).isGreaterThanOrEqualTo(1);
        assertThat(sunny.fulfillmentRate()).isGreaterThan(0.0);

        var byBuilding = analyticsService.byBuilding(SUNNY, null);
        assertThat(byBuilding).isNotEmpty();
        assertThat(byBuilding.get(0).building()).isNotBlank();

        var byType = analyticsService.byOrderType(SUNNY, null);
        assertThat(byType).extracting(OrderTypeEfficiency::orderType)
                .contains(OrderType.FOOD, OrderType.GROCERY);
    }

    // 14. 物业端通行影响看板：避障/电梯满载等占道事件被统计
    @Test
    void propertyPassageImpact_reflectsBlockingEvents() {
        String no = driveToGatePassed(SUNNY);
        orchestrator.reportObstacle(no, new ObstacleReport(
                ObstacleType.ELDERLY_CHILD, "COMMUNITY", 40L, false, null));

        PassageImpact impact = propertyService.passageImpact(SUNNY,
                Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(impact.blockingEventCount()).isGreaterThanOrEqualTo(1);
        assertThat(impact.obstacleWaitCount()).isGreaterThanOrEqualTo(1);
        assertThat(impact.totalBlockingMs()).isGreaterThanOrEqualTo(40_000L);
        assertThat(impact.byBuilding()).isNotEmpty();
    }

    // 15. 非法状态转移被拒绝
    @Test
    void illegalTransition_rejected() {
        DeliveryOrder order = orchestrator.createOrder(orderReq(SUNNY, OrderType.FOOD, false, "015"));
        // 尚未申请门禁就通过门禁
        assertThrows(BusinessException.class, () -> orchestrator.passGate(order.getOrderNo()));
        // 无机器人签收
        assertThrows(BusinessException.class, () -> orchestrator.sign(order.getOrderNo(), EventActor.USER, null));
    }

    // 16. 出发前（CREATED）客服更正楼栋：直接改地址，不产生二次配送绕行
    @Test
    void correctBuildingBeforeDeparture_updatesAddressOnly() {
        DeliveryOrder order = orchestrator.createOrder(orderReq(SUNNY, OrderType.GROCERY, false, "016"));
        orchestrator.correctBuilding(order.getOrderNo(), "8号楼", 3, "用户下单时填错楼栋");
        DeliveryOrder reloaded = orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow();
        assertThat(reloaded.getTargetBuilding()).isEqualTo("8号楼");
        assertThat(reloaded.getTargetFloor()).isEqualTo(3);
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(hasEvent(order.getOrderNo(), EventType.CUSTOMER_SERVICE_INTERVENED)).isTrue();
        // 出发前更正不应产生路线调整（还没有路线）
        assertThat(hasEvent(order.getOrderNo(), EventType.ROUTE_ADJUSTED)).isFalse();
    }
}
