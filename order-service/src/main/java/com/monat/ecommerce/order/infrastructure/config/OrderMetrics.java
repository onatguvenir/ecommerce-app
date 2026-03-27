package com.monat.ecommerce.order.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer orderCreationTimer;
    private final Timer sagaExecutionTimer;
    private final Timer outboxPublishTimer;
    private final DistributionSummary orderItemSummary;
    private final DistributionSummary orderAmountSummary;
    private final DistributionSummary outboxBatchSummary;

    public OrderMetrics(MeterRegistry meterRegistry, OutboxEventRepository outboxEventRepository) {
        this.meterRegistry = meterRegistry;
        this.orderCreationTimer = MetricUtils.timer(
                meterRegistry,
                "order_creation_duration",
                "Duration of order creation requests",
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2));
        this.sagaExecutionTimer = MetricUtils.timer(
                meterRegistry,
                "order_saga_duration",
                "Duration of order saga execution",
                Duration.ofMillis(250),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10));
        this.outboxPublishTimer = MetricUtils.timer(
                meterRegistry,
                "order_outbox_publish_duration",
                "Duration of order outbox publish batches",
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1));
        this.orderItemSummary = MetricUtils.summary(
                meterRegistry,
                "order_items_per_order",
                "Distribution of item counts per order",
                "items",
                new double[]{1, 2, 5, 10, 20});
        this.orderAmountSummary = MetricUtils.summary(
                meterRegistry,
                "order_total_amount",
                "Distribution of total order amounts",
                "currency_units",
                new double[]{10, 50, 100, 250, 500, 1000});
        this.outboxBatchSummary = MetricUtils.summary(
                meterRegistry,
                "order_outbox_batch_size",
                "Distribution of order outbox publish batch sizes",
                "events",
                new double[]{1, 10, 25, 50, 100});

        Gauge.builder("order_outbox_pending_events", outboxEventRepository, OutboxEventRepository::countByProcessedFalse)
                .description("Pending order outbox events waiting to be published")
                .register(meterRegistry);
    }

    public Timer orderCreationTimer() {
        return orderCreationTimer;
    }

    public Timer sagaExecutionTimer() {
        return sagaExecutionTimer;
    }

    public Timer outboxPublishTimer() {
        return outboxPublishTimer;
    }

    public void recordOrderCreated(int itemCount, double totalAmount, String source) {
        orderItemSummary.record(itemCount);
        orderAmountSummary.record(totalAmount);
        counter("orders_created_total", "Successfully created orders", "source", source).increment();
    }

    public void incrementOrderCreationFailure(String reason) {
        counter("order_creation_failures_total", "Order creation failures by reason", "reason", reason).increment();
    }

    public void incrementSagaResult(String result) {
        counter("order_saga_total", "Order saga executions by result", "result", result).increment();
    }

    public void incrementSagaStep(String step, String result) {
        counter("order_saga_step_total", "Order saga step attempts by result", "step", step, "result", result).increment();
    }

    public void incrementOutboxPublish(String result, String eventType) {
        counter("order_outbox_publish_total", "Order outbox publish attempts by result", "result", result, "event_type", eventType).increment();
    }

    public void recordOutboxBatchSize(int batchSize) {
        outboxBatchSummary.record(batchSize);
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
