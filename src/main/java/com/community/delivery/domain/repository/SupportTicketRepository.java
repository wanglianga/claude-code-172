package com.community.delivery.domain.repository;

import com.community.delivery.domain.entity.SupportTicket;
import com.community.delivery.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketNo(String ticketNo);
    List<SupportTicket> findByOrderNo(String orderNo);
    List<SupportTicket> findByStatus(TicketStatus status);
}
