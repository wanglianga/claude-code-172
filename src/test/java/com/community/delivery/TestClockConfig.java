package com.community.delivery;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/** 用可变时钟替换系统时钟，便于测试夜间限制与超时场景 */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock testClock() {
        // 2026-10-06 18:00 上海时间（白天）
        return MutableClock.shanghaiAt("2026-10-06T10:00:00Z");
    }
}
