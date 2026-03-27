package com.monat.ecommerce.common.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public final class MetricUtils {

    private MetricUtils() {
    }

    public static Counter counter(MeterRegistry registry, String name, String description, String... tags) {
        return Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(registry);
    }

    public static Timer timer(MeterRegistry registry, String name, String description, Duration... serviceLevelObjectives) {
        Timer.Builder builder = Timer.builder(name)
                .description(description)
                .publishPercentileHistogram();

        if (serviceLevelObjectives != null && serviceLevelObjectives.length > 0) {
            builder.serviceLevelObjectives(serviceLevelObjectives);
        }

        return builder.register(registry);
    }

    public static DistributionSummary summary(
            MeterRegistry registry,
            String name,
            String description,
            String baseUnit,
            double[] serviceLevelObjectives,
            String... tags) {
        DistributionSummary.Builder builder = DistributionSummary.builder(name)
                .description(description)
                .baseUnit(baseUnit)
                .tags(tags)
                .publishPercentileHistogram();

        if (serviceLevelObjectives != null && serviceLevelObjectives.length > 0) {
            builder.serviceLevelObjectives(serviceLevelObjectives);
        }

        return builder.register(registry);
    }
}
