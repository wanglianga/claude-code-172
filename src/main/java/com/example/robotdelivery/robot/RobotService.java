package com.example.robotdelivery.robot;

import com.example.robotdelivery.community.Community;
import com.example.robotdelivery.community.CommunityRepository;
import com.example.robotdelivery.config.ApiException;
import com.example.robotdelivery.config.DeliveryProperties;
import com.example.robotdelivery.cs.CaseType;
import com.example.robotdelivery.cs.CsService;
import com.example.robotdelivery.cs.Responsibility;
import com.example.robotdelivery.event.DeliveryEventService;
import com.example.robotdelivery.event.EventActor;
import com.example.robotdelivery.event.EventType;
import com.example.robotdelivery.order.DeliveryOrderRepository;
import com.example.robotdelivery.order.DeliveryStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RobotService {

    private final RobotRepository robotRepository;
    private final CargoCompartmentRepository compartmentRepository;
    private final CommunityRepository communityRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DeliveryEventService eventService;
    private final CsService csService;
    private final DeliveryProperties properties;

    public RobotService(RobotRepository robotRepository,
                        CargoCompartmentRepository compartmentRepository,
                        CommunityRepository communityRepository,
                        DeliveryOrderRepository orderRepository,
                        DeliveryEventService eventService,
                        CsService csService,
                        DeliveryProperties properties) {
        this.robotRepository = robotRepository;
        this.compartmentRepository = compartmentRepository;
        this.communityRepository = communityRepository;
        this.orderRepository = orderRepository;
        this.eventService = eventService;
        this.csService = csService;
        this.properties = properties;
    }

    /**
     * 注册机器人，默认配备 A（冷藏）/B（普通）两个载物仓。
     */
    @Transactional
    public Robot register(String code, Long communityId, Integer battery) {
        if (robotRepository.findByCode(code).isPresent()) {
            throw ApiException.conflict("机器人编号已存在: " + code);
        }
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> ApiException.notFound("社区不存在: " + communityId));
        Robot robot = new Robot();
        robot.setCode(code);
        robot.setCommunity(community);
        robot.setBattery(battery == null ? 100 : battery);
        robot.setStatus(RobotStatus.IDLE);
        robotRepository.save(robot);

        CargoCompartment cold = new CargoCompartment();
        cold.setRobot(robot);
        cold.setCompartmentNo("A");
        cold.setColdChainCapable(true);
        cold.setStatus(CompartmentStatus.IDLE);
        compartmentRepository.save(cold);

        CargoCompartment normal = new CargoCompartment();
        normal.setRobot(robot);
        normal.setCompartmentNo("B");
        normal.setColdChainCapable(false);
        normal.setStatus(CompartmentStatus.IDLE);
        compartmentRepository.save(normal);
        return robot;
    }

    /**
     * 机器人状态/电量上报。配送途中电量低于阈值时写入 LOW_BATTERY 事件并触发客服介入。
     */
    @Transactional
    public Robot updateStatus(Long robotId, RobotStatus status, Integer battery) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> ApiException.notFound("机器人不存在: " + robotId));
        if (status != null) {
            robot.setStatus(status);
        }
        if (battery != null) {
            if (battery < 0 || battery > 100) {
                throw ApiException.badRequest("电量须在 0-100 之间");
            }
            robot.setBattery(battery);
        }
        robotRepository.save(robot);

        if (battery != null && battery < properties.getLowBatteryThreshold()) {
            orderRepository.findFirstByRobot_IdAndStatusInOrderByIdDesc(robotId, DeliveryStatus.ACTIVE)
                    .ifPresent(order -> {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("robotCode", robot.getCode());
                        payload.put("battery", battery);
                        payload.put("threshold", properties.getLowBatteryThreshold());
                        payload.put("orderNo", order.getOrderNo());
                        eventService.emit(order.getId(), order.getCommunity().getId(),
                                EventType.LOW_BATTERY, EventActor.ROBOT, payload);
                        csService.openCase(order.getId(), CaseType.LOW_BATTERY,
                                Responsibility.ROBOT_VENDOR,
                                "机器人" + robot.getCode() + "配送途中电量不足(" + battery + "%)，建议人工接力", null);
                    });
        }
        return robot;
    }

    public List<Robot> list(Long communityId) {
        return communityId == null ? robotRepository.findAll() : robotRepository.findByCommunityId(communityId);
    }

    public List<CargoCompartment> compartments(Long robotId) {
        return compartmentRepository.findByRobotId(robotId);
    }
}
