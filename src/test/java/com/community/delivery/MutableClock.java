package com.community.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** 测试用可变时钟：可在测试中把时间拨到夜间窗口或超时时刻 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void advanceSeconds(long seconds) {
        this.instant = instant.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /** ISO UTC 字符串构造上海时区时钟 */
    public static MutableClock shanghaiAt(String utcIso) {
        return new MutableClock(Instant.parse(utcIso), ZoneId.of("Asia/Shanghai"));
    }
}
