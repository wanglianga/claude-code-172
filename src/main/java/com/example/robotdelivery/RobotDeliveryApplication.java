package com.example.robotdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RobotDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotDeliveryApplication.class, args);
    }
}
