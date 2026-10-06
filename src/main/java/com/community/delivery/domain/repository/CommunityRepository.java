package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByCode(String code);
}
