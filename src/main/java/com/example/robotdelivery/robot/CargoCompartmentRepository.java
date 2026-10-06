package com.example.robotdelivery.robot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CargoCompartmentRepository extends JpaRepository<CargoCompartment, Long> {

    List<CargoCompartment> findByRobotId(Long robotId);

    Optional<CargoCompartment> findByRobotIdAndCompartmentNo(Long robotId, String compartmentNo);

    CargoCompartment findFirstByRobotIdAndStatusOrderByCompartmentNoAsc(Long robotId, CompartmentStatus status);

    CargoCompartment findFirstByRobotIdAndStatusAndColdChainCapableOrderByCompartmentNoAsc(
            Long robotId, CompartmentStatus status, boolean coldChainCapable);
}
