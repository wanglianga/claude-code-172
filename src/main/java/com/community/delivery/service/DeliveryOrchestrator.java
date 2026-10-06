package com.community.delivery.service;

import com.community.delivery.config.AppRulesProperties;
import com.community.delivery.domain.entity.*;
import com.community.delivery.domain.enums.*;
import com.community.delivery.domain.repository.*;
import com.community.delivery.web.dto.BatteryReport;
import com.community.delivery.web.dto.CreateOrderRequest;
import com.community.delivery.web.dto.ObstacleReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 配送编排核心服务。机器人不是孤立设备：这里把门禁、电梯、物业规则、用户取件与人工接力串成一条业务流，
 * 并且每个设备动作/业务结果都通过 {@link EventRecorder} 追加到同一配送事件流。
 *
 * 覆盖的九类异常：门禁拒绝、老人/儿童挡路、电梯满载、楼道施工、电量不足、
 * 用户超时不取、载物仓打不开、冷藏订单超时、投诉送错楼栋；外加物业夜间运行限制。
 */
@Service
public class DeliveryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DeliveryOrchestrator.class);

    public static final String LOC_GATE = "GATE";
    public static final String LOC_COMMUNITY = "COMMUNITY";
    public static final String LOC_BUILDING = "BUILDING";
    public static final String LOC_ELEVATOR = "ELEVATOR";
    public static final String LOC_DOOR = "DOOR";

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryEventRepository eventRepository;
    private final RobotRepository robotRepository;
    private final CommunityRepository communityRepository;
    private final PropertyRuleRepository ruleRepository;
    private final ConstructionNoticeRepository constructionRepository;
    private final GateAuthorizationRepository gateRepository;
    private final ElevatorRequestRepository elevatorRepository;
    private final EventRecorder eventRecorder;
    private final NotificationService notificationService;
    private final AppRulesProperties rules;
    private final Clock clock;

    public DeliveryOrchestrator(DeliveryOrderRepository orderRepository,
                                DeliveryEventRepository eventRepository,
                                RobotRepository robotRepository,
                                CommunityRepository communityRepository,
                                PropertyRuleRepository ruleRepository,
                                ConstructionNoticeRepository constructionRepository,
                                GateAuthorizationRepository gateRepository,
                                ElevatorRequestRepository elevatorRepository,
                                EventRecorder eventRecorder,
                                NotificationService notificationService,
                                AppRulesProperties rules,
                                Clock clock) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.robotRepository = robotRepository;
        this.communityRepository = communityRepository;
        this.ruleRepository = ruleRepository;
        this.constructionRepository = constructionRepository;
        this.gateRepository = gateRepository;
        this.elevatorRepository = elevatorRepository;
        this.eventRecorder = eventRecorder;
        this.notificationService = notificationService;
        this.rules = rules;
        this.clock = clock;
    }

    // ---------------------------------------------------------------------
    // 下单与机器人分配
    // ---------------------------------------------------------------------

    @Transactional
    public DeliveryOrder createOrder(CreateOrderRequest req) {
        Community community = communityRepository.findById(req.communityId())
                .orElseThrow(() -> new NotFoundException("小区不存在: " + req.communityId()));
        if (!community.isActive()) {
            throw new BusinessException("COMMUNITY_INACTIVE", "小区已停用: " + community.getName());
        }

        DeliveryOrder order = new DeliveryOrder();
        order.setOrderNo(generateOrderNo());
        order.setOrderType(req.orderType());
        order.setCommunityId(req.communityId());
        order.setTargetBuilding(req.targetBuilding());
        order.setUnit(req.unit());
        order.setTargetFloor(Math.max(1, req.targetFloor()));
        order.setRoom(req.room());
        order.setColdChain(req.coldChain());
        order.setUserName(req.userName());
        order.setUserPhone(req.userPhone());
        order.setMiniAppUserId(req.miniAppUserId());
        Instant eta = req.estimatedArrivalAt() != null
                ? req.estimatedArrivalAt()
                : Instant.now(clock).plusSeconds(1800);
        order.setEstimatedArrivalAt(eta);
        order.setStatus(OrderStatus.CREATED);
        DeliveryOrder saved = orderRepository.save(order);

        eventRecorder.record(saved, EventType.ORDER_CREATED, EventActor.SYSTEM, null,
                String.format("%s订单已创建，收货%s %s，冷藏=%s，预计送达%s",
                        req.orderType().getLabel(), community.getName(), req.targetBuilding(),
                        req.coldChain() ? "是" : "否", eta),
                0, false, ResponsibleParty.NONE);
        log.info("订单创建 {} type={} 冷藏={}", saved.getOrderNo(), req.orderType(), req.coldChain());
        return saved;
    }

    @Transactional
    public DeliveryOrder assignRobot(String orderNo, String robotCode) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.CREATED, OrderStatus.GATE_PENDING);

        Robot robot;
        if (robotCode != null && !robotCode.isBlank()) {
            robot = robotRepository.findByCode(robotCode)
                    .orElseThrow(() -> new NotFoundException("机器人不存在: " + robotCode));
            if (robot.getStatus() != RobotStatus.IDLE) {
                throw new BusinessException("ROBOT_BUSY", "机器人非空闲状态: " + robot.getCode());
            }
        } else {
            robot = robotRepository.findByStatus(RobotStatus.IDLE).stream()
                    .filter(r -> !order.isColdChain() || r.getColdBinCapacity() > 0)
                    // 电量充足者优先
                    .max((a, b) -> Integer.compare(a.getBatteryPercent(), b.getBatteryPercent()))
                    .orElseThrow(() -> new BusinessException("NO_ROBOT", "暂无可用机器人"));
        }
        if (order.isColdChain() && robot.getColdBinCapacity() <= 0) {
            throw new BusinessException("NO_COLD_BIN", "机器人无冷藏载物仓，不能接冷藏订单");
        }

        robot.setStatus(RobotStatus.IN_DELIVERY);
        robot.setCurrentCommunityId(order.getCommunityId());
        order.setRobotId(robot.getId());
        order.setAssignedBatteryPercent(robot.getBatteryPercent());
        // 冷藏单分配冷藏仓，否则普通仓
        order.setBinCode(robot.getCode() + (order.isColdChain() ? "-COLD-1" : "-BIN-1"));
        orderRepository.save(order);
        robotRepository.save(robot);

        eventRecorder.record(order, robot, EventType.ROBOT_ASSIGNED, EventActor.SYSTEM, null,
                String.format("分配机器人%s，电量%d%%，载物仓%s",
                        robot.getCode(), robot.getBatteryPercent(), order.getBinCode()),
                0, false, ResponsibleParty.NONE);
        return order;
    }

    // ---------------------------------------------------------------------
    // 门禁：申请 / 物业审批 / 通过
    // ---------------------------------------------------------------------

    @Transactional
    public GateAuthorization requestGate(String orderNo, String gatePoint) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.CREATED, OrderStatus.EXCEPTION_HANDOVER);
        if (order.getRobotId() == null) {
            assignRobot(orderNo, null);
            order = getOrder(orderNo);
        }
        Robot robot = getRobot(order);
        PropertyRule rule = getOrCreateRule(order.getCommunityId());
        Instant now = Instant.now(clock);

        // 异常 10（物业规则）：夜间限制
        if (rule.isNightRestricted() && isWithinNightWindow(now, rule)) {
            order.setStatus(OrderStatus.EXCEPTION_HANDOVER);
            robot.setStatus(RobotStatus.BLOCKED);
            GateAuthorization denied = saveGateRequest(order, robot, gatePoint,
                    GateAuthStatus.REJECTED, "夜间禁止机器人运行，等待白天重试", now);
            eventRecorder.record(order, robot, EventType.NIGHT_RESTRICTION, EventActor.PROPERTY, LOC_GATE,
                    String.format("到达%s但处于物业夜间禁行时段(%s-%s)，门禁不予开通",
                            gateText(gatePoint), timeText(rule.getNightStart()), timeText(rule.getNightEnd())),
                    0, false, ResponsibleParty.PROPERTY);
            eventRecorder.record(order, robot, EventType.GATE_REJECTED, EventActor.PROPERTY, LOC_GATE,
                    "门禁拒绝：夜间运行限制", 0, false, ResponsibleParty.PROPERTY);
            robotRepository.save(robot);
            return denied;
        }

        // 物业整体禁止机器人
        if (!rule.isRobotAllowed()) {
            robot.setStatus(RobotStatus.BLOCKED);
            GateAuthorization denied = saveGateRequest(order, robot, gatePoint,
                    GateAuthStatus.REJECTED, "物业禁止机器人进入小区", now);
            order.setStatus(OrderStatus.RETURNING);
            eventRecorder.record(order, robot, EventType.GATE_REJECTED, EventActor.PROPERTY, LOC_GATE,
                    "门禁拒绝：物业暂停机器人准入，订单退回", 0, false, ResponsibleParty.PROPERTY);
            robotRepository.save(robot);
            return denied;
        }

        // 正常申请：人工审批 -> PENDING；自动授权 -> GRANTED
        GateAuthorization auth = saveGateRequest(order, robot, gatePoint, GateAuthStatus.PENDING, null, now);
        order.setStatus(OrderStatus.GATE_PENDING);
        eventRecorder.record(order, robot, EventType.GATE_REQUESTED, EventActor.ROBOT, LOC_GATE,
                String.format("机器人到达%s，申请门禁（订单%s）", gateText(gatePoint), order.getOrderNo()),
                0, false, ResponsibleParty.NONE);

        if (!rule.isGateManualApproval()) {
            grantGateInternal(order, robot, auth, "小区免审批自动授权", now);
        }
        return auth;
    }

    @Transactional
    public GateAuthorization decideGate(String requestNo, boolean approved, String reason) {
        GateAuthorization auth = gateRepository.findByRequestNo(requestNo)
                .orElseThrow(() -> new NotFoundException("门禁申请不存在: " + requestNo));
        DeliveryOrder order = getOrder(auth.getOrderNo());
        Robot robot = getRobot(order);

        if (auth.getStatus() != GateAuthStatus.PENDING) {
            throw new BusinessException("GATE_DECIDED", "门禁申请已处理: " + auth.getStatus());
        }
        Instant now = Instant.now(clock);
        auth.setDecidedAt(now);

        if (!approved) {
            // 异常 1：物业门禁拒绝
            auth.setStatus(GateAuthStatus.REJECTED);
            auth.setRejectReason(reason);
            gateRepository.save(auth);
            order.setStatus(OrderStatus.RETURNING);
            robot.setStatus(RobotStatus.RETURNING);
            eventRecorder.record(order, robot, EventType.GATE_REJECTED, EventActor.PROPERTY, LOC_GATE,
                    "门禁拒绝：" + Optional.ofNullable(reason).orElse("物业未说明原因"),
                    0, false, ResponsibleParty.PROPERTY);
            robotRepository.save(robot);
            return auth;
        }
        grantGateInternal(order, robot, auth, reason, now);
        return auth;
    }

    private void grantGateInternal(DeliveryOrder order, Robot robot, GateAuthorization auth, String reason, Instant now) {
        auth.setStatus(GateAuthStatus.GRANTED);
        auth.setExpiresAt(now.plusSeconds(300)); // 一次性授权 5 分钟有效
        auth.setDecidedAt(now);
        auth.setRejectReason(null);
        gateRepository.save(auth);
        eventRecorder.record(order, robot, EventType.GATE_GRANTED, EventActor.PROPERTY, LOC_GATE,
                "门禁开通：" + Optional.ofNullable(reason).orElse("物业批准"), 0, false, ResponsibleParty.NONE);
    }

    @Transactional
    public DeliveryOrder passGate(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.GATE_PENDING);
        GateAuthorization auth = gateRepository.findTopByOrderNoOrderByIdDesc(orderNo)
                .orElseThrow(() -> new BusinessException("NO_GATE_AUTH", "尚未申请门禁"));
        if (auth.getStatus() != GateAuthStatus.GRANTED) {
            throw new BusinessException("GATE_NOT_GRANTED", "门禁未授权，当前状态: " + auth.getStatus());
        }
        Robot robot = getRobot(order);
        robot.setStatus(RobotStatus.IN_DELIVERY);
        order.setStatus(OrderStatus.IN_COMMUNITY);
        eventRecorder.record(order, robot, EventType.GATE_PASSED, EventActor.ROBOT, LOC_GATE,
                String.format("通过%s进入小区", gateText(auth.getGatePoint())), 0, false, ResponsibleParty.NONE);
        robotRepository.save(robot);
        return order;
    }

    // ---------------------------------------------------------------------
    // 园区/楼栋行进、避障与施工
    // ---------------------------------------------------------------------

    @Transactional
    public DeliveryOrder enterBuilding(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.IN_COMMUNITY);
        Robot robot = getRobot(order);
        order.setStatus(OrderStatus.IN_BUILDING);
        eventRecorder.record(order, robot, EventType.ENTER_BUILDING, EventActor.ROBOT, LOC_BUILDING,
                String.format("进入目标楼栋%s", order.getTargetBuilding()), 0, false, ResponsibleParty.NONE);
        return order;
    }

    /**
     * 异常 2：老人/儿童（或居民、违停车辆等）挡路。
     * 机器人先原地避障停留（不鸣笛不催行，优先照顾居民通行体验），有绕行路线则绕行。
     */
    @Transactional
    public DeliveryOrder reportObstacle(String orderNo, ObstacleReport report) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.IN_COMMUNITY, OrderStatus.IN_BUILDING,
                OrderStatus.ELEVATOR_WAITING);
        Robot robot = getRobot(order);
        String location = report.location() != null ? report.location() : LOC_COMMUNITY;
        long waitMs = Math.max(0, report.waitSeconds()) * 1000L;

        robot.setStatus(RobotStatus.BLOCKED);
        // 老人/儿童属于外部/居民因素，不计物业责任；违停非机动车归物业管理责任
        ResponsibleParty party = report.obstacleType() == ObstacleType.PARKED_BIKE
                ? ResponsibleParty.PROPERTY : ResponsibleParty.EXTERNAL;

        eventRecorder.record(order, robot, EventType.OBSTACLE_DETECTED, EventActor.ROBOT, location,
                String.format("检测到障碍：%s。%s 机器人原地避让，不催促居民",
                        report.obstacleType().getLabel(),
                        report.detail() != null ? report.detail() : ""),
                0, true, party);
        eventRecorder.record(order, robot, EventType.OBSTACLE_WAIT, EventActor.ROBOT, location,
                String.format("避障停留%d秒，等待居民安全通过", report.waitSeconds()),
                waitMs, true, party);

        if (report.detourAvailable()) {
            order.setStatus(statusBeforeBlocked(order, location));
            robot.setStatus(RobotStatus.IN_DELIVERY);
            eventRecorder.record(order, robot, EventType.ROUTE_ADJUSTED, EventActor.ROBOT, location,
                    "居民通行缓慢，机器人改走备用路线绕行，避免占道", waitMs, false, party);
        }
        robotRepository.save(robot);
        return order;
    }

    /** 障碍物解除（老人儿童通过后），继续行进 */
    @Transactional
    public DeliveryOrder clearObstacle(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        Robot robot = getRobot(order);
        String location = lastLocation(order);
        order.setStatus(statusBeforeBlocked(order, location));
        robot.setStatus(RobotStatus.IN_DELIVERY);
        eventRecorder.record(order, robot, EventType.OBSTACLE_CLEARED, EventActor.ROBOT, location,
                "前方行人已安全通过，恢复行进", 0, false, ResponsibleParty.NONE);
        robotRepository.save(robot);
        return order;
    }

    /**
     * 异常 4：楼道施工。有绕行（其他单元/楼梯）则路线调整；无绕行则人工接力。
     */
    @Transactional
    public DeliveryOrder encounterConstruction(String orderNo, boolean detourAvailable, String note) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.IN_COMMUNITY, OrderStatus.IN_BUILDING);
        Robot robot = getRobot(order);

        List<ConstructionNotice> notices = constructionRepository
                .findByCommunityIdAndActiveTrue(order.getCommunityId()).stream()
                .filter(n -> n.getBuilding() == null || n.getBuilding().equals(order.getTargetBuilding()))
                .toList();
        String noticeText = notices.isEmpty() ? "" :
                "（物业施工通告：" + notices.get(0).getDescription() + "）";

        eventRecorder.record(order, robot, EventType.CONSTRUCTION_ENCOUNTERED, EventActor.ROBOT, LOC_BUILDING,
                String.format("楼栋%s通道施工，无法按原路线进楼%s%s",
                        order.getTargetBuilding(), noticeText, note != null ? " " + note : ""),
                0, true, ResponsibleParty.PROPERTY);

        if (detourAvailable) {
            eventRecorder.record(order, robot, EventType.ROUTE_ADJUSTED, EventActor.ROBOT, LOC_BUILDING,
                    "改走备用单元通道/施工侧临时通道绕行", 0, false, ResponsibleParty.PROPERTY);
        } else {
            handoverInternal(order, robot, "楼道施工且无绕行路线，转人工步行接力配送");
        }
        return order;
    }

    // ---------------------------------------------------------------------
    // 电梯协同
    // ---------------------------------------------------------------------

    @Transactional
    public ElevatorRequest requestElevator(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.IN_BUILDING, OrderStatus.ELEVATOR_WAITING);
        Robot robot = getRobot(order);
        order.setStatus(OrderStatus.ELEVATOR_WAITING);

        ElevatorRequest request = new ElevatorRequest();
        request.setOrderNo(orderNo);
        request.setCommunityId(order.getCommunityId());
        request.setBuilding(order.getTargetBuilding());
        request.setRobotId(robot.getId());
        request.setTargetFloor(order.getTargetFloor());
        elevatorRepository.save(request);

        eventRecorder.record(order, robot, EventType.ELEVATOR_REQUESTED, EventActor.ROBOT, LOC_ELEVATOR,
                String.format("与电梯系统协同呼叫电梯，目标%d层", order.getTargetFloor()),
                0, false, ResponsibleParty.NONE);
        return request;
    }

    /** 异常 3：电梯满载（居民优先），机器人礼让等待下一班 */
    @Transactional
    public ElevatorRequest reportElevatorFull(String orderNo, long waitSeconds) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ELEVATOR_WAITING);
        Robot robot = getRobot(order);
        ElevatorRequest request = elevatorRepository.findTopByOrderNoOrderByIdDesc(orderNo)
                .orElseThrow(() -> new BusinessException("NO_ELEVATOR_REQ", "尚未呼叫电梯"));

        request.setStatus(ElevatorRequestStatus.FULL_REJECTED);
        request.setFullRetries(request.getFullRetries() + 1);
        elevatorRepository.save(request);

        eventRecorder.record(order, robot, EventType.ELEVATOR_FULL, EventActor.ELEVATOR, LOC_ELEVATOR,
                String.format("电梯满载（第%d次），礼让居民，等待%d秒后改乘下一班",
                        request.getFullRetries(), waitSeconds),
                waitSeconds * 1000L, true, ResponsibleParty.ELEVATOR);

        // 重新排队
        request.setStatus(ElevatorRequestStatus.REQUESTED);
        elevatorRepository.save(request);
        return request;
    }

    @Transactional
    public DeliveryOrder boardElevator(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ELEVATOR_WAITING);
        Robot robot = getRobot(order);
        ElevatorRequest request = elevatorRepository.findTopByOrderNoOrderByIdDesc(orderNo).orElseThrow();
        request.setStatus(ElevatorRequestStatus.BOARDED);
        request.setBoardedAt(Instant.now(clock));
        elevatorRepository.save(request);
        eventRecorder.record(order, robot, EventType.ELEVATOR_BOARDED, EventActor.ELEVATOR, LOC_ELEVATOR,
                "电梯到达，机器人礼让居民后进入电梯", 0, false, ResponsibleParty.NONE);
        return order;
    }

    @Transactional
    public DeliveryOrder arriveFloor(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ELEVATOR_WAITING, OrderStatus.IN_BUILDING);
        Robot robot = getRobot(order);
        ElevatorRequest request = elevatorRepository.findTopByOrderNoOrderByIdDesc(orderNo).orElse(null);
        if (request != null) {
            request.setStatus(ElevatorRequestStatus.ARRIVED);
            request.setArrivedAt(Instant.now(clock));
            elevatorRepository.save(request);
            eventRecorder.record(order, robot, EventType.ELEVATOR_ARRIVED_FLOOR, EventActor.ELEVATOR, LOC_ELEVATOR,
                    String.format("电梯到达%d层，机器人出梯", order.getTargetFloor()), 0, false, ResponsibleParty.NONE);
        }
        return arriveAtDoor(order, robot);
    }

    /** 低层无电梯场景直接到达门口 */
    @Transactional
    public DeliveryOrder arriveAtDoorDirect(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.IN_BUILDING);
        return arriveAtDoor(order, getRobot(order));
    }

    private DeliveryOrder arriveAtDoor(DeliveryOrder order, Robot robot) {
        order.setStatus(OrderStatus.ARRIVED_WAITING_PICKUP);
        order.setArrivedAt(Instant.now(clock));
        orderRepository.save(order);
        String channel = order.getMiniAppUserId() != null ? "小程序" : "短信";
        String content = String.format("您的%s已由配送机器人送到%s%s层%s，请在15分钟内取件，取件码后四位%s",
                order.getOrderType().getLabel(), order.getTargetBuilding(), order.getTargetFloor(),
                order.getRoom() != null ? order.getRoom() : "门口", orderNoTail(order));
        eventRecorder.record(order, robot, EventType.ARRIVED_NOTIFIED, EventActor.ROBOT, LOC_DOOR,
                String.format("到达户门口，已通过%s通知用户取件", channel), 0, false, ResponsibleParty.NONE);
        notificationService.notify(order, "ARRIVED", content);
        return order;
    }

    // ---------------------------------------------------------------------
    // 电量
    // ---------------------------------------------------------------------

    /** 异常 5：机器人电量不足，停止自主配送并呼叫人工接力 */
    @Transactional
    public DeliveryOrder reportBattery(String orderNo, BatteryReport report) {
        DeliveryOrder order = getOrder(orderNo);
        Robot robot = getRobot(order);
        robot.setBatteryPercent(report.batteryPercent());
        robotRepository.save(robot);
        if (report.batteryPercent() > rules.getLowBatteryThreshold()) {
            return order; // 电量仍充足，仅更新
        }
        eventRecorder.record(order, robot, EventType.LOW_BATTERY, EventActor.ROBOT,
                lastLocation(order),
                String.format("电量仅剩%d%%，低于阈值%d%%，就近安全停靠并呼叫人工接力",
                        report.batteryPercent(), rules.getLowBatteryThreshold()),
                0, true, ResponsibleParty.ROBOT);
        handoverInternal(order, robot, "电量不足，人工接力完成剩余配送");
        return order;
    }

    @Transactional
    public DeliveryOrder staffTakeover(String orderNo, String note) {
        DeliveryOrder order = getOrder(orderNo);
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        handoverInternal(order, robot, note != null ? note : "客服/调度安排人工接力");
        return order;
    }

    private void handoverInternal(DeliveryOrder order, Robot robot, String note) {
        order.setStatus(OrderStatus.EXCEPTION_HANDOVER);
        orderRepository.save(order);
        if (robot != null) {
            robot.setStatus(RobotStatus.OUT_OF_SERVICE);
            robotRepository.save(robot);
        }
        eventRecorder.record(order, robot, EventType.STAFF_HANDOVER, EventActor.STAFF, lastLocation(order),
                note, 0, false, ResponsibleParty.STAFF);
    }

    // ---------------------------------------------------------------------
    // 载物仓与签收
    // ---------------------------------------------------------------------

    /** 异常 7：载物仓打不开，机器人报修并转人工现场处置 */
    @Transactional
    public DeliveryOrder reportBinFailure(String orderNo, String note) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ARRIVED_WAITING_PICKUP, OrderStatus.EXCEPTION_HANDOVER);
        Robot robot = getRobot(order);
        eventRecorder.record(order, robot, EventType.BIN_OPEN_FAILED, EventActor.ROBOT, LOC_DOOR,
                String.format("载物仓%s无法弹开，用户无法取件；通知运维并转人工现场开锁%s",
                        order.getBinCode(), note != null ? "：" + note : ""),
                0, true, ResponsibleParty.ROBOT);
        handoverInternal(order, robot, "载物仓故障，人工现场开箱交付");
        return order;
    }

    @Transactional
    public DeliveryOrder binOpened(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ARRIVED_WAITING_PICKUP, OrderStatus.EXCEPTION_HANDOVER);
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        eventRecorder.record(order, robot, EventType.BIN_OPENED, EventActor.ROBOT, LOC_DOOR,
                "载物仓" + order.getBinCode() + "已打开", 0, false, ResponsibleParty.NONE);
        return order;
    }

    /** 用户签收（人工接力时由工作人员代签收，actor=STAFF） */
    @Transactional
    public DeliveryOrder sign(String orderNo, EventActor actor, String signerName) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.ARRIVED_WAITING_PICKUP, OrderStatus.EXCEPTION_HANDOVER);
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);

        eventRecorder.record(order, robot, EventType.BIN_OPENED, actor, LOC_DOOR,
                "交付前打开载物仓" + order.getBinCode(), 0, false, ResponsibleParty.NONE);
        Instant now = Instant.now(clock);
        order.setStatus(OrderStatus.PICKED_UP);
        order.setSignedAt(now);
        orderRepository.save(order);
        eventRecorder.record(order, robot, EventType.SIGNED, actor, LOC_DOOR,
                String.format("%s已签收（%s）", actor == EventActor.STAFF ? "人工接力交付，用户" : "用户",
                        signerName != null ? signerName : "本人"),
                0, false, ResponsibleParty.NONE);
        if (robot != null) {
            robot.setStatus(RobotStatus.IDLE);
            robotRepository.save(robot);
        }
        log.info("订单签收 {} actor={}", orderNo, actor);
        return order;
    }

    // ---------------------------------------------------------------------
    // 退回
    // ---------------------------------------------------------------------

    @Transactional
    public DeliveryOrder startReturn(String orderNo, String reason) {
        DeliveryOrder order = getOrder(orderNo);
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        order.setStatus(OrderStatus.RETURNING);
        if (robot != null) {
            robot.setStatus(RobotStatus.RETURNING);
            robotRepository.save(robot);
        }
        eventRecorder.record(order, robot, EventType.RETURN_STARTED, EventActor.ROBOT, lastLocation(order),
                "开始退回：" + reason, 0, false, ResponsibleParty.USER);
        return order;
    }

    @Transactional
    public DeliveryOrder confirmReturned(String orderNo) {
        DeliveryOrder order = getOrder(orderNo);
        requireStatus(order, OrderStatus.RETURNING);
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(Instant.now(clock));
        orderRepository.save(order);
        eventRecorder.record(order, robot, EventType.RETURNED, EventActor.ROBOT, null,
                "机器人返回站点，订单退回完成", 0, false, ResponsibleParty.NONE);
        if (robot != null) {
            robot.setStatus(RobotStatus.IDLE);
            robotRepository.save(robot);
        }
        return order;
    }

    // ---------------------------------------------------------------------
    // 定时扫描：用户超时不取 / 冷藏超时
    // ---------------------------------------------------------------------

    /**
     * 扫描已到达等待取件的订单：
     * - 异常 6：超过取件时限（默认 15 分钟）→ 用户超时不取，提醒后退回；
     * - 异常 8：冷藏订单超过冷藏等待时限（默认 30 分钟，且不晚于取件时限之后太久）→ 冷链风险，人工优先处置。
     * @return 本轮被处置的订单号
     */
    @Transactional
    public List<String> scanPickupTimeouts() {
        Instant now = Instant.now(clock);
        List<DeliveryOrder> waiting = orderRepository.findByStatus(OrderStatus.ARRIVED_WAITING_PICKUP);
        return waiting.stream()
                .filter(o -> o.getArrivedAt() != null)
                .filter(o -> {
                    long waited = now.getEpochSecond() - o.getArrivedAt().getEpochSecond();
                    // 冷藏超时优先判定
                    if (o.isColdChain() && !o.isColdTimeoutOccurred()
                            && waited >= rules.getColdChainTimeoutSeconds()) {
                        handleColdChainTimeout(o, waited);
                        return false;
                    }
                    if (waited >= rules.getPickupTimeoutSeconds()) {
                        handlePickupTimeout(o, waited);
                        return true;
                    }
                    // 到达后提醒一次（超过一半时限）
                    if (waited >= rules.getPickupTimeoutSeconds() / 2 && !reminded(o)) {
                        sendReminder(o);
                    }
                    return false;
                })
                .map(DeliveryOrder::getOrderNo)
                .toList();
    }

    private boolean reminded(DeliveryOrder order) {
        return eventRepository.existsByOrderIdAndEventType(order.getId(), EventType.PICKUP_REMINDER);
    }

    private void sendReminder(DeliveryOrder order) {
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        eventRecorder.record(order, robot, EventType.PICKUP_REMINDER, EventActor.SYSTEM, LOC_DOOR,
                "取件时限将到，再次提醒用户尽快取件", 0, false, ResponsibleParty.NONE);
        notificationService.notify(order, "REMINDER",
                String.format("提醒：您的%s仍在%s等待取件，超时将退回。",
                        order.getOrderType().getLabel(), order.getTargetBuilding()));
    }

    private void handlePickupTimeout(DeliveryOrder order, long waitedSeconds) {
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        eventRecorder.record(order, robot, EventType.PICKUP_TIMEOUT, EventActor.USER, LOC_DOOR,
                String.format("用户超过%d分钟未取件（等待%d秒），订单退回",
                        rules.getPickupTimeoutSeconds() / 60, waitedSeconds),
                waitedSeconds * 1000L, true, ResponsibleParty.USER);
        notificationService.notify(order, "EXCEPTION", "您的订单超时未取，机器人已开始退回，请联系客服处理。");
        order.setStatus(OrderStatus.RETURNING);
        if (robot != null) {
            robot.setStatus(RobotStatus.RETURNING);
            robotRepository.save(robot);
        }
        eventRecorder.record(order, robot, EventType.RETURN_STARTED, EventActor.ROBOT, LOC_DOOR,
                "用户超时不取，载物仓原路返回站点", 0, false, ResponsibleParty.USER);
    }

    private void handleColdChainTimeout(DeliveryOrder order, long waitedSeconds) {
        Robot robot = robotRepository.findById(order.getRobotId()).orElse(null);
        order.setColdTimeoutOccurred(true);
        orderRepository.save(order);
        eventRecorder.record(order, robot, EventType.COLD_CHAIN_TIMEOUT, EventActor.ROBOT, LOC_DOOR,
                String.format("冷藏订单等待%d秒超过冷链时限%d秒，存在变质风险，立即通知用户并人工处置",
                        waitedSeconds, rules.getColdChainTimeoutSeconds()),
                waitedSeconds * 1000L, true, ResponsibleParty.ROBOT);
        notificationService.notify(order, "EXCEPTION",
                "您的冷藏药品/生鲜即将超过安全保鲜时限，请立即取件；工作人员已同步到场处理。");
        handoverInternal(order, robot, "冷藏超时，人工到场优先交付或按规范处置货品");
    }

    // ---------------------------------------------------------------------
    // 投诉送错楼栋：客服判责 + 修正楼栋二次配送
    // ---------------------------------------------------------------------

    @Transactional
    public DeliveryOrder correctBuilding(String orderNo, String correctBuilding, Integer correctFloor, String note) {
        DeliveryOrder order = getOrder(orderNo);
        Robot robot = order.getRobotId() != null
                ? robotRepository.findById(order.getRobotId()).orElse(null) : null;
        String old = order.getTargetBuilding();
        order.setTargetBuilding(correctBuilding);
        if (correctFloor != null && correctFloor > 0) {
            order.setTargetFloor(correctFloor);
        }

        ResponsibleParty party = order.getResponsibleParty() == ResponsibleParty.PENDING
                ? ResponsibleParty.ROBOT : order.getResponsibleParty();

        if (order.getStatus() == OrderStatus.CREATED) {
            // 尚未出发：直接更正收货地址，按正常订单继续，不产生二次配送绕行
            orderRepository.save(order);
            eventRecorder.record(order, robot, EventType.CUSTOMER_SERVICE_INTERVENED, EventActor.SUPPORT,
                    LOC_BUILDING,
                    String.format("客服介入：出发前更正楼栋 %s -> %s。%s",
                            old, correctBuilding, note != null ? note : ""),
                    0, false, party);
            return order;
        }

        // 已在途：清空到达标记，重新进入小区内配送流程
        order.setArrivedAt(null);
        order.setStatus(OrderStatus.IN_COMMUNITY);
        orderRepository.save(order);

        eventRecorder.record(order, robot, EventType.CUSTOMER_SERVICE_INTERVENED, EventActor.SUPPORT, LOC_BUILDING,
                String.format("客服介入：核实为送错楼栋，%s -> %s，安排二次配送。%s",
                        old, correctBuilding, note != null ? note : ""),
                0, false, party);
        eventRecorder.record(order, robot, EventType.ROUTE_ADJUSTED, EventActor.STAFF, LOC_COMMUNITY,
                "按正确楼栋重新规划路线，二次配送中", 0, false, ResponsibleParty.NONE);
        if (robot != null) {
            robot.setStatus(RobotStatus.IN_DELIVERY);
            robotRepository.save(robot);
        }
        return order;
    }

    // ---------------------------------------------------------------------
    // 辅助方法
    // ---------------------------------------------------------------------

    private GateAuthorization saveGateRequest(DeliveryOrder order, Robot robot, String gatePoint,
                                              GateAuthStatus status, String rejectReason, Instant now) {
        GateAuthorization auth = new GateAuthorization();
        auth.setRequestNo("GATE-" + order.getOrderNo() + "-" + System.nanoTime() % 100000);
        auth.setOrderNo(order.getOrderNo());
        auth.setCommunityId(order.getCommunityId());
        auth.setRobotId(robot.getId());
        auth.setGatePoint(gatePoint != null ? gatePoint : "南门人行门");
        auth.setStatus(status);
        auth.setRejectReason(rejectReason);
        auth.setRequestedAt(now);
        if (status != GateAuthStatus.PENDING) {
            auth.setDecidedAt(now);
        }
        return gateRepository.save(auth);
    }

    private PropertyRule getOrCreateRule(Long communityId) {
        return ruleRepository.findByCommunityId(communityId).orElseGet(() -> {
            PropertyRule rule = new PropertyRule();
            rule.setCommunityId(communityId);
            rule.setRobotAllowed(true);
            rule.setNightRestricted(false);
            rule.setNightStart(rules.getNightStart());
            rule.setNightEnd(rules.getNightEnd());
            rule.setGateManualApproval(true);
            return ruleRepository.save(rule);
        });
    }

    private boolean isWithinNightWindow(Instant now, PropertyRule rule) {
        LocalTime start = rule.getNightStart() != null ? rule.getNightStart() : rules.getNightStart();
        LocalTime end = rule.getNightEnd() != null ? rule.getNightEnd() : rules.getNightEnd();
        LocalTime t = LocalTime.ofInstant(now, clock.getZone());
        // 跨午夜窗口，如 22:00-06:00
        return !t.isBefore(start) || t.isBefore(end);
    }

    private OrderStatus statusBeforeBlocked(DeliveryOrder order, String location) {
        return LOC_BUILDING.equals(location) || LOC_ELEVATOR.equals(location)
                ? OrderStatus.IN_BUILDING : OrderStatus.IN_COMMUNITY;
    }

    private String lastLocation(DeliveryOrder order) {
        return switch (order.getStatus()) {
            case GATE_PENDING -> LOC_GATE;
            case IN_BUILDING, ELEVATOR_WAITING -> LOC_BUILDING;
            case ARRIVED_WAITING_PICKUP -> LOC_DOOR;
            default -> LOC_COMMUNITY;
        };
    }

    private void requireStatus(DeliveryOrder order, OrderStatus... allowed) {
        for (OrderStatus s : allowed) {
            if (order.getStatus() == s) {
                return;
            }
        }
        throw new BusinessException("ILLEGAL_STATUS",
                "订单 " + order.getOrderNo() + " 当前状态 " + order.getStatus() + " 不允许该操作");
    }

    private DeliveryOrder getOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("订单不存在: " + orderNo));
    }

    private Robot getRobot(DeliveryOrder order) {
        if (order.getRobotId() == null) {
            throw new BusinessException("NO_ROBOT_ASSIGNED", "订单尚未分配机器人");
        }
        return robotRepository.findById(order.getRobotId())
                .orElseThrow(() -> new NotFoundException("机器人不存在: " + order.getRobotId()));
    }

    private String generateOrderNo() {
        return "DO" + Instant.now(clock).toEpochMilli() + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String gateText(String gatePoint) {
        return gatePoint != null ? gatePoint : "小区门禁";
    }

    private String timeText(LocalTime t) {
        return t != null ? t.toString() : "--:--";
    }

    private String orderNoTail(DeliveryOrder order) {
        String no = order.getOrderNo();
        return no.substring(no.length() - 4);
    }
}
