package com.monat.ecommerce.payment.infrastructure.bootstrap;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds the database with dummy payments.
 * Only runs when 'docker' profile is active and database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class PaymentDataSeeder implements CommandLineRunner {

    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) throws Exception {
        if (paymentRepository.count() > 0) {
            log.info("Payments already exist. Skipping seeding.");
            return;
        }

        log.info("Seeding payments...");

        // Corresponds to Order ORD-2023-001 (Completed)
        createPayment("ORD-2023-001", new BigDecimal("1199.98"), PaymentStatus.COMPLETED, "PAY-REF-001");

        // Corresponds to Order ORD-2023-002 (Processing - Payment Confirmed)
        createPayment("ORD-2023-002", new BigDecimal("149.99"), PaymentStatus.COMPLETED, "PAY-REF-002");

        // Corresponds to Order ORD-2023-003 (Cancelled/Refunded)
        Payment payment3 = createPayment("ORD-2023-003", new BigDecimal("449.99"), PaymentStatus.REFUNDED,
                "PAY-REF-003");
        payment3.setRefundReference("REF-REF-003");
        payment3.setRefundedAmount(new BigDecimal("449.99"));
        paymentRepository.save(payment3);

        log.info("Seeding payments completed. Created {} payments.", paymentRepository.count());
    }

    private Payment createPayment(String orderId, BigDecimal amount, PaymentStatus status, String reference) {
        Payment payment = Payment.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .orderId(orderId) // In real scenario this is UUID, here string match with Order Number for
                                  // simplicity or we assume ID
                .userId(UUID.randomUUID().toString())
                .paymentReference(reference)
                .amount(amount)
                .currency("USD")
                .paymentMethod(PaymentMethod.CARD)
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }
}
