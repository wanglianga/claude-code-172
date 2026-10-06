package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.Robot;
import com.community.delivery.domain.enums.RobotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    Optional<Robot> findByCode(String code);
    List<Robot> findByStatus(RobotStatus status);
}
