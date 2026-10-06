package com.community.delivery.config;

import com.community.delivery.domain.entity.Community;
import com.community.delivery.domain.entity.ConstructionNotice;
import com.community.delivery.domain.entity.PropertyRule;
import com.community.delivery.domain.entity.Robot;
import com.community.delivery.domain.enums.RobotStatus;
import com.community.delivery.domain.repository.CommunityRepository;
import com.community.delivery.domain.repository.ConstructionNoticeRepository;
import com.community.delivery.domain.repository.PropertyRuleRepository;
import com.community.delivery.domain.repository.RobotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;

/** 初始化演示数据：小区、物业规则、机器人、施工通告 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedData(CommunityRepository communityRepository,
                                      PropertyRuleRepository ruleRepository,
                                      RobotRepository robotRepository,
                                      ConstructionNoticeRepository constructionRepository,
                                      Clock clock) {
        return args -> {
            if (communityRepository.count() > 0) {
                return;
            }
            Community sunny = new Community();
            sunny.setCode("C-SUNNY");
            sunny.setName("阳光花园");
            sunny.setAddress("上海市闵行区阳光路 88 号");
            communityRepository.save(sunny);

            Community river = new Community();
            river.setCode("C-RIVER");
            river.setName("河畔家园");
            river.setAddress("上海市浦东新区滨河路 12 号");
            communityRepository.save(river);

            PropertyRule sunnyRule = new PropertyRule();
            sunnyRule.setCommunityId(sunny.getId());
            sunnyRule.setRobotAllowed(true);
            sunnyRule.setGateManualApproval(true);
            sunnyRule.setNightRestricted(false);
            sunnyRule.setNightStart(LocalTime.of(22, 0));
            sunnyRule.setNightEnd(LocalTime.of(6, 0));
            ruleRepository.save(sunnyRule);

            PropertyRule riverRule = new PropertyRule();
            riverRule.setCommunityId(river.getId());
            riverRule.setRobotAllowed(true);
            riverRule.setGateManualApproval(false); // 河畔家园门禁免人工审批
            riverRule.setNightRestricted(true);     // 已应物业要求限制夜间运行
            riverRule.setNightStart(LocalTime.of(21, 30));
            riverRule.setNightEnd(LocalTime.of(6, 30));
            riverRule.setRemark("物业要求 21:30-06:30 机器人停止运行");
            ruleRepository.save(riverRule);

            createRobot(robotRepository, "R01", sunny.getId(), 96, 2, RobotStatus.IDLE);
            createRobot(robotRepository, "R02", sunny.getId(), 58, 1, RobotStatus.IDLE);
            createRobot(robotRepository, "R03", river.getId(), 15, 0, RobotStatus.CHARGING);

            ConstructionNotice notice = new ConstructionNotice();
            notice.setCommunityId(sunny.getId());
            notice.setBuilding("6号楼");
            notice.setDescription("6号楼一层大厅地面维修，单侧通道封闭");
            notice.setStartTime(Instant.now(clock).minusSeconds(3600));
            notice.setEndTime(Instant.now(clock).plusSeconds(7 * 24 * 3600));
            notice.setActive(true);
            constructionRepository.save(notice);

            log.info("演示数据初始化完成：小区 2 个、机器人 3 台、施工通告 1 条");
        };
    }

    private void createRobot(RobotRepository repo, String code, Long communityId,
                             int battery, int coldCapacity, RobotStatus status) {
        Robot robot = new Robot();
        robot.setCode(code);
        robot.setCurrentCommunityId(communityId);
        robot.setBatteryPercent(battery);
        robot.setColdBinCapacity(coldCapacity);
        robot.setStatus(status);
        repo.save(robot);
    }
}
