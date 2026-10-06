package com.community.delivery.service;

import com.community.delivery.config.AppRulesProperties;
import com.community.delivery.domain.entity.ConstructionNotice;
import com.community.delivery.domain.entity.PropertyRule;
import com.community.delivery.domain.enums.EventType;
import com.community.delivery.domain.repository.ConstructionNoticeRepository;
import com.community.delivery.domain.repository.DeliveryEventRepository;
import com.community.delivery.domain.repository.PropertyRuleRepository;
import com.community.delivery.web.dto.PassageImpact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * 物业端服务：
 * 1) 维护小区门禁/夜间运行规则；
 * 2) 发布/撤销楼道施工通告；
 * 3) 查看机器人是否影响居民通行（占道事件、占用时长、按楼栋分布、事件明细）。
 */
@Service
public class PropertyService {

    private final PropertyRuleRepository ruleRepository;
    private final ConstructionNoticeRepository constructionRepository;
    private final DeliveryEventRepository eventRepository;
    private final AppRulesProperties rules;
    private final Clock clock;

    public PropertyService(PropertyRuleRepository ruleRepository,
                           ConstructionNoticeRepository constructionRepository,
                           DeliveryEventRepository eventRepository,
                           AppRulesProperties rules,
                           Clock clock) {
        this.ruleRepository = ruleRepository;
        this.constructionRepository = constructionRepository;
        this.eventRepository = eventRepository;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PropertyRule getRule(Long communityId) {
        return ruleRepository.findByCommunityId(communityId)
                .orElseGet(() -> {
                    PropertyRule rule = new PropertyRule();
                    rule.setCommunityId(communityId);
                    rule.setNightStart(rules.getNightStart());
                    rule.setNightEnd(rules.getNightEnd());
                    return rule;
                });
    }

    /** 更新物业规则（含"要求限制夜间运行"） */
    @Transactional
    public PropertyRule updateRule(Long communityId, boolean robotAllowed, boolean nightRestricted,
                                   String nightStart, String nightEnd, boolean gateManualApproval, String remark) {
        PropertyRule rule = ruleRepository.findByCommunityId(communityId).orElseGet(() -> {
            PropertyRule r = new PropertyRule();
            r.setCommunityId(communityId);
            return r;
        });
        rule.setRobotAllowed(robotAllowed);
        rule.setNightRestricted(nightRestricted);
        rule.setNightStart(nightRestricted
                ? LocalTime.parse(nightStart != null ? nightStart : rules.getNightStart().toString())
                : rules.getNightStart());
        rule.setNightEnd(nightRestricted
                ? LocalTime.parse(nightEnd != null ? nightEnd : rules.getNightEnd().toString())
                : rules.getNightEnd());
        rule.setGateManualApproval(gateManualApproval);
        rule.setRemark(remark);
        return ruleRepository.save(rule);
    }

    @Transactional
    public ConstructionNotice publishConstruction(Long communityId, String building,
                                                  String description, Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("BAD_TIME_RANGE", "施工结束时间必须晚于开始时间");
        }
        ConstructionNotice notice = new ConstructionNotice();
        notice.setCommunityId(communityId);
        notice.setBuilding(building);
        notice.setDescription(description);
        notice.setStartTime(startTime);
        notice.setEndTime(endTime);
        notice.setActive(true);
        return constructionRepository.save(notice);
    }

    @Transactional
    public ConstructionNotice revokeConstruction(Long noticeId) {
        ConstructionNotice notice = constructionRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("施工通告不存在: " + noticeId));
        notice.setActive(false);
        return constructionRepository.save(notice);
    }

    @Transactional(readOnly = true)
    public List<ConstructionNotice> listConstructions(Long communityId) {
        if (communityId != null) {
            return constructionRepository.findByCommunityIdAndActiveTrue(communityId);
        }
        return constructionRepository.findAll();
    }

    /** 物业看板：机器人对居民通行的影响 */
    @Transactional(readOnly = true)
    public PassageImpact passageImpact(Long communityId, Instant since) {
        Instant from = since != null ? since : Instant.now(clock).minusSeconds(7L * 24 * 3600);

        List<PassageImpact.BuildingPassage> byBuilding = eventRepository
                .aggregatePassageImpactByBuilding(communityId, from).stream()
                .map(row -> new PassageImpact.BuildingPassage(
                        (String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();

        long blocking = eventRepository.countByCommunityIdAndBlocksResidentPassageTrue(communityId);

        List<PassageImpact.EventTypeCount> breakdown = eventRepository.aggregateEventImpact(communityId, from)
                .stream()
                .map(row -> {
                    EventType type = (EventType) row[0];
                    return new PassageImpact.EventTypeCount(type.name(), type.getLabel(),
                            ((Number) row[1]).longValue(), ((Number) row[2]).longValue());
                })
                .toList();

        long gateRejected = countType(breakdown, EventType.GATE_REJECTED);
        long night = countType(breakdown, EventType.NIGHT_RESTRICTION);
        long obstacleWait = countType(breakdown, EventType.OBSTACLE_WAIT);
        long elevatorFull = countType(breakdown, EventType.ELEVATOR_FULL);
        long construction = countType(breakdown, EventType.CONSTRUCTION_ENCOUNTERED);
        long totalBlockingMs = byBuilding.stream().mapToLong(PassageImpact.BuildingPassage::blockingMs).sum();

        return new PassageImpact(communityId, blocking, totalBlockingMs, gateRejected, night,
                obstacleWait, elevatorFull, construction, byBuilding, breakdown);
    }

    private long countType(List<PassageImpact.EventTypeCount> breakdown, EventType type) {
        return breakdown.stream().filter(c -> c.eventType().equals(type.name()))
                .mapToLong(PassageImpact.EventTypeCount::count).findFirst().orElse(0);
    }
}
