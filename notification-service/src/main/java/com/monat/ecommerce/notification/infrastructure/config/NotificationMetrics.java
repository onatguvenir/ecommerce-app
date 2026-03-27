package com.monat.ecommerce.notification.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer consumerTimer;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.consumerTimer = MetricUtils.timer(
                meterRegistry,
                "notification_consumer_duration",
                "Duration of notification event consumer handlers",
                Duration.ofMillis(25),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1));
    }

    public Timer consumerTimer() {
        return consumerTimer;
    }

    public void incrementConsumerEvent(String eventType, String result) {
        counter("notification_consumer_events_total", "Notification consumer events by result", "event_type", eventType, "result", result)
                .increment();
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
