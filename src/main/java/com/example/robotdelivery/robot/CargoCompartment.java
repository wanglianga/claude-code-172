package com.example.robotdelivery.robot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cargo_compartment")
public class CargoCompartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    @Column(name = "compartment_no", nullable = false, length = 10)
    private String compartmentNo;

    /** 是否支持冷藏 */
    @Column(name = "cold_chain_capable", nullable = false)
    private boolean coldChainCapable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompartmentStatus status;
}
