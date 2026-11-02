package com.divistay.web;

import com.divistay.service.GroupLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the "System/Timer" actor: expiries and hotel retries, every few seconds. */
@Component
public class LifecycleJob {

    private final GroupLifecycleService lifecycle;

    public LifecycleJob(GroupLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelayString = "${divistay.lifecycle-interval-ms:10000}")
    public void run() {
        lifecycle.runOnce();
    }
}
