package com.monat.ecommerce.cart.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CartMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer cartOperationTimer;
    private final DistributionSummary cartItemSummary;

    public CartMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cartOperationTimer = MetricUtils.timer(
                meterRegistry,
                "cart_operation_duration",
                "Duration of cart operations",
                Duration.ofMillis(10),
                Duration.ofMillis(25),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250));
        this.cartItemSummary = MetricUtils.summary(
                meterRegistry,
                "cart_items_per_cart",
                "Distribution of item counts per cart after mutations",
                "items",
                new double[]{1, 2, 5, 10, 20, 50, 100});
    }

    public Timer cartOperationTimer() {
        return cartOperationTimer;
    }

    public void incrementOperation(String operation, String result) {
        counter("cart_operations_total", "Cart operations by result", "operation", operation, "result", result).increment();
    }

    public void recordCartSize(double itemCount, String operation) {
        cartItemSummary.record(itemCount);
        counter("cart_size_samples_total", "Cart size samples emitted after mutations", "operation", operation).increment();
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
