package com.monat.ecommerce.payment.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import com.monat.ecommerce.payment.infrastructure.persistence.repository.PaymentOutboxEventJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;
    private final PaymentOutboxEventJpaRepository outboxRepository;
    private final AtomicLong pendingOutboxCount = new AtomicLong(0);
    private final Timer paymentProcessingTimer;
    private final Timer refundProcessingTimer;
    private final Timer outboxPublishTimer;
    private final DistributionSummary paymentAmountSummary;
    private final DistributionSummary refundAmountSummary;
    private final DistributionSummary outboxBatchSummary;

    public PaymentMetrics(MeterRegistry meterRegistry, PaymentOutboxEventJpaRepository outboxRepository) {
        this.meterRegistry = meterRegistry;
        this.outboxRepository = outboxRepository;
        this.paymentProcessingTimer = MetricUtils.timer(
                meterRegistry,
                "payment_processing_duration",
                "Duration of payment processing flow",
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5));
        this.refundProcessingTimer = MetricUtils.timer(
                meterRegistry,
                "payment_refund_duration",
                "Duration of refund processing flow",
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2));
        this.outboxPublishTimer = MetricUtils.timer(
                meterRegistry,
                "payment_outbox_publish_duration",
                "Duration of payment outbox publish batches",
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1));
        this.paymentAmountSummary = MetricUtils.summary(
                meterRegistry,
                "payment_amount",
                "Distribution of processed payment amounts",
                "currency_units",
                new double[]{10, 50, 100, 250, 500, 1000});
        this.refundAmountSummary = MetricUtils.summary(
                meterRegistry,
                "payment_refund_amount",
                "Distribution of refunded payment amounts",
                "currency_units",
                new double[]{10, 50, 100, 250, 500, 1000});
        this.outboxBatchSummary = MetricUtils.summary(
                meterRegistry,
                "payment_outbox_batch_size",
                "Distribution of payment outbox publish batch sizes",
                "events",
                new double[]{1, 10, 25, 50, 100});

        Gauge.builder("payment_outbox_pending_events", pendingOutboxCount, AtomicLong::get)
                .description("Pending payment outbox events waiting to be published")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 60_000)
    public void refreshPendingOutboxCount() {
        pendingOutboxCount.set(outboxRepository.countByProcessedFalse());
    }

    public Timer paymentProcessingTimer() {
        return paymentProcessingTimer;
    }

    public Timer refundProcessingTimer() {
        return refundProcessingTimer;
    }

    public Timer outboxPublishTimer() {
        return outboxPublishTimer;
    }

    public void recordPaymentAmount(double amount, String currency, String result) {
        paymentAmountSummary.record(amount);
        counter("payment_requests_total", "Payment requests by result", "operation", "process", "result", result, "currency", currency)
                .increment();
    }

    public void recordRefundAmount(double amount, String currency, String result) {
        refundAmountSummary.record(amount);
        counter("payment_requests_total", "Payment requests by result", "operation", "refund", "result", result, "currency", currency)
                .increment();
    }

    public void incrementPaymentResult(String operation, String result) {
        counter("payment_requests_total", "Payment requests by result", "operation", operation, "result", result)
                .increment();
    }

    public void incrementOutboxPublish(String result, String eventType) {
        counter("payment_outbox_publish_total", "Payment outbox publish attempts by result", "result", result, "event_type", eventType)
                .increment();
    }

    public void recordOutboxBatchSize(int batchSize) {
        outboxBatchSummary.record(batchSize);
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
