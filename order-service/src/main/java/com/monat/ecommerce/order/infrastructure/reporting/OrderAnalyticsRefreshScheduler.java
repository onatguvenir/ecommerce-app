package com.monat.ecommerce.order.infrastructure.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAnalyticsRefreshScheduler {

    private final OrderAnalyticsRepository orderAnalyticsRepository;

    @Scheduled(fixedDelayString = "${application.reporting.materialized-view-refresh-ms:300000}")
    public void refreshMaterializedViews() {
        log.debug("Refreshing order analytics materialized views");
        orderAnalyticsRepository.refreshMaterializedViews();
    }
}
