package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
    List<NotificationRecord> findByOrderNoOrderBySentAtAsc(String orderNo);
}
