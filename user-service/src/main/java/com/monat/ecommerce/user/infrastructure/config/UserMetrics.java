package com.monat.ecommerce.user.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class UserMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer suspensionEventTimer;

    public UserMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.suspensionEventTimer = MetricUtils.timer(
                meterRegistry,
                "user_suspension_event_duration",
                "Duration of fraud-driven user suspension event handling",
                Duration.ofMillis(25),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500));
    }

    public Timer suspensionEventTimer() {
        return suspensionEventTimer;
    }

    public void incrementSuspensionEvent(String result) {
        counter("user_suspension_events_total", "User suspension events consumed by result", "result", result).increment();
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
