package com.divistay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock tests can move forward, to check what happens when 30 minutes pass. */
public class TestClock extends Clock {

    private Instant now;
    private final ZoneId zone;

    public TestClock(Instant start) {
        this(start, ZoneId.of("Asia/Jerusalem"));
    }

    private TestClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    public void advance(Duration d) {
        now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
