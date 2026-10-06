package com.example.robotdelivery.cs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CsCaseRepository extends JpaRepository<CsCase, Long> {

    List<CsCase> findByStatus(CaseStatus status);

    List<CsCase> findByResponsibility(Responsibility responsibility);

    List<CsCase> findByCaseType(CaseType caseType);

    List<CsCase> findByDeliveryId(Long deliveryId);

    Optional<CsCase> findByCaseNo(String caseNo);
}
