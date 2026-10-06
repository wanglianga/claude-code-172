package com.community.delivery.web;

import com.community.delivery.domain.entity.Community;
import com.community.delivery.domain.entity.Robot;
import com.community.delivery.domain.repository.CommunityRepository;
import com.community.delivery.domain.repository.RobotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 基础数据查询：小区与机器人台账（各端登录后可见） */
@RestController
@RequestMapping("/api/meta")
@Tag(name = "基础数据", description = "小区、机器人台账")
@SecurityRequirement(name = "basicAuth")
public class MetaController {

    private final CommunityRepository communityRepository;
    private final RobotRepository robotRepository;

    public MetaController(CommunityRepository communityRepository, RobotRepository robotRepository) {
        this.communityRepository = communityRepository;
        this.robotRepository = robotRepository;
    }

    @GetMapping("/communities")
    @Operation(summary = "小区列表")
    public List<Community> communities() {
        return communityRepository.findAll();
    }

    @GetMapping("/robots")
    @Operation(summary = "机器人台账（状态/电量/冷藏仓能力）")
    public List<Robot> robots() {
        return robotRepository.findAll();
    }
}
