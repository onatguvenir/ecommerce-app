package com.monat.ecommerce.inventory.infrastructure.scheduler;

import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.service.InventoryDomainService;
import com.monat.ecommerce.inventory.domain.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job to release expired reservations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final StockReservationRepository reservationRepository;
    private final InventoryDomainService inventoryDomainService;

    @Scheduled(fixedDelay = 60000) // Every minute
    public void releaseExpiredReservations() {
        log.debug("Checking for expired reservations");

        var expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.ACTIVE,
                LocalDateTime.now()
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations", expiredReservations.size());

        expiredReservations.forEach(reservation -> {
            try {
                log.info("Releasing expired reservation: {}", reservation.getReservationId());
                inventoryDomainService.releaseReservation(reservation.getReservationId());
                reservation.markAsExpired();
                reservationRepository.save(reservation);
            } catch (Exception e) {
                log.error("Failed to release expired reservation: {}", reservation.getId(), e);
            }
        });
    }
}
