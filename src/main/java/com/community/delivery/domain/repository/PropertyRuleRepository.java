package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.PropertyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRuleRepository extends JpaRepository<PropertyRule, Long> {
    Optional<PropertyRule> findByCommunityId(Long communityId);
}
