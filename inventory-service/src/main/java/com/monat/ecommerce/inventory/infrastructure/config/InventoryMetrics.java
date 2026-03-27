package com.monat.ecommerce.inventory.infrastructure.config;

import com.monat.ecommerce.common.util.MetricUtils;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class InventoryMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer reservationTimer;
    private final Timer adjustmentTimer;
    private final DistributionSummary reservationQuantitySummary;
    private final DistributionSummary stockAdjustmentSummary;

    public InventoryMetrics(MeterRegistry meterRegistry, InventoryRepository inventoryRepository) {
        this.meterRegistry = meterRegistry;
        this.reservationTimer = MetricUtils.timer(
                meterRegistry,
                "inventory_reservation_duration",
                "Duration of stock reservation and confirmation flows",
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500),
                Duration.ofSeconds(1));
        this.adjustmentTimer = MetricUtils.timer(
                meterRegistry,
                "inventory_adjustment_duration",
                "Duration of inventory stock adjustment flows",
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(250),
                Duration.ofMillis(500));
        this.reservationQuantitySummary = MetricUtils.summary(
                meterRegistry,
                "inventory_reserved_quantity",
                "Distribution of reserved stock quantities",
                "items",
                new double[]{1, 2, 5, 10, 25, 50});
        this.stockAdjustmentSummary = MetricUtils.summary(
                meterRegistry,
                "inventory_adjustment_quantity",
                "Distribution of stock adjustment quantities",
                "items",
                new double[]{1, 5, 10, 25, 50, 100, 250});

        Gauge.builder("inventory_products_total", inventoryRepository, InventoryRepository::count)
                .description("Number of inventory records")
                .register(meterRegistry);
    }

    public Timer reservationTimer() {
        return reservationTimer;
    }

    public Timer adjustmentTimer() {
        return adjustmentTimer;
    }

    public void recordReservationQuantity(double quantity, String action, String result) {
        reservationQuantitySummary.record(quantity);
        counter("inventory_reservations_total", "Inventory reservation actions by result", "action", action, "result", result)
                .increment();
    }

    public void recordStockAdjustment(double quantity, String action) {
        stockAdjustmentSummary.record(quantity);
        counter("inventory_stock_adjustments_total", "Inventory stock adjustment operations", "action", action).increment();
    }

    public void incrementCacheEviction(String result) {
        counter("inventory_cache_evictions_total", "Inventory cache eviction attempts by result", "result", result).increment();
    }

    public void incrementBulkUpdate(String action, String result) {
        counter("inventory_bulk_updates_total", "Bulk inventory update messages by result", "action", action, "result", result).increment();
    }

    private Counter counter(String name, String description, String... tags) {
        return MetricUtils.counter(meterRegistry, name, description, tags);
    }
}
