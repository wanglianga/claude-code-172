package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.ConstructionNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ConstructionNoticeRepository extends JpaRepository<ConstructionNotice, Long> {

    /** 某小区当前时刻仍在生效（且时间窗覆盖）的施工通告 */
    List<ConstructionNotice> findByCommunityIdAndActiveTrueAndStartTimeBeforeAndEndTimeAfter(
            Long communityId, Instant now1, Instant now2);

    List<ConstructionNotice> findByCommunityIdAndActiveTrue(Long communityId);
}
