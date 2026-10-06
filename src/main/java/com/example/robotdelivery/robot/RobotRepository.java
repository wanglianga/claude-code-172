package com.example.robotdelivery.robot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    List<Robot> findByCommunityId(Long communityId);

    Optional<Robot> findByCode(String code);
}
